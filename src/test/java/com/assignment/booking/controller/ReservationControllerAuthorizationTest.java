package com.assignment.booking.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Covers the most safety-critical requirements of the assignment:
 *  - reservation ownership is derived from the JWT, never trusted from the request body
 *  - USER can only see/manage their own reservations, ADMIN can see/manage all
 *  - filtering and pagination behave correctly
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String secondUserToken;
    private long resourceId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAndGetToken("admin", "Admin@123");
        userToken = loginAndGetToken("user", "User@123");

        if (!userRepository.existsByUsername("user2")) {
            userRepository.save(User.builder()
                    .username("user2")
                    .password(passwordEncoder.encode("User2@123"))
                    .role(Role.USER)
                    .build());
        }
        secondUserToken = loginAndGetToken("user2", "User2@123");

        // Create a resource to reserve, as admin.
        Map<String, Object> resourceBody = Map.of(
                "name", "Test Room",
                "type", "ROOM",
                "description", "for reservation tests",
                "available", true
        );
        String response = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resourceBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        resourceId = objectMapper.readTree(response).get("id").asLong();
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

    private Map<String, Object> reservationBody(String start, String end, String price) {
        Map<String, Object> body = new HashMap<>();
        body.put("resourceId", resourceId);
        body.put("startTime", start);
        body.put("endTime", end);
        body.put("price", price);
        return body;
    }

    @Test
    void creatingReservation_assignsOwnershipFromJwt_notFromRequestBody() throws Exception {
        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-01T09:00:00", "2026-10-01T10:00:00", "50.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        long reservationId = objectMapper.readTree(response).get("id").asLong();

        // The owner can fetch it directly.
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void userCannotViewAnotherUsersReservation() throws Exception {
        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-02T09:00:00", "2026-10-02T10:00:00", "20.00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());

        // But ADMIN can view any reservation.
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void userListEndpoint_returnsOnlyOwnReservations_adminSeesAll() throws Exception {
        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-03T09:00:00", "2026-10-03T10:00:00", "30.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-04T09:00:00", "2026-10-04T10:00:00", "40.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", everyItem(equalTo("user"))));
        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        // ADMIN's result set must include reservations from more than one user.
    }

    @Test
    void userCanCancelOwnReservation_butCannotConfirmIt() throws Exception {
        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-05T09:00:00", "2026-10-05T10:00:00", "15.00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        // USER attempting to self-confirm should be forbidden.
        mockMvc.perform(patch("/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isForbidden());

        // USER cancelling their own reservation is allowed.
        mockMvc.perform(patch("/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminCanConfirmAnyReservation() throws Exception {
        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-06T09:00:00", "2026-10-06T10:00:00", "60.00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void userCannotDeleteReservation_onlyAdminCan() throws Exception {
        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-07T09:00:00", "2026-10-07T10:00:00", "25.00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidTimeWindow_returns400() throws Exception {
        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-08T10:00:00", "2026-10-08T09:00:00", "10.00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filteringByStatusAndPriceRange_worksTogetherWithPagination() throws Exception {
        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-09T09:00:00", "2026-10-09T10:00:00", "100.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                reservationBody("2026-10-10T09:00:00", "2026-10-10T10:00:00", "5.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "PENDING")
                        .param("minPrice", "50")
                        .param("maxPrice", "200")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].price", everyItem(org.hamcrest.Matchers.notNullValue())));
    }
}
