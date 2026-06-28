# AI Book Study Companion — Operations, Security & Observability Specification
## Enterprise-Grade Analytics, Security Guardrails, Automated Testing, CI/CD, and Observability Pipelines

---

## 1. End-to-End Operational Architecture & Flow Design

This section outlines the operational architecture that ensures high reliability, prompt injection defense, automated verification, and performance tracing across millions of active study sessions.

### 1.1 Architectural Design (ASCII Diagram)

```text
[ Flutter Mobile App / Client Request ]
                    |
                    v
    +-------------------------------+
    |  Stage 1: Security Gateway    | <--- API Rate Limiter, Prompt Injection Guard
    +-------------------------------+
                    |
                    v
    +-------------------------------+
    |   Stage 2: Core Routing       | <--- Custom Cloud Load Balancer (WAF Enabled)
    +-------------------------------+
                    |
          /---------+---------\
         /          |          \
        v           v           v
  +-----------+ +-----------+ +-----------+
  | Firebase  | | Cloud Run | | GCS Storage|
  |   Auth    | |  Backend  | |   Asset   |
  +-----------+ +-----------+ +-----------+
        \           |           /
         \----------+----------/
                    |
                    v
    +-------------------------------+
    |  Stage 4: Real-time Analytics | <--- Pub/Sub telemetry stream to BigQuery
    +-------------------------------+
                    |
                    v
    +-------------------------------+
    | Stage 5: Cloud Observability  | <--- Tracing, Error Reporting & Cost Auditing
    +-------------------------------+
```

### 1.2 Telemetry and Auditing Loop

```text
Client App      API Gateway       Cloud Run       Pub/Sub Stream      BigQuery / Ops
    |                |                |                  |                  |
    |-- User Action->|                |                  |                  |
    |   (Quiz Match) |-- Sanitize --->|                  |                  |
    |                |   Prompt       |-- Write Log ---->|                  |
    |                |                |   Telemetry      |-- Batch Stream ->|
    |                |<-- Response ---|                  |                  |
    |                |   Executed     |                  |                  |
    |-- Render UI -->|                |                  |                  |
    |                |                |                  |-- Query Cost --->|
    |                |                |                  |   Dashboard      |
```

---

## 2. Analytics System & Behavior Logging

The analytics architecture tracks critical user journeys (CUJs), concept retention metrics, and AI service consumption, piping structural telemetry to **Google BigQuery** for long-term cohort analysis.

### 2.1 Event Taxonomy & Naming Standard
All event schemas follow a strict prefix structure: `[domain]_[entity]_[action]`.

| Domain | Event Name | Payload Description | Critical Business Metric |
| :--- | :--- | :--- | :--- |
| **Auth** | `auth_session_started` | Authentication method, device model, local IP. | Active Session Duration |
| **Ingestion** | `book_ingestion_completed`| Ingestion size, page count, processing duration. | Core Pipeline Performance|
| **Study** | `concept_mastery_updated` | Concept ID, Bayesian probability update, input. | Student Knowledge Growth |
| **Assessment**| `quiz_answer_submitted` | Question ID, selected option index, correctness. | Active Recall Accuracy |
| **AI** | `tutor_query_resolved` | Tokens spent, latency milliseconds, safety score. | API Cost & SLA Tracking  |

### 2.2 BigQuery Telemetry Schema

```json
{
  "table_name": "ai_tutor_sessions",
  "fields": [
    { "name": "event_timestamp", "type": "TIMESTAMP", "mode": "REQUIRED" },
    { "name": "user_id", "type": "STRING", "mode": "REQUIRED" },
    { "name": "session_id", "type": "STRING", "mode": "REQUIRED" },
    { "name": "concept_id", "type": "STRING", "mode": "NULLABLE" },
    { "name": "prompt_tokens", "type": "INTEGER", "mode": "REQUIRED" },
    { "name": "completion_tokens", "type": "INTEGER", "mode": "REQUIRED" },
    { "name": "api_latency_ms", "type": "INTEGER", "mode": "REQUIRED" },
    { "name": "prompt_injection_flagged", "type": "BOOLEAN", "mode": "REQUIRED" }
  ]
}
```

---

## 3. Information Security Model

The system enforces modern security standards to protect student data privacy, maintain backend separation, and defend against prompt manipulation exploits.

### 3.1 Prompt Injection Defensive Filters (AI Safety)
To mitigate prompt injection and output hijack attempts:
1. **Input Sanitization**: User-submitted chat messages are screened using structured regex rules to identify system instructions keywords (e.g., `"ignore previous instructions"`, `"system prompt"`, `"you are now an expert"`).
2. **Double-Pass Evaluation Pattern**: If high-risk keywords are detected, the request is routed through a lightweight checking model running on `gemini-3.5-flash` before processing. If flagged, the transaction is rejected immediately:

```json
{
  "type": "OBJECT",
  "properties": {
    "isPromptInjection": { "type": "BOOLEAN" },
    "confidenceScore": { "type": "NUMBER" }
  },
  "required": ["isPromptInjection", "confidenceScore"]
}
```

### 3.2 Firestore Defensive Rule Model
To prevent unauthorized data access, security rules enforce ownership and restrict cross-user lookups:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAuthenticated() {
      return request.auth != null;
    }
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    match /users/{userId}/{document=**} {
      allow read, write: if isOwner(userId);
    }
  }
}
```

---

## 4. Testing Framework & Quality Gates

Automated tests run continuously in CI/CD pipelines to verify application features, rendering engines, and AI state configurations.

### 4.1 Mocking AI Conversational Responses
To ensure deterministic testing, we mock downstream Gemini API endpoints. The testing harness replaces the active Client service with a static JSON mock interceptor:

```kotlin
class MockGeminiClient : GeminiClient {
    override suspend fun generateContent(prompt: String): String {
        return """
        {
          "conceptName": "Mocked Concept",
          "coreDefinition": "Determined by testing specifications.",
          "foundationalBreakdown": ["Step 1", "Step 2"],
          "caseStudy": {
            "title": "Mock Scenario",
            "scenario": "Action taken in testing scenario.",
            "solution": "Successfully verified."
          },
          "reflectionQuery": "Does the test pass?"
        }
        """.trimIndent()
    }
}
```

---

## 5. Production-Ready CI/CD Pipelines

All configuration changes, security rules, and code branches are automatically compiled and validated using **GitHub Actions**.

### 5.1 Environment Isolation & Canary Deployments

```text
       [ GitHub Push to main ]
                  |
                  v
       +-------------------------------+
       |     Stage 1: Lint & Build     | <--- Compiles Applet & Checks Syntax
       +-------------------------------+
                  |
                  v
       +-------------------------------+
       |    Stage 2: Run Unit Tests    | <--- Runs Local JVM & Mock Tests
       +-------------------------------+
                  |
                  v
       +-------------------------------+
       |    Stage 3: Canary Deploy     | <--- Deploys to 10% of Production Nodes
       +-------------------------------+
                  |
                  +-----------------------+-----------------------+
                  |                                               |
         [ Error Rate < 0.1% ]                           [ Error Rate >= 0.1% ]
                  |                                               |
                  v                                               v
       +-------------------------------+               +-------------------------------+
       |   Stage 4: Complete Rollout   |               |   Stage 4: Automated Rollback |
       +-------------------------------+               +-------------------------------+
```

---

## 6. Observability, Metrics & Alert Thresholds

The platform monitors operations using real-time dashboards and triggers alerts when performance drifts from baseline targets.

### 6.1 Critical Alert Matrix

| Incident Metric | Alert Severity | Trigger Window | Automated Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **API Latency** | High | $> 8.0\text{s}$ over 5 min | Temporarily route requests to secondary region servers or fall back to simplified summaries. |
| **Error Rate** | Critical | $> 1.0\%$ over 1 min | Pause deployment, redirect users to stable routes, and notify the SRE on-call engineer. |
| **Daily Token Cap**| Warning | $> 85\%$ of quota limit| Restrict complex tutoring requests for free-tier users to conserve token allocations. |
| **Malware Flag** | Critical | Instant | Isolate the affected user's GCS bucket directory and flag their account for administrative audit. |

---

## 7. Disaster Recovery & Production Readiness Checklist

- [x] **Database Point-In-Time Recovery (PITR)**: Firestore PITR is enabled, allowing databases to be restored to any microsecond coordinate within the last 7 days.
- [x] **Regional Failovers**: Multi-region database and storage buckets ensure high availability ($99.99\%$ SLA targets).
- [x] **Rate-Limiting Middleware**: IP and user token rate-limit configurations protect backend servers from unexpected query spikes.
- [x] **Automated Cost Monitors**: Daily budget thresholds trigger automated Slack and Email alerts if API spend exceeds daily limits.
