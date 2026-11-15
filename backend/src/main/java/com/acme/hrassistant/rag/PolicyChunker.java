package com.acme.hrassistant.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Splits markdown/text by headings, then by size (~500–800 tokens ≈ 2000–3200 chars).
 */
@Component
public class PolicyChunker {

    private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,6})\\s+(.+)$");
    private static final int TARGET_CHARS = 2800;
    private static final int OVERLAP_CHARS = 350;

    public List<Chunk> chunk(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<Section> sections = splitByHeadings(content);
        List<Chunk> chunks = new ArrayList<>();
        for (Section section : sections) {
            chunks.addAll(splitSection(section.title(), section.body()));
        }
        return chunks;
    }

    private List<Section> splitByHeadings(String content) {
        Matcher matcher = HEADING.matcher(content);
        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(2).trim());
        }

        if (starts.isEmpty()) {
            return List.of(new Section("General", content.trim()));
        }

        List<Section> sections = new ArrayList<>();
        if (starts.getFirst() > 0) {
            String preamble = content.substring(0, starts.getFirst()).trim();
            if (!preamble.isEmpty()) {
                sections.add(new Section("Introduction", preamble));
            }
        }

        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i);
            int to = (i + 1 < starts.size()) ? starts.get(i + 1) : content.length();
            String body = content.substring(from, to).trim();
            if (!body.isEmpty()) {
                sections.add(new Section(titles.get(i), body));
            }
        }
        return sections;
    }

    private List<Chunk> splitSection(String section, String body) {
        if (body.length() <= TARGET_CHARS) {
            return List.of(new Chunk(section, body));
        }

        List<Chunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < body.length()) {
            int end = Math.min(body.length(), start + TARGET_CHARS);
            if (end < body.length()) {
                int breakAt = body.lastIndexOf('\n', end);
                if (breakAt > start + TARGET_CHARS / 2) {
                    end = breakAt;
                }
            }
            String piece = body.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(new Chunk(section, piece));
            }
            if (end >= body.length()) {
                break;
            }
            start = Math.max(end - OVERLAP_CHARS, start + 1);
        }
        return chunks;
    }

    public record Chunk(String section, String text) {
    }

    private record Section(String title, String body) {
    }
}
