package com.finflow.transactionservice.ai.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GeminiClient {

    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-flash-lite-latest}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.timeout-ms:60000}") int timeoutMs) {

        String key = apiKey != null ? apiKey.trim() : "";
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            key = key.substring(1, key.length() - 1).trim();
        }
        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
            key = key.substring(1, key.length() - 1).trim();
        }
        this.apiKey = key;

        String configuredModel = model != null && !model.isBlank() ? model.trim() : "gemini-flash-lite-latest";
        if (configuredModel.startsWith("models/")) {
            configuredModel = configuredModel.substring("models/".length());
        }
        // Auto-upgrade retired/sunset models to active live equivalent
        if ("gemini-1.5-flash".equalsIgnoreCase(configuredModel) || "gemini-2.5-flash".equalsIgnoreCase(configuredModel)) {
            configuredModel = "gemini-flash-lite-latest";
        }
        this.model = configuredModel;

        String cleanBaseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
        if (cleanBaseUrl.endsWith("/v1beta")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - "/v1beta".length());
        }
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(cleanBaseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    public Optional<String> generateContent(String systemInstruction, String userPrompt) {
        if (!isConfigured()) {
            log.info("Gemini API key is not configured. Skipping Gemini API call.");
            return Optional.empty();
        }

        GeminiGenerateRequest request = GeminiGenerateRequest.builder()
                .systemInstruction(systemInstruction != null && !systemInstruction.isBlank()
                        ? GeminiGenerateRequest.Content.builder()
                        .parts(List.of(GeminiGenerateRequest.Part.builder().text(systemInstruction).build()))
                        .build()
                        : null)
                .contents(List.of(
                        GeminiGenerateRequest.Content.builder()
                                .role("user")
                                .parts(List.of(GeminiGenerateRequest.Part.builder().text(userPrompt).build()))
                                .build()
                ))
                .generationConfig(GeminiGenerateRequest.GenerationConfig.builder()
                        .temperature(0.2)
                        .build())
                .build();

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", this.model)
                    .header("x-goog-api-key", this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        int statusCode = clientResponse.getStatusCode().value();
                        byte[] responseBytes = clientResponse.getBody().readAllBytes();
                        String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

                        if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                            String category;
                            String diagnostic;

                            if ((statusCode == 400 && responseBody.contains("API_KEY_INVALID")) || statusCode == 401 || statusCode == 403) {
                                category = "AUTHENTICATION_ERROR";
                                diagnostic = "Google Gemini API key is invalid (API_KEY_INVALID). Please ensure GEMINI_API_KEY is an AI Studio API key (starts with 'AIzaSy...').";
                            } else if (statusCode == 404) {
                                category = "MODEL_NOT_FOUND";
                                diagnostic = "Gemini model '" + this.model + "' was not found or does not support generateContent. Check GEMINI_MODEL setting.";
                            } else if (statusCode == 429) {
                                category = "QUOTA_EXCEEDED";
                                diagnostic = "Gemini rate limit or quota exceeded (HTTP 429). Check your Gemini API usage limits.";
                            } else if (statusCode >= 500) {
                                category = "UPSTREAM_SERVER_ERROR";
                                diagnostic = "Google Gemini server error (HTTP " + statusCode + ").";
                            } else {
                                category = "BAD_REQUEST";
                                diagnostic = "Gemini API request rejected with HTTP " + statusCode + ".";
                            }

                            log.error("Gemini API error [{}]: HTTP status {} - {}", category, statusCode, diagnostic);
                            throw new GeminiException(diagnostic, statusCode, category);
                        }

                        if (responseBytes.length == 0) {
                            return null;
                        }

                        return objectMapper.readValue(responseBytes, GeminiGenerateResponse.class);
                    });

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiGenerateResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null
                        && !candidate.getContent().getParts().isEmpty()) {

                    String combinedText = candidate.getContent().getParts().stream()
                            .map(GeminiGenerateResponse.Part::getText)
                            .filter(t -> t != null && !t.isBlank())
                            .collect(Collectors.joining("\n"));

                    return Optional.of(combinedText);
                }
            }

            return Optional.empty();

        } catch (GeminiException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            String category = "NETWORK_TIMEOUT";
            String diagnostic = "Network timeout or connection failure communicating with Google Gemini API.";
            log.error("Gemini connection error [{}]: {}", category, ex.getMessage());
            throw new GeminiException(diagnostic, ex, 504, category);
        } catch (Exception ex) {
            String category = "UNEXPECTED_ERROR";
            String diagnostic = "Unexpected error communicating with Gemini API: " + ex.getClass().getSimpleName();
            log.error("Unexpected error invoking Gemini [{}]: {}", category, ex.getMessage(), ex);
            throw new GeminiException(diagnostic, ex, 500, category);
        }
    }
}
