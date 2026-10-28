package com.finflow.transactionservice.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.transactionservice.ai.dto.AiChatRequest;
import com.finflow.transactionservice.ai.dto.AiChatResponse;
import com.finflow.transactionservice.ai.dto.AiInsightsResponse;
import com.finflow.transactionservice.ai.dto.FinancialSummaryDto;
import com.finflow.transactionservice.ai.service.AiFinancialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiFinancialService aiFinancialService;

    @InjectMocks
    private AiController aiController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long userId = 101L;
    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController).build();
    }

    @Test
    @DisplayName("GET /api/ai/insights returns grounded financial insights")
    void getInsights_ReturnsInsights() throws Exception {
        FinancialSummaryDto summary = FinancialSummaryDto.builder()
                .totalInflow(new BigDecimal("5000.00"))
                .totalOutflow(new BigDecimal("1500.00"))
                .netCashFlow(new BigDecimal("3500.00"))
                .completedTransactionCount(2)
                .currentBalance(new BigDecimal("3500.00"))
                .currency("INR")
                .hasTransactions(true)
                .build();

        AiInsightsResponse response = AiInsightsResponse.builder()
                .summary(summary)
                .insights("Positive net cash flow observed.")
                .disclaimer(AiFinancialService.DISCLAIMER)
                .build();

        when(aiFinancialService.getFinancialInsights(userId, "Bearer token123"))
                .thenReturn(response);

        mockMvc.perform(get("/api/ai/insights")
                        .principal(authentication)
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalInflow").value(5000.00))
                .andExpect(jsonPath("$.summary.netCashFlow").value(3500.00))
                .andExpect(jsonPath("$.insights").value("Positive net cash flow observed."))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());

        verify(aiFinancialService).getFinancialInsights(userId, "Bearer token123");
    }

    @Test
    @DisplayName("GET /ai/insights direct route also resolves correctly")
    void getInsights_DirectRoute() throws Exception {
        AiInsightsResponse response = AiInsightsResponse.builder()
                .insights("Insights for direct route.")
                .disclaimer(AiFinancialService.DISCLAIMER)
                .build();

        when(aiFinancialService.getFinancialInsights(userId, null))
                .thenReturn(response);

        mockMvc.perform(get("/ai/insights").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").value("Insights for direct route."));
    }

    @Test
    @DisplayName("POST /api/ai/chat returns grounded answer")
    void chat_ValidQuestion_ReturnsAnswer() throws Exception {
        AiChatRequest request = new AiChatRequest("How much did I spend?");
        AiChatResponse response = AiChatResponse.builder()
                .question("How much did I spend?")
                .answer("You spent 1500.00 INR.")
                .disclaimer(AiFinancialService.DISCLAIMER)
                .build();

        when(aiFinancialService.chat(userId, "Bearer token123", "How much did I spend?"))
                .thenReturn(response);

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authentication)
                        .header("Authorization", "Bearer token123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("How much did I spend?"))
                .andExpect(jsonPath("$.answer").value("You spent 1500.00 INR."))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/ai/chat rejects blank question with 400 Bad Request")
    void chat_BlankQuestion_Returns400() throws Exception {
        AiChatRequest request = new AiChatRequest("   ");

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
