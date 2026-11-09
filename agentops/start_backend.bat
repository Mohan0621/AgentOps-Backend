@echo off
echo Starting IncidentIQ Backend Server on http://localhost:8080 ...
cd /d "%~dp0"

if "%DB_URL%"=="" set DB_URL=jdbc:mysql://localhost:3306/agentops_db
if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%DB_PASSWORD%"=="" set DB_PASSWORD=12345678
if "%JWT_SECRET%"=="" set JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
if "%JWT_EXPIRATION_MS%"=="" set JWT_EXPIRATION_MS=86400000

if not exist "target\agentops-0.0.1-SNAPSHOT.jar" (
    echo Backend JAR not found; packaging application...
    call mvn package -DskipTests -Dmaven.repo.local=D:\m2_repo
    if errorlevel 1 exit /b 1
)

java "-Dspring.classformat.ignore=true" -jar target\agentops-0.0.1-SNAPSHOT.jar
