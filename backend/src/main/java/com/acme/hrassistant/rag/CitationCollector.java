package com.acme.hrassistant.rag;

import com.acme.hrassistant.web.dto.ChatResponse.CitationDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request-scoped collector filled by {@code searchHrPolicies}; read by ChatService.
 */
public class CitationCollector {

    private final Map<String, CitationDto> citations = new LinkedHashMap<>();

    public void addAll(List<CitationDto> items) {
        if (items == null) {
            return;
        }
        for (CitationDto item : items) {
            if (item == null) {
                continue;
            }
            String key = nullToEmpty(item.policyId())
                    + "|"
                    + nullToEmpty(item.source())
                    + "|"
                    + nullToEmpty(item.section())
                    + "|"
                    + nullToEmpty(item.snippet());
            citations.putIfAbsent(key, item);
        }
    }

    public List<CitationDto> getCitations() {
        return new ArrayList<>(citations.values());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
