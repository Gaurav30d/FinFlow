package com.finflow.transactionservice.ai.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiClientTest {

    @Test
    @DisplayName("isConfigured returns false when GEMINI_API_KEY is empty or blank")
    void isConfigured_BlankKey_ReturnsFalse() {
        GeminiClient clientEmpty = new GeminiClient("", "gemini-1.5-flash", "https://generativelanguage.googleapis.com", 5000);
        assertThat(clientEmpty.isConfigured()).isFalse();

        GeminiClient clientNull = new GeminiClient(null, "gemini-1.5-flash", "https://generativelanguage.googleapis.com", 5000);
        assertThat(clientNull.isConfigured()).isFalse();

        GeminiClient clientWhitespace = new GeminiClient("   ", "gemini-1.5-flash", "https://generativelanguage.googleapis.com", 5000);
        assertThat(clientWhitespace.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("isConfigured returns true and strips quotes when quotes are present")
    void isConfigured_QuotedKey_SanitizesAndReturnsTrue() {
        GeminiClient client = new GeminiClient("\"AIzaSyDummyKey123\"", "gemini-1.5-flash", "https://generativelanguage.googleapis.com", 5000);
        assertThat(client.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("Strips models/ prefix if configured in GEMINI_MODEL")
    void modelPrefixStripped() {
        GeminiClient client = new GeminiClient("AIzaSyDummyKey123", "models/gemini-2.0-flash", "https://generativelanguage.googleapis.com", 5000);
        assertThat(client.getModel()).isEqualTo("gemini-2.0-flash");
    }

    @Test
    @DisplayName("Automatically upgrades retired gemini-1.5-flash to gemini-flash-lite-latest")
    void autoUpgradesSunsetModel() {
        GeminiClient client = new GeminiClient("AIzaSyDummyKey123", "models/gemini-1.5-flash", "https://generativelanguage.googleapis.com", 5000);
        assertThat(client.getModel()).isEqualTo("gemini-flash-lite-latest");
    }

    @Test
    @DisplayName("generateContent returns Optional.empty() immediately when key is not configured")
    void generateContent_Unconfigured_ReturnsEmptyOptional() {
        GeminiClient client = new GeminiClient("", "gemini-1.5-flash", "https://generativelanguage.googleapis.com", 5000);
        Optional<String> result = client.generateContent("System instructions", "User prompt");
        assertThat(result).isEmpty();
    }
}
