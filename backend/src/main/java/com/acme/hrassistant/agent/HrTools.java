package com.acme.hrassistant.agent;

import com.acme.hrassistant.application.chat.ToolCallAuditService;
import com.acme.hrassistant.application.holidays.HolidayFacade;
import com.acme.hrassistant.application.leave.LeaveBalanceFacade;
import com.acme.hrassistant.domain.Holiday;
import com.acme.hrassistant.domain.LeaveBalance;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Read-only HR tools. employeeId is never a model parameter — only ToolContext.
 */
@Component
public class HrTools {

    private final LeaveBalanceFacade leaveBalanceFacade;
    private final HolidayFacade holidayFacade;
    private final ToolCallAuditService toolCallAuditService;

    public HrTools(
            LeaveBalanceFacade leaveBalanceFacade,
            HolidayFacade holidayFacade,
            ToolCallAuditService toolCallAuditService) {
        this.leaveBalanceFacade = leaveBalanceFacade;
        this.holidayFacade = holidayFacade;
        this.toolCallAuditService = toolCallAuditService;
    }

    @Tool(description = """
            Get the logged-in employee's remaining leave balances for the current calendar year
            (casual / sick / earned). Use when the user asks how many leaves they have left.
            Do not invent balances — always call this tool.
            """)
    public String getLeaveBalance(ToolContext toolContext) {
        Long employeeId = requireEmployeeId(toolContext);
        String conversationId = requireConversationId(toolContext);

        List<LeaveBalance> balances = leaveBalanceFacade.getBalancesForEmployee(employeeId);
        String result;
        if (balances.isEmpty()) {
            result = "No leave balance records found for this employee for the current year.";
        } else {
            result = balances.stream()
                    .map(b -> b.getLeaveType() + ": " + b.getRemainingDays() + " day(s) remaining (" + b.getYear() + ")")
                    .collect(Collectors.joining("; "));
        }

        toolCallAuditService.record(employeeId, conversationId, "getLeaveBalance", Map.of(), result);
        return result;
    }

    @Tool(description = """
            Get Acme Corp company holidays for a given year, optionally filtered by month (1-12).
            Use for holiday calendars, festival offs, or whether a date is a holiday.
            """)
    public String getHolidayList(
            @ToolParam(description = "Calendar year, e.g. 2026") int year,
            @ToolParam(description = "Optional month 1-12; pass 0 or omit for the full year", required = false)
                    Integer month,
            ToolContext toolContext) {
        Long employeeId = requireEmployeeId(toolContext);
        String conversationId = requireConversationId(toolContext);
        String location = String.valueOf(toolContext.getContext().get(HrToolContextKeys.EMPLOYEE_LOCATION));

        Integer normalizedMonth = (month == null || month == 0) ? null : month;
        List<Holiday> holidays = holidayFacade.getHolidays(year, normalizedMonth, location);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("year", year);
        input.put("month", normalizedMonth);

        String result;
        if (holidays.isEmpty()) {
            result = "No holidays found for year=" + year
                    + (normalizedMonth == null ? "" : ", month=" + normalizedMonth)
                    + ".";
        } else {
            result = holidays.stream()
                    .map(h -> h.getDate() + " — " + h.getName()
                            + (h.getLocation() == null || h.getLocation().isBlank()
                                    ? " (company-wide)"
                                    : " (" + h.getLocation() + ")"))
                    .collect(Collectors.joining("\n"));
        }

        toolCallAuditService.record(employeeId, conversationId, "getHolidayList", input, result);
        return result;
    }

    private static Long requireEmployeeId(ToolContext toolContext) {
        Object value = toolContext.getContext().get(HrToolContextKeys.EMPLOYEE_ID);
        if (value instanceof Long id) {
            return id;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("employeeId missing from tool context");
    }

    private static String requireConversationId(ToolContext toolContext) {
        Object value = toolContext.getContext().get(HrToolContextKeys.CONVERSATION_ID);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalStateException("conversationId missing from tool context");
        }
        return String.valueOf(value);
    }
}
