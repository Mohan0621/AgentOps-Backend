Write-Host "Launching IncidentIQ JavaFX Desktop Workspace..." -ForegroundColor Cyan
Set-Location -Path "D:\Mohan\AgentOps\agentops"
mvn compile exec:java "-Dexec.mainClass=com.agentops.UI.Launcher" "-Dmaven.repo.local=D:\m2_repo"

