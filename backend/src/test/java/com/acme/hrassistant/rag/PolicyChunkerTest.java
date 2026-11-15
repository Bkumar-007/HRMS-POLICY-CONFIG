package com.acme.hrassistant.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyChunkerTest {

    private final PolicyChunker chunker = new PolicyChunker();

    @Test
    void splitsByHeadings() {
        String md = """
                # Title

                Intro paragraph.

                ## Casual Leave

                Twelve days per year.

                ## Sick Leave

                Ten days per year.
                """;

        List<PolicyChunker.Chunk> chunks = chunker.chunk(md);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(c -> c.section().contains("Casual")));
        assertTrue(chunks.stream().anyMatch(c -> c.section().contains("Sick")));
    }
}
