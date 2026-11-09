package com.agentops.UI;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thread-safe HTTP Client to interact with the IncidentIQ Spring Boot Backend APIs.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String jwtToken = null;
    private static String currentUserEmail = null;

    public static void setAuth(String token, String email) {
        jwtToken = token;
        currentUserEmail = email;
    }

    public static boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.isBlank();
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public static String getJwtToken() {
        return jwtToken;
    }

    public static HttpResponse<String> login(String email, String password) throws Exception {
        String json = objectMapper.writeValueAsString(new LoginPayload(email, password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> register(String fullName, String email, String password) throws Exception {
        String json = objectMapper.writeValueAsString(new RegisterPayload(fullName, email, password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> getIncidents() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/incidents?page=0&size=50&sort=createdAt,desc"))
                .GET();
        if (jwtToken != null) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> createIncident(String title, String description, String errorLogs, String severity) throws Exception {
        return createIncident(title, description, errorLogs, severity, "OPEN", "APPLICATION_BUG");
    }

    public static HttpResponse<String> createIncident(String title, String description, String errorLogs,
                                                      String severity, String status, String incidentType) throws Exception {
        String finalType = (incidentType == null || incidentType.isBlank()) ? "APPLICATION_BUG" : incidentType.trim();
        String finalStatus = (status == null || status.isBlank()) ? "OPEN" : status.trim();
        var payload = new CreateIncidentPayload(title, description, errorLogs, severity, finalStatus, finalType);
        String json = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/incidents"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> updateIncidentStatus(Long incidentId, String status) throws Exception {
        String json = objectMapper.writeValueAsString(new UpdateStatusPayload(status));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/incidents/" + incidentId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> getInvestigations(Long incidentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/incidents/" + incidentId + "/investigations"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> getDashboardMetrics() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/analytics/dashboard"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> runAiDiagnosis(Long incidentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/ai/incidents/" + incidentId + "/diagnose"))
                .header("Authorization", "Bearer " + jwtToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> runMultiAgentInvestigation(Long incidentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/agents/incidents/" + incidentId + "/investigate"))
                .header("Authorization", "Bearer " + jwtToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static record LoginPayload(String email, String password) {}
    public static record RegisterPayload(String fullName, String email, String password) {}
    public static record CreateIncidentPayload(String title, String description, String errorLogs, String severity, String status, String incidentType) {}
    public static record UpdateStatusPayload(String status) {}
}
