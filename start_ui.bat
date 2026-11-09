@echo off
title IncidentIQ JavaFX Desktop Workspace
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0agentops\run-ui.ps1"
if errorlevel 1 pause

