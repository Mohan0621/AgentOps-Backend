# IncidentIQ: Project Specification & Architecture

## Executive Abstract
Traditional software development and incident management often involve fragmented workflows across error monitoring tools, log analysis systems, debugging environments, and team communication platforms. **IncidentIQ** is a centralized AI-powered incident management and collaborative investigation platform that integrates intelligent debugging, automated root cause analysis, and multi-agent AI assistance into a unified workspace.

---

## System Architecture

```mermaid
graph TD
    subgraph Client / User Interface
        UI[IncidentIQ Web Workspace]
    end

    subgraph Spring Boot Backend (agentops)
        API[REST API Gateway / Auth Controllers]
        SEC[Spring Security & JWT Filter]
        SRV[Business Services: Incidents, Users, Workspaces]
        JPA[Spring Data JPA & Hibernate]
        DB[(MySQL / PostgreSQL)]
    end

    subgraph AI Investigation Core
        ORCH[Agent Orchestrator - LangGraph / LangChain]
        A1[Log Analyzer Agent]
        A2[Diagnosis & Tracing Agent]
        A3[Code Context Agent]
        A4[Root Cause Analysis Agent]
        A5[Resolution & Patch Assistant]
        RAG[(Vector DB / RAG Knowledge Store)]
    end

    UI --> API
    API --> SEC
    SEC --> SRV
    SRV --> JPA
    JPA --> DB
    SRV <--> ORCH
    ORCH --> A1
    ORCH --> A2
    ORCH --> A3
    ORCH --> A4
    ORCH --> A5
    ORCH <--> RAG
```

---

## 8 Core Pillars & Detailed Specifications

### 1. Incident Management Dashboard
- **Incident Lifecycle Management**: States: `OPEN`, `TRIAGED`, `INVESTIGATING`, `MITIGATED`, `RESOLVED`, `CLOSED`.
- **Severity Classification**:
  - `P1 (Critical)`: System outage / major data impairment.
  - `P2 (High)`: Significant service degradation.
  - `P3 (Medium)`: Partial functionality impaired.
  - `P4 (Low)`: Minor bugs or informational anomalies.
- **Assignment & SLA**: Direct ownership allocation, team escalation policies, and SLA tracking.

### 2. AI Log Analyzer
- Ingestion of structured (JSON, Logfmt) and unstructured logs / stack traces.
- Pattern extraction, rate anomaly detection, and clustering of recurring failures.
- Direct correlation between log spikes and open incidents.

### 3. Root Cause Analysis (RCA) Engine
- Trace correlation across execution environments.
- Automated hypothesis generation detailing the trigger, faulty component, and contributing factors.
- Outputs human-readable, auditable RCA reports with confidence metrics.

### 4. Multi-Agent Investigation System
- Orchestrated via **LangGraph** / **LangChain**:
  - **Log Analysis Agent**: Dissects raw telemetry and isolates failure windows.
  - **Error Diagnosis Agent**: Evaluates error taxonomy, exceptions, and system state.
  - **Code Investigation Agent**: Cross-references traces with git repositories and recent commits.
  - **RCA Agent**: Unifies agent findings into a single root cause conclusion.
  - **Resolution Agent**: Synthesizes actionable remediation plans.

### 5. Collaborative Incident Workspace
- Shared live war room for incident commanders and engineers.
- Real-time updates, activity timelines, investigative notes, and threaded discussions.

### 6. AI-Powered Resolution Assistant
- Generates contextual fix suggestions, runbook recommendations, and code patch diffs.
- Evaluates mitigation trade-offs (e.g., service rollback vs. configuration patch vs. hotfix).

### 7. Incident Analytics & Monitoring
- Aggregates operational health metrics: MTTA (Mean Time to Acknowledge), MTTR (Mean Time to Resolve), incident frequencies, and team resolution velocity.
- Tracks AI agent precision, hallucination checks, and recommendation acceptance rates.

### 8. Knowledge Retrieval System (RAG)
- Vector database embedding past post-mortems, internal wikis, standard operating procedures (SOPs), and codebase docs.
- Supplies contextual background to the multi-agent investigation system to ensure accurate, enterprise-specific reasoning.

