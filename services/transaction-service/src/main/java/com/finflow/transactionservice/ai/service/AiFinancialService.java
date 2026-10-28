package com.finflow.transactionservice.ai.service;

import com.finflow.transactionservice.ai.client.GeminiClient;
import com.finflow.transactionservice.ai.client.GeminiException;
import com.finflow.transactionservice.ai.dto.AiChatResponse;
import com.finflow.transactionservice.ai.dto.AiInsightsResponse;
import com.finflow.transactionservice.ai.dto.FinancialSummaryDto;
import com.finflow.transactionservice.ai.dto.TransactionSummaryItemDto;
import com.finflow.transactionservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiFinancialService {

    public static final String DISCLAIMER =
            "This financial analysis is for informational purposes only and does not constitute regulated financial advice.";

    private static final String SYSTEM_PROMPT = """
            You are FinFlow AI, a financial intelligence assistant powered by Google Gemini.
            You provide clear, grounded financial summaries and budgeting observations for the user.
            CRITICAL RULES:
            1. Base your answer strictly on the user's provided financial metrics and completed transactions.
            2. Do NOT invent transactions, merchants, or categories that do not exist in the data.
            3. Do NOT execute, simulate, or claim to execute transfers, deposits, withdrawals, or SQL commands.
            4. If data is insufficient or absent, state so clearly without guessing.
            5. Provide helpful, encouraging, and actionable budgeting observations.
            6. Keep your answers concise, clear, and well-structured.
            """;

    private final FinancialCalculationService financialCalculationService;
    private final GeminiClient geminiClient;

    public AiInsightsResponse getFinancialInsights(Long userId, String token) {
        FinancialSummaryDto summary = financialCalculationService.calculateSummary(userId, token);

        if (!summary.isHasTransactions()) {
            return AiInsightsResponse.builder()
                    .summary(summary)
                    .insights("You do not have any completed transactions in your FinFlow account yet. " +
                            "Once you make deposits, withdrawals, or transfers, FinFlow AI will provide personalized financial intelligence and spending insights.")
                    .disclaimer(DISCLAIMER)
                    .build();
        }

        if (!geminiClient.isConfigured()) {
            String fallbackInsights = buildRuleBasedSummary(summary) +
                    "\n\nNote: Natural language AI insights are currently unconfigured (GEMINI_API_KEY is not set). " +
                    "Your financial totals above are accurately computed from your actual transaction records.";
            return AiInsightsResponse.builder()
                    .summary(summary)
                    .insights(fallbackInsights)
                    .disclaimer(DISCLAIMER)
                    .build();
        }

        String dataContext = buildDataContext(summary);
        String userPrompt = "Please analyze my financial activity, explain the inflow and outflow, and offer practical budgeting suggestions based on this data:\n"
                + dataContext;

        String insights;
        try {
            Optional<String> completion = geminiClient.generateContent(SYSTEM_PROMPT, userPrompt);
            insights = completion.orElseGet(() -> buildRuleBasedSummary(summary));
        } catch (GeminiException ex) {
            log.warn("Gemini API invocation failed, falling back to computed summary: {}", ex.getMessage());
            insights = buildRuleBasedSummary(summary) +
                    "\n\nNote: AI natural language generation is temporarily unavailable. " +
                    "Your calculated financial metrics above reflect your actual completed transactions.";
        }

        return AiInsightsResponse.builder()
                .summary(summary)
                .insights(insights)
                .disclaimer(DISCLAIMER)
                .build();
    }

    public AiChatResponse chat(Long userId, String token, String question) {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("Question must not be blank");
        }

        FinancialSummaryDto summary = financialCalculationService.calculateSummary(userId, token);

        if (!summary.isHasTransactions()) {
            return AiChatResponse.builder()
                    .question(question)
                    .answer("You do not have any completed transactions recorded on FinFlow yet. " +
                            "Your recorded inflow, outflow, and transaction count are currently 0. " +
                            "Once you perform deposits or transfers, I can answer questions about your spending habits.")
                    .groundedSummary(summary)
                    .disclaimer(DISCLAIMER)
                    .build();
        }

        if (!geminiClient.isConfigured()) {
            String answer = "AI natural language chat is currently unconfigured (GEMINI_API_KEY is not set). " +
                    "Based on your actual records, your balance is " + summary.getCurrentBalance() + " " + summary.getCurrency() +
                    ", total inflow is " + summary.getTotalInflow() + " " + summary.getCurrency() +
                    ", total outflow is " + summary.getTotalOutflow() + " " + summary.getCurrency() +
                    ", with " + summary.getCompletedTransactionCount() + " completed transaction(s).";
            return AiChatResponse.builder()
                    .question(question)
                    .answer(answer)
                    .groundedSummary(summary)
                    .disclaimer(DISCLAIMER)
                    .build();
        }

        String dataContext = buildDataContext(summary);
        String userPrompt = "Financial Context:\n" + dataContext + "\n\nUser Question: " + question.trim();

        String answer;
        try {
            Optional<String> completion = geminiClient.generateContent(SYSTEM_PROMPT, userPrompt);
            answer = completion.orElseGet(() ->
                    "Based on your transaction records: your total inflow is " + summary.getTotalInflow() + " " + summary.getCurrency() +
                            ", total outflow is " + summary.getTotalOutflow() + " " + summary.getCurrency() +
                            ", and your current balance is " + summary.getCurrentBalance() + " " + summary.getCurrency() + ".");
        } catch (GeminiException ex) {
            log.warn("Gemini API invocation failed during chat, falling back to grounded answer: {}", ex.getMessage());
            answer = "I am currently experiencing connectivity issues with the AI language service (" + ex.getMessage() + "). " +
                    "However, based on your recorded transactions: your current balance is " + summary.getCurrentBalance() + " " + summary.getCurrency() +
                    ", total inflow is " + summary.getTotalInflow() + " " + summary.getCurrency() +
                    ", and total outflow is " + summary.getTotalOutflow() + " " + summary.getCurrency() + ".";
        }

        return AiChatResponse.builder()
                .question(question)
                .answer(answer)
                .groundedSummary(summary)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private String buildDataContext(FinancialSummaryDto summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("- Current Available Balance: ").append(summary.getCurrentBalance()).append(" ").append(summary.getCurrency()).append("\n");
        sb.append("- Total Inflow: ").append(summary.getTotalInflow()).append(" ").append(summary.getCurrency())
                .append(" (Deposits: ").append(summary.getTotalDeposits())
                .append(", Transfers Received: ").append(summary.getTotalTransfersReceived()).append(")\n");
        sb.append("- Total Outflow: ").append(summary.getTotalOutflow()).append(" ").append(summary.getCurrency())
                .append(" (Withdrawals: ").append(summary.getTotalWithdrawals())
                .append(", Transfers Sent: ").append(summary.getTotalTransfersSent()).append(")\n");
        sb.append("- Net Cash Flow: ").append(summary.getNetCashFlow()).append(" ").append(summary.getCurrency()).append("\n");
        sb.append("- Completed Transactions Count: ").append(summary.getCompletedTransactionCount()).append("\n");

        if (summary.getRecentTransactions() != null && !summary.getRecentTransactions().isEmpty()) {
            sb.append("- Recent Completed Transactions:\n");
            for (TransactionSummaryItemDto item : summary.getRecentTransactions()) {
                sb.append("  * ").append(item.getType()).append(": ").append(item.getAmount()).append(" ").append(item.getCurrency())
                        .append(" - ").append(item.getDescription()).append(" (Ref: ").append(item.getTransactionReference()).append(")\n");
            }
        }
        return sb.toString();
    }

    private String buildRuleBasedSummary(FinancialSummaryDto summary) {
        return "Financial Summary:\n" +
                "• Available Balance: " + summary.getCurrentBalance() + " " + summary.getCurrency() + "\n" +
                "• Total Inflow: " + summary.getTotalInflow() + " " + summary.getCurrency() + " (Deposits: " + summary.getTotalDeposits() + ", Transfers Received: " + summary.getTotalTransfersReceived() + ")\n" +
                "• Total Outflow: " + summary.getTotalOutflow() + " " + summary.getCurrency() + " (Withdrawals: " + summary.getTotalWithdrawals() + ", Transfers Sent: " + summary.getTotalTransfersSent() + ")\n" +
                "• Net Cash Flow: " + summary.getNetCashFlow() + " " + summary.getCurrency() + "\n" +
                "• Total Completed Transactions: " + summary.getCompletedTransactionCount();
    }
}
