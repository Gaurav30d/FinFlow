package com.finflow.transactionservice.ai.controller;

import com.finflow.transactionservice.ai.dto.AiChatRequest;
import com.finflow.transactionservice.ai.dto.AiChatResponse;
import com.finflow.transactionservice.ai.dto.AiInsightsResponse;
import com.finflow.transactionservice.ai.service.AiFinancialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/ai", "/ai"})
@RequiredArgsConstructor
public class AiController {

    private final AiFinancialService aiFinancialService;

    @GetMapping("/insights")
    public ResponseEntity<AiInsightsResponse> getInsights(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Long userId = (Long) authentication.getPrincipal();
        AiInsightsResponse response = aiFinancialService.getFinancialInsights(userId, token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody AiChatRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        AiChatResponse response = aiFinancialService.chat(userId, token, request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
