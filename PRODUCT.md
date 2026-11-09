# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop-javafx (Java 21 target on JDK 26.0.2 runtime, JavaFX 21.0.2 desktop shell + targeted embedded JS/Canvas WebView components where native JavaFX cannot match interactive shader/globe animations, backed by Spring Boot 3.3.4 REST APIs)

## Users

- **Primary User:** SREs, DevOps engineers, and backend developers triaging production exceptions, stack traces, and outages.
- **Secondary User:** Engineering leads and platform managers monitoring MTTR, incident distribution, and AI investigation coverage across teams.

## Product Purpose

**IncidentIQ (AgentOps)** is a centralized AI-powered incident management and collaborative root-cause investigation desktop platform. Users ingest incident error logs and stack traces, run sub-second LLM triage (`Groq` / `Gemini`) or a **6-agent orchestration pipeline** (`Log Analysis -> Classification -> Severity Evaluation -> Root Cause -> Resolution -> Report Generation`), track status lifecycle, and monitor operational telemetry.

## Positioning

Unlike static issue trackers or plain chat wrappers bolted onto legacy dashboards, IncidentIQ orchestrates an autonomous 6-agent investigation workflow over JWT-secured Spring Boot APIs — transforming noisy raw stack traces directly into structured root-cause diagnoses, severity evaluations, remediation playbooks, and live MTTR metrics.

## Operating Context

- Desktop JavaFX workspace (`com.agentops.UI.Launcher` / `IncidentIqApp`) talking to the local Spring Boot backend (`http://localhost:8080`).
- Supported endpoints already implemented in backend:
  - Auth: `POST /api/login`, `POST /api/register` (JWT Bearer token)
  - Incidents: `GET /api/incidents`, `POST /api/incidents`, `PUT /api/incidents/{id}`, `DELETE /api/incidents/{id}`
  - AI Triage: `POST /api/ai/incidents/{id}/diagnose`
  - 6-Agent Pipeline: `POST /api/agents/incidents/{id}/investigate`
  - Analytics: `GET /api/analytics/dashboard`

## Capabilities and Constraints

- Must run on JavaFX (`IncidentIqApp`), built via Maven with `-Dmaven.repo.local=D:\m2_repo` and launched via `start_ui.ps1` / `start_ui.bat`.
- Public showcase pages (`Home`, `Platform`, `For Teams`, `About`) + `Login` + inside workspace (`Incidents`, `Report`, `Analytics`) — inside workspace layout updates pending final layout reference from user; public pages and overall design system can begin immediately.
- Bundled static geometric sans typography (`PlusJakartaSans-*.ttf` in `src/main/resources/fonts/`).
- Do not fabricate commercial claims, customer logos, or unimplemented API capabilities; all telemetry/demonstration traces shown on showcase pages must be clearly framed as product demonstrations of real IncidentIQ pipeline stages.

## Brand Commitments

- **Product Name:** IncidentIQ (by AgentOps)
- **Pinned Visual Reference:** Strictly modeled on `https://www.serko.ai/` across landing page (`Home`, `Platform`, `For Teams`, `About`), authentication, and inside workspace surfaces — adapting copy and domain visuals to IncidentIQ while keeping the exact light editorial + horizon glow visual world.
- **Inferred Workflow Note:** User explicitly instructed to begin work and approved using JS selectively where JavaFX cannot replicate effects natively (`code-first` workflow applied since user asked to start directly).

