$authBody = @{
    fullName = "Incident Commander"
    email = "admin@incidentiq.com"
    password = "password123"
} | ConvertTo-Json

try {
    $reg = Invoke-RestMethod -Uri "http://localhost:8080/api/login" -Method Post -ContentType "application/json" -Body $authBody -ErrorAction Stop
} catch {
    $reg = Invoke-RestMethod -Uri "http://localhost:8080/api/register" -Method Post -ContentType "application/json" -Body $authBody
}
$token = $reg.token

$rawLogs = @"
java.lang.NullPointerException: Cannot invoke 'com.agentops.Models.User.getEmail()' because 'customer' is null
	at com.agentops.Services.OrderService.processOrder(OrderService.java:42)
	at com.agentops.Controllers.WebhookController.handlePaymentSuccess(WebhookController.java:28)
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:188)
"@

$incidentPayload = @{
    title = "NullPointerException in Order Processing Webhook"
    description = "Stripe payment confirmation webhook crashes when payload contains empty customer metadata"
    errorLogs = $rawLogs
    severity = "HIGH"
    status = "OPEN"
    incidentType = "API_WEBHOOK_FAILURE"
} | ConvertTo-Json

$headers = @{ Authorization = "Bearer $token" }

Write-Host "1. Submitting error logs to IncidentIQ..." -ForegroundColor Cyan
$newInc = Invoke-RestMethod -Uri "http://localhost:8080/api/incidents" -Method Post -Headers $headers -ContentType "application/json" -Body $incidentPayload
Write-Host "[SUCCESS] Incident #$($newInc.id) created: $($newInc.title)" -ForegroundColor Green

Write-Host ""
Write-Host "2. Running Quick AI Diagnosis (Groq LLM)..." -ForegroundColor Cyan
$aiRes = Invoke-RestMethod -Uri "http://localhost:8080/api/ai/incidents/$($newInc.id)/diagnose" -Method Post -Headers $headers
Write-Host "---------------- AI DIAGNOSIS RESULT ----------------" -ForegroundColor Yellow
Write-Host "Error Type:         $($aiRes.errorType)"
Write-Host "Assessed Severity:  $($aiRes.severity)"
Write-Host "Affected Component: $($aiRes.affectedComponent)"
Write-Host "Root Cause:         $($aiRes.rootCause)"
Write-Host "Suggested Fixes:"
foreach ($s in $aiRes.suggestedSolutions) {
    Write-Host "  * $s"
}

Write-Host ""
Write-Host "3. Orchestrating 6-Agent LangGraph Pipeline..." -ForegroundColor Cyan
$multiAgent = Invoke-RestMethod -Uri "http://localhost:8080/api/agents/incidents/$($newInc.id)/investigate" -Method Post -Headers $headers
Write-Host "---------------- MULTI-AGENT REPORT -----------------" -ForegroundColor Yellow
Write-Host "Error Category:       $($multiAgent.errorClassification.primaryCategory) (Confidence: $($multiAgent.errorClassification.confidenceScore * 100)%)"
Write-Host "Blast Radius:         $($multiAgent.severityEvaluation.blastRadius)"
Write-Host "Identified Root Cause: $($multiAgent.rootCauseAnalysis.underlyingCause)"
Write-Host "Immediate Mitigation: $($multiAgent.resolutionPlan.immediateMitigation)"
Write-Host "Suggested Code Patch:"
Write-Host "$($multiAgent.resolutionPlan.suggestedCodePatch)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Executive Summary:"
Write-Host "$($multiAgent.finalReport.executiveSummary)" -ForegroundColor Green

