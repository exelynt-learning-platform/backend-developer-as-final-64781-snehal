package com.assignment.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAndGetToken("admin", "Admin@123");
        userToken = loginAndGetToken("user", "User@123");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void listResourcesWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanListResources() throws Exception {
        mockMvc.perform(get("/resources").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void userCannotCreateResource_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Rogue Room",
                "type", "ROOM",
                "description", "should not be created",
                "available", true
        );
        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteResource() throws Exception {
        Map<String, Object> createBody = Map.of(
                "name", "Meeting Pod 1",
                "type", "ROOM",
                "description", "small pod",
                "available", true
        );

        String createResponse = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createResponse).get("id").asLong();

        Map<String, Object> updateBody = Map.of(
                "name", "Meeting Pod 1 - Updated",
                "type", "ROOM",
                "description", "small pod updated",
                "available", false
        );

        mockMvc.perform(put("/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Meeting Pod 1 - Updated"))
                .andExpect(jsonPath("$.available").value(false));

        mockMvc.perform(delete("/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createResourceWithMissingRequiredFields_returns400() throws Exception {
        Map<String, Object> invalidBody = Map.of("description", "no name or type");

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}
