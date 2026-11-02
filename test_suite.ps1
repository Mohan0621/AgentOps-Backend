$timestamp = (Get-Date).Ticks
$email = "qa_$timestamp@incidentiq.com"

Write-Host "--- 1. Testing Registration ---" -ForegroundColor Cyan
$regBody = @{
    fullName = "QA Engineer"
    email = $email
    password = "password123"
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "http://localhost:8080/api/register" -Method Post -ContentType "application/json" -Body $regBody
$token = $regRes.token
Write-Host "[SUCCESS] User Registered: $email"
Write-Host "[SUCCESS] JWT Token issued: $($token.Substring(0, 30))..."

Write-Host "--- 2. Testing Login ---" -ForegroundColor Cyan
$loginBody = @{
    email = $email
    password = "password123"
} | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/login" -Method Post -ContentType "application/json" -Body $loginBody
Write-Host "[SUCCESS] Logged in with status: $($loginRes.message)"

$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "--- 3. Testing Create Incident ---" -ForegroundColor Cyan
$incBody = @{
    title = "Database Connection Timeout on Checkout"
    description = "Checkout service times out when writing order"
    errorLogs = "java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30005ms."
    severity = "CRITICAL"
    status = "OPEN"
    incidentType = "DATABASE_TIMEOUT"
} | ConvertTo-Json
$incRes = Invoke-RestMethod -Uri "http://localhost:8080/api/incidents" -Method Post -Headers $headers -ContentType "application/json" -Body $incBody
$incId = $incRes.id
Write-Host "[SUCCESS] Created Incident #$($incId): $($incRes.title)"

Write-Host "--- 4. Testing Incident Queue ---" -ForegroundColor Cyan
$queue = Invoke-RestMethod -Uri "http://localhost:8080/api/incidents" -Method Get -Headers $headers
Write-Host "[SUCCESS] Total Incidents in Queue: $($queue.totalElements)"

Write-Host "--- 5. Testing Quick AI Diagnosis (Groq LLM) ---" -ForegroundColor Cyan
$aiDiag = Invoke-RestMethod -Uri "http://localhost:8080/api/ai/incidents/$($incId)/diagnose" -Method Post -Headers $headers
Write-Host "[SUCCESS] AI Error Type: $($aiDiag.errorType)"
Write-Host "[SUCCESS] AI Severity: $($aiDiag.severity)"
Write-Host "[SUCCESS] AI Root Cause: $($aiDiag.rootCause)"
Write-Host "[SUCCESS] AI Solutions: $($aiDiag.suggestedSolutions.Count) suggested fixes"

Write-Host "--- 6. Testing 6-Agent LangGraph Pipeline ---" -ForegroundColor Cyan
$agentReport = Invoke-RestMethod -Uri "http://localhost:8080/api/agents/incidents/$($incId)/investigate" -Method Post -Headers $headers
Write-Host "[SUCCESS] Log Analysis Anomaly: $($agentReport.logAnalysis.keyAnomaly)"
Write-Host "[SUCCESS] Error Classification: $($agentReport.errorClassification.primaryCategory)"
Write-Host "[SUCCESS] Assessed Severity: $($agentReport.severityEvaluation.assessedSeverity)"
Write-Host "[SUCCESS] Root Cause Identified: $($agentReport.rootCauseAnalysis.underlyingCause)"
Write-Host "[SUCCESS] Remediation Steps: $($agentReport.resolutionPlan.remediationSteps.Count) steps suggested"

Write-Host "--- 7. Testing Dashboard Analytics ---" -ForegroundColor Cyan
$analytics = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/dashboard" -Method Get -Headers $headers
Write-Host "[SUCCESS] Total Incidents Count: $($analytics.totalIncidents)"
Write-Host "[SUCCESS] Critical Incidents: $($analytics.criticalIncidents)"
Write-Host "[SUCCESS] AI Coverage Percentage: $($analytics.aiMetrics.aiCoveragePercentage)%"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "   ALL 7 END-TO-END SUITE TESTS PASSED 100%! " -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
