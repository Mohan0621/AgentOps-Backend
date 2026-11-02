package com.agentops.Services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.agentops.DTO.AiDiagnosisRequest;
import com.agentops.DTO.AiDiagnosisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiDiagnosisService {

    @Value("${ai.provider:groq}") // Options: groq, gemini
    private String aiProvider;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiDiagnosisResult diagnose(AiDiagnosisRequest request) {
        String prompt = buildPrompt(request);

        try {
            if ("gemini".equalsIgnoreCase(aiProvider) && geminiApiKey != null && !geminiApiKey.isBlank()) {
                return callGemini(prompt);
            } else if (groqApiKey != null && !groqApiKey.isBlank()) {
                return callGroq(prompt);
            } else {
                return fallbackRuleBasedDiagnosis(request);
            }
        } catch (Exception ex) {
            // Log warning and fallback gracefully to rule-based diagnostic
            return fallbackRuleBasedDiagnosis(request);
        }
    }

    private String buildPrompt(AiDiagnosisRequest req) {
        return """
                You are an expert site reliability and software debugging AI assistant for the IncidentIQ platform.
                Analyze the incident details below and diagnose the issue.

                INCIDENT TITLE: %s
                DESCRIPTION: %s
                ERROR LOGS / STACK TRACE:
                %s

                You MUST return ONLY a raw JSON object (with NO markdown formatting, no ```json code fences) with the exact keys:
                {
                  "errorType": "<type of error or exception name, e.g. NullPointerException>",
                  "severity": "<LOW | MEDIUM | HIGH | CRITICAL>",
                  "rootCause": "<clear, concise explanation of the root cause>",
                  "affectedComponent": "<service, class or component name identified in trace>",
                  "suggestedSolutions": [
                    "<step 1>",
                    "<step 2>",
                    "<step 3>"
                  ]
                }
                """.formatted(
                req.getTitle() != null ? req.getTitle() : "N/A",
                req.getDescription() != null ? req.getDescription() : "N/A",
                req.getErrorLogs() != null ? req.getErrorLogs() : "N/A"
        );
    }

    private AiDiagnosisResult callGroq(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", groqModel);
        body.put("messages", Collections.singletonList(message));
        body.put("temperature", 0.1);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(groqApiUrl, HttpMethod.POST, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String rawJson = root.path("choices").get(0).path("message").path("content").asText();
        return parseDiagnosisJson(rawJson);
    }

    private AiDiagnosisResult callGemini(String prompt) throws Exception {
        String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> contents = Map.of("parts", Collections.singletonList(part));
        Map<String, Object> body = Map.of("contents", Collections.singletonList(contents));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String rawText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        return parseDiagnosisJson(cleanJsonOutput(rawText));
    }

    private String cleanJsonOutput(String raw) {
        String clean = raw.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }

    private AiDiagnosisResult parseDiagnosisJson(String jsonString) {
        try {
            return objectMapper.readValue(cleanJsonOutput(jsonString), AiDiagnosisResult.class);
        } catch (Exception e) {
            return fallbackRuleBasedDiagnosis(new AiDiagnosisRequest());
        }
    }

    /**
     * Fallback heuristic engine if external API key is not configured or network call fails
     */
    private AiDiagnosisResult fallbackRuleBasedDiagnosis(AiDiagnosisRequest req) {
        String logs = (req.getErrorLogs() != null ? req.getErrorLogs() : "") + " " +
                      (req.getDescription() != null ? req.getDescription() : "");

        String errorType = "UnknownException";
        String severity = "HIGH";
        String rootCause = "An unhandled runtime error was triggered in application execution.";
        String affectedComponent = "ApplicationCore";
        List<String> solutions = new ArrayList<>();

        if (logs.contains("NullPointerException")) {
            errorType = "NullPointerException";
            severity = "HIGH";
            rootCause = "An object reference was accessed or invoked before initialization.";
            affectedComponent = extractComponentFromStack(logs, "Service");
            solutions.add("Check for null values before invoking methods");
            solutions.add("Add null validation via Objects.requireNonNull() or @NotNull");
            solutions.add("Review object initialization lifecycle and dependency injection");
        } else if (logs.contains("SQLException") || logs.contains("Communications link failure") || logs.contains("Hikari")) {
            errorType = "DatabaseConnectionException";
            severity = "CRITICAL";
            rootCause = "Database server is unreachable or pool connections timed out.";
            affectedComponent = "DatabasePoolManager";
            solutions.add("Check database connectivity, credentials, and host availability");
            solutions.add("Inspect maximum pool size and idle connection timeouts");
            solutions.add("Review slow running queries locking connection threads");
        } else if (logs.contains("AccessDeniedException") || logs.contains("403") || logs.contains("401")) {
            errorType = "UnauthorizedAccessException";
            severity = "MEDIUM";
            rootCause = "Security credentials are missing, expired, or have insufficient privileges.";
            affectedComponent = "SecurityConfig";
            solutions.add("Verify JWT Bearer token validity and expiration");
            solutions.add("Check user role permissions for the target endpoint");
            solutions.add("Review Spring Security filter chain rule matching");
        } else {
            solutions.add("Inspect the complete stack trace and contextual application logs");
            solutions.add("Verify input payload format and constraints");
            solutions.add("Add defensive checks and comprehensive unit/integration tests");
        }

        return AiDiagnosisResult.builder()
                .errorType(errorType)
                .severity(severity)
                .rootCause(rootCause)
                .affectedComponent(affectedComponent)
                .suggestedSolutions(solutions)
                .build();
    }

    private String extractComponentFromStack(String text, String fallback) {
        if (text.contains("com.agentops.")) {
            int start = text.indexOf("com.agentops.");
            int end = text.indexOf("(", start);
            if (end > start) {
                String sub = text.substring(start, end);
                String[] parts = sub.split("\\.");
                if (parts.length > 0) {
                    return parts[parts.length - 2];
                }
            }
        }
        return fallback;
    }
}

