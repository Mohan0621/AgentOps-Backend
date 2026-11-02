Write-Host "Starting IncidentIQ Backend Server on http://localhost:8080 ..." -ForegroundColor Cyan
Set-Location -Path "D:\Mohan\AgentOps\agentops"

if (-not $env:DB_URL) { $env:DB_URL = "jdbc:mysql://localhost:3306/agentops_db" }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "12345678" }
if (-not $env:JWT_SECRET) { $env:JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" }
if (-not $env:JWT_EXPIRATION_MS) { $env:JWT_EXPIRATION_MS = "86400000" }

java "-Dspring.classformat.ignore=true" -jar target\agentops-0.0.1-SNAPSHOT.jar
