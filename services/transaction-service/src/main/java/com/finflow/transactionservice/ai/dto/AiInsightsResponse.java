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
public class AiInsightsResponse {
    private FinancialSummaryDto summary;
    private String insights;
    private String disclaimer;
}
