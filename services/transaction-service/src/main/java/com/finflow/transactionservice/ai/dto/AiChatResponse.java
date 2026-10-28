package com.finflow.transactionservice.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {
    private String question;
    private String answer;
    private FinancialSummaryDto groundedSummary;
    private String disclaimer;
}
