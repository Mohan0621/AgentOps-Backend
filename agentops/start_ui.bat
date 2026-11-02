@echo off
title IncidentIQ JavaFX Desktop Workspace
cd /d D:\Mohan\AgentOps\agentops
echo Launching IncidentIQ Desktop UI...
mvn compile exec:java "-Dexec.mainClass=com.agentops.UI.Launcher" "-Dmaven.repo.local=D:\m2_repo"
pause

