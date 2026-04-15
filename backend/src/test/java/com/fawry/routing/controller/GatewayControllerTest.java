package com.fawry.routing.controller;

import com.fawry.routing.dto.response.GatewayResponse;
import com.fawry.routing.exception.GatewayNotFoundException;
import com.fawry.routing.security.AppUserDetailsService;
import com.fawry.routing.security.JwtAuthenticationEntryPoint;
import com.fawry.routing.security.JwtAuthenticationFilter;
import com.fawry.routing.security.JwtTokenProvider;
import com.fawry.routing.service.GatewayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GatewayController.class)
@AutoConfigureMockMvc(addFilters = false)
class GatewayControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private GatewayService gatewayService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("GET /api/gateways returns the list from the service")
    void listGateways() throws Exception {
        GatewayResponse gateway = GatewayResponse.builder()
                .id(1L).code("GW1").name("Gateway 1")
                .fixedFee(new BigDecimal("2.00"))
                .percentageFee(new BigDecimal("0.0150"))
                .dailyLimit(new BigDecimal("50000.00"))
                .minTransaction(new BigDecimal("10.00"))
                .maxTransaction(new BigDecimal("5000.00"))
                .processingTimeMinutes(0)
                .available24x7(true)
                .active(true)
                .build();
        when(gatewayService.listAll()).thenReturn(List.of(gateway));

        mockMvc.perform(get("/api/gateways"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("GW1"))
                .andExpect(jsonPath("$[0].fixedFee").value(2.00));
    }

    @Test
    @DisplayName("GET /api/gateways/{id} returns 404 when gateway is missing")
    void returns404ForMissingGateway() throws Exception {
        when(gatewayService.get(99L)).thenThrow(new GatewayNotFoundException(99L));

        mockMvc.perform(get("/api/gateways/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GATEWAY_NOT_FOUND"));
    }
}
