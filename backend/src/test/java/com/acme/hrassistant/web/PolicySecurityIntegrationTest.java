package com.acme.hrassistant.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.hrassistant.support.PostgresTestcontainers;
import com.acme.hrassistant.support.TestAiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAiConfig.class)
class PolicySecurityIntegrationTest extends PostgresTestcontainers {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void nonAdminUploadIsDenied() throws Exception {
        String token = login("priya.employee", "password");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.md",
                "text/markdown",
                "# Hello\n\nWorld".getBytes());

        mockMvc.perform(multipart("/api/policies")
                        .file(file)
                        .param("name", "Should Fail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUploadMarkdown() throws Exception {
        String token = login("admin.hr", "password");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wfh-extra.md",
                "text/markdown",
                "# Extra WFH\n\nFridays are fine for demo.".getBytes());

        mockMvc.perform(multipart("/api/policies")
                        .file(file)
                        .param("name", "Extra WFH Demo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("Extra WFH Demo"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }
}
