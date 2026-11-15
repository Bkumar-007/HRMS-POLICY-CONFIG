package com.acme.hrassistant.web;

import com.acme.hrassistant.application.policies.commands.DeletePolicyCommand;
import com.acme.hrassistant.application.policies.commands.DisablePolicyCommand;
import com.acme.hrassistant.application.policies.commands.IngestAllPoliciesCommand;
import com.acme.hrassistant.application.policies.commands.IngestPolicyCommand;
import com.acme.hrassistant.application.policies.commands.UploadPolicyCommand;
import com.acme.hrassistant.application.policies.queries.GetPolicyQuery;
import com.acme.hrassistant.application.policies.queries.ListPoliciesQuery;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class PolicyController {

    private final ListPoliciesQuery listPoliciesQuery;
    private final GetPolicyQuery getPolicyQuery;
    private final UploadPolicyCommand uploadPolicyCommand;
    private final DeletePolicyCommand deletePolicyCommand;
    private final DisablePolicyCommand disablePolicyCommand;
    private final IngestPolicyCommand ingestPolicyCommand;
    private final IngestAllPoliciesCommand ingestAllPoliciesCommand;

    public PolicyController(
            ListPoliciesQuery listPoliciesQuery,
            GetPolicyQuery getPolicyQuery,
            UploadPolicyCommand uploadPolicyCommand,
            DeletePolicyCommand deletePolicyCommand,
            DisablePolicyCommand disablePolicyCommand,
            IngestPolicyCommand ingestPolicyCommand,
            IngestAllPoliciesCommand ingestAllPoliciesCommand) {
        this.listPoliciesQuery = listPoliciesQuery;
        this.getPolicyQuery = getPolicyQuery;
        this.uploadPolicyCommand = uploadPolicyCommand;
        this.deletePolicyCommand = deletePolicyCommand;
        this.disablePolicyCommand = disablePolicyCommand;
        this.ingestPolicyCommand = ingestPolicyCommand;
        this.ingestAllPoliciesCommand = ingestAllPoliciesCommand;
    }

    @GetMapping("/policies")
    public ApiResponse<List<PolicyDocumentDto>> list() {
        List<PolicyDocumentDto> policies = listPoliciesQuery.execute();
        return ApiResponse.ok(policies, policies.size());
    }

    @GetMapping("/policies/{id}")
    public ApiResponse<PolicyDocumentDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(getPolicyQuery.execute(id));
    }

    @PostMapping(value = "/policies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<PolicyDocumentDto> upload(
            @RequestParam("name") String name,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "effectiveDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate effectiveDate,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "location", required = false) String location) {
        return ApiResponse.ok(uploadPolicyCommand.execute(
                SecurityUtils.currentUser(), name, file, effectiveDate, grade, location));
    }

    @DeleteMapping("/policies/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        deletePolicyCommand.execute(id);
        return ApiResponse.ok(null, "Deleted");
    }

    @PostMapping("/policies/{id}/disable")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<PolicyDocumentDto> disable(@PathVariable UUID id) {
        return ApiResponse.ok(disablePolicyCommand.execute(id), "Policy disabled");
    }

    @PostMapping("/policies/{id}/enable")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<PolicyDocumentDto> enable(@PathVariable UUID id) {
        // Re-index chunks so the policy is searchable again
        return ApiResponse.ok(ingestPolicyCommand.execute(id), "Policy enabled");
    }

    @PostMapping("/policies/{id}/ingest")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<PolicyDocumentDto> ingest(@PathVariable UUID id) {
        return ApiResponse.ok(ingestPolicyCommand.execute(id));
    }

    @PostMapping("/admin/policies/ingest-all")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<List<PolicyDocumentDto>> ingestAll() {
        List<PolicyDocumentDto> results = ingestAllPoliciesCommand.execute();
        return ApiResponse.ok(results, results.size());
    }
}
