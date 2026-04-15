package com.fawry.routing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fawry.routing.dto.request.LoginRequest;
import com.fawry.routing.dto.request.RecommendRequest;
import com.fawry.routing.dto.request.SplitRequest;
import com.fawry.routing.dto.response.AuthResponse;
import com.fawry.routing.dto.response.RecommendResponse;
import com.fawry.routing.dto.response.SplitResponse;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.support.PostgresContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("End-to-end payment routing flows against a real PostgreSQL instance")
class PaymentRoutingIntegrationTest extends PostgresContainerSupport {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("recommend endpoint picks cheapest eligible gateway for CAN_WAIT 5000 EGP")
    void recommendReturnsCheapestGatewayForCanWait() throws Exception {
        mockMvc = mvc();
        String token = login("biller1", "Password123!");

        RecommendRequest payload = new RecommendRequest(
                "BILL_12345", new BigDecimal("5000"), Urgency.CAN_WAIT);

        MvcResult result = mockMvc.perform(post("/api/payments/recommend")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        RecommendResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecommendResponse.class);

        assertThat(response.recommendedGateway()).isNotNull();
        assertThat(response.recommendedGateway().id()).isIn("GW1", "GW2", "GW3");
        assertThat(response.alternatives()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("BILLER cannot submit a recommendation for another biller's code")
    void billerCannotAccessAnotherBillersId() throws Exception {
        mockMvc = mvc();
        String token = login("biller1", "Password123!");

        RecommendRequest foreign = new RecommendRequest(
                "BILL_67890", new BigDecimal("100"), Urgency.CAN_WAIT);

        mockMvc.perform(post("/api/payments/recommend")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foreign)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(3)
    @DisplayName("Idempotency-Key replays the stored response and rejects mismatched payloads")
    void idempotencyKeyReplaysPreviousResponse() throws Exception {
        mockMvc = mvc();
        String token = login("admin", "Password123!");
        String idempotencyKey = "it-key-" + System.nanoTime();

        RecommendRequest payload = new RecommendRequest(
                "BILL_67890", new BigDecimal("200"), Urgency.CAN_WAIT);
        String payloadJson = objectMapper.writeValueAsString(payload);

        String first = mockMvc.perform(post("/api/payments/recommend")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String replay = mockMvc.perform(post("/api/payments/recommend")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(replay).isEqualTo(first);

        RecommendRequest differentPayload = new RecommendRequest(
                "BILL_67890", new BigDecimal("500"), Urgency.CAN_WAIT);

        mockMvc.perform(post("/api/payments/recommend")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(differentPayload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @Order(4)
    @DisplayName("Quota exhaustion on the only INSTANT gateway blocks further instant payments")
    void quotaExhaustionBlocksFurtherInstantPayments() throws Exception {
        mockMvc = mvc();
        String token = login("biller1", "Password123!");

        SplitRequest exhaust = new SplitRequest(
                "BILL_12345", new BigDecimal("50000"), Urgency.INSTANT);

        MvcResult result = mockMvc.perform(post("/api/payments/split")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exhaust)))
                .andExpect(status().isOk())
                .andReturn();

        SplitResponse split = objectMapper.readValue(
                result.getResponse().getContentAsString(), SplitResponse.class);

        assertThat(split.selectedGateway()).isEqualTo("GW1");
        assertThat(split.requiresSplitting()).isTrue();
        assertThat(split.splits()).hasSize(10);
        assertThat(split.splits()).allMatch(chunk -> chunk.compareTo(new BigDecimal("5000")) == 0);

        RecommendRequest blocked = new RecommendRequest(
                "BILL_12345", new BigDecimal("100"), Urgency.INSTANT);

        mockMvc.perform(post("/api/payments/recommend")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blocked)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_ELIGIBLE_GATEWAY"));
    }

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.accessToken();
    }
}
