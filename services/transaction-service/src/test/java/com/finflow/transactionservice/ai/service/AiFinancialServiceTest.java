package com.finflow.transactionservice.ai.service;

import com.finflow.transactionservice.ai.client.GeminiClient;
import com.finflow.transactionservice.ai.client.GeminiException;
import com.finflow.transactionservice.ai.dto.AiChatResponse;
import com.finflow.transactionservice.ai.dto.AiInsightsResponse;
import com.finflow.transactionservice.ai.dto.FinancialSummaryDto;
import com.finflow.transactionservice.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiFinancialServiceTest {

    @Mock
    private FinancialCalculationService financialCalculationService;

    @Mock
    private GeminiClient geminiClient;

    private AiFinancialService aiFinancialService;

    private final Long userId = 101L;
    private final String token = "Bearer test.token";

    @BeforeEach
    void setUp() {
        aiFinancialService = new AiFinancialService(financialCalculationService, geminiClient);
    }

    private FinancialSummaryDto createSampleSummary(boolean hasTransactions) {
        if (!hasTransactions) {
            return FinancialSummaryDto.builder()
                    .totalInflow(BigDecimal.ZERO)
                    .totalOutflow(BigDecimal.ZERO)
                    .netCashFlow(BigDecimal.ZERO)
                    .completedTransactionCount(0)
                    .currentBalance(BigDecimal.ZERO)
                    .currency("INR")
                    .hasTransactions(false)
                    .recentTransactions(Collections.emptyList())
                    .build();
        }

        return FinancialSummaryDto.builder()
                .totalInflow(new BigDecimal("5000.00"))
                .totalOutflow(new BigDecimal("2000.00"))
                .netCashFlow(new BigDecimal("3000.00"))
                .totalDeposits(new BigDecimal("5000.00"))
                .totalWithdrawals(new BigDecimal("500.00"))
                .totalTransfersSent(new BigDecimal("1500.00"))
                .totalTransfersReceived(BigDecimal.ZERO)
                .completedTransactionCount(3)
                .currentBalance(new BigDecimal("3000.00"))
                .currency("INR")
                .hasTransactions(true)
                .recentTransactions(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Empty transaction history returns clear explanation without calling Gemini")
    void getFinancialInsights_EmptyHistory_ReturnsClearExplanation() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(false));

        AiInsightsResponse response = aiFinancialService.getFinancialInsights(userId, token);

        assertThat(response.getSummary().isHasTransactions()).isFalse();
        assertThat(response.getInsights()).contains("do not have any completed transactions");
        assertThat(response.getDisclaimer()).isEqualTo(AiFinancialService.DISCLAIMER);
        verify(geminiClient, never()).generateContent(anyString(), anyString());
    }

    @Test
    @DisplayName("Missing API key returns calculated summary with unconfigured notice without throwing")
    void getFinancialInsights_MissingApiKey_ReturnsCalculatedSummary() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(true));
        when(geminiClient.isConfigured()).thenReturn(false);

        AiInsightsResponse response = aiFinancialService.getFinancialInsights(userId, token);

        assertThat(response.getSummary().isHasTransactions()).isTrue();
        assertThat(response.getInsights()).contains("GEMINI_API_KEY is not set");
        assertThat(response.getInsights()).contains("Total Inflow: 5000.00 INR");
        verify(geminiClient, never()).generateContent(anyString(), anyString());
    }

    @Test
    @DisplayName("Configured API key invokes Gemini and returns generated insights")
    void getFinancialInsights_ConfiguredApiKey_ReturnsAiInsights() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(true));
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateContent(anyString(), anyString()))
                .thenReturn(Optional.of("You have a strong positive net cash flow of 3000.00 INR. Consider budgeting 20% for savings."));

        AiInsightsResponse response = aiFinancialService.getFinancialInsights(userId, token);

        assertThat(response.getInsights()).contains("positive net cash flow");
        assertThat(response.getDisclaimer()).isEqualTo(AiFinancialService.DISCLAIMER);
        verify(geminiClient).generateContent(anyString(), anyString());
    }

    @Test
    @DisplayName("Upstream Gemini failure is handled gracefully with grounded fallback")
    void getFinancialInsights_UpstreamFailure_HandledGracefully() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(true));
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateContent(anyString(), anyString()))
                .thenThrow(new GeminiException("Gemini API client error: 500"));

        AiInsightsResponse response = aiFinancialService.getFinancialInsights(userId, token);

        assertThat(response.getInsights()).contains("AI natural language generation is temporarily unavailable");
        assertThat(response.getInsights()).contains("Total Inflow: 5000.00 INR");
        assertThat(response.getDisclaimer()).isEqualTo(AiFinancialService.DISCLAIMER);
    }

    @Test
    @DisplayName("Chat with empty history returns immediate grounded answer without calling Gemini")
    void chat_EmptyHistory_ReturnsImmediateAnswer() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(false));

        AiChatResponse response = aiFinancialService.chat(userId, token, "How much did I spend?");

        assertThat(response.getAnswer()).contains("do not have any completed transactions");
        verify(geminiClient, never()).generateContent(anyString(), anyString());
    }

    @Test
    @DisplayName("Chat with missing API key returns grounded summary answer")
    void chat_MissingApiKey_ReturnsGroundedSummary() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(true));
        when(geminiClient.isConfigured()).thenReturn(false);

        AiChatResponse response = aiFinancialService.chat(userId, token, "What is my balance?");

        assertThat(response.getAnswer()).contains("GEMINI_API_KEY is not set");
        assertThat(response.getAnswer()).contains("balance is 3000.00 INR");
        verify(geminiClient, never()).generateContent(anyString(), anyString());
    }

    @Test
    @DisplayName("Chat with configured API key returns Gemini generated answer")
    void chat_ConfiguredApiKey_ReturnsLlmAnswer() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(true));
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateContent(anyString(), anyString()))
                .thenReturn(Optional.of("Your total spending is 2000.00 INR, consisting of 500.00 INR withdrawals and 1500.00 INR in transfers sent."));

        AiChatResponse response = aiFinancialService.chat(userId, token, "How much did I spend?");

        assertThat(response.getAnswer()).contains("Your total spending is 2000.00 INR");
        assertThat(response.getGroundedSummary()).isNotNull();
        assertThat(response.getDisclaimer()).isEqualTo(AiFinancialService.DISCLAIMER);
    }

    @Test
    @DisplayName("Chat with upstream API failure returns grounded fallback answer")
    void chat_UpstreamFailure_ReturnsGroundedFallback() {
        when(financialCalculationService.calculateSummary(userId, token))
                .thenReturn(createSampleSummary(true));
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateContent(anyString(), anyString()))
                .thenThrow(new GeminiException("Connection timeout"));

        AiChatResponse response = aiFinancialService.chat(userId, token, "What are my expenses?");

        assertThat(response.getAnswer()).contains("connectivity issues");
        assertThat(response.getAnswer()).contains("total outflow is 2000.00 INR");
    }

    @Test
    @DisplayName("Chat with blank question throws BadRequestException")
    void chat_BlankQuestion_ThrowsBadRequestException() {
        assertThatThrownBy(() -> aiFinancialService.chat(userId, token, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Question must not be blank");
    }
}
