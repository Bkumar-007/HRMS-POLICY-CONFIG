# HRMS Policy Config — HR Policy Assistant + Action Agent

Portfolio project: **RAG over HR policies** plus an **action agent** with tool calling, RBAC, and user confirmation for write operations.

> Confidentiality: uses **mock HR APIs**, fictional company data (e.g. Acme Corp), and authored sample policies — no real employer code or data.

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot, Spring AI, Spring Security (JWT) |
| AI | Gemini (MVP chat + embeddings); Ollama optional later |
| Data | PostgreSQL + pgvector, JPA |
| Cache / limits | Caffeine, Bucket4j |
| Frontend | Next.js (JavaScript) + feature `actions/` |
| Tests | JUnit 5, Mockito, Testcontainers |

## Architecture

```text
React Chat UI  →  Spring Boot API  →  RAG (pgvector) + Agent tools → Mock HR
                      │
                      ├── JWT / RBAC
                      ├── Pending actions (confirm / cancel)
                      ├── Rate limit + Caffeine cache
                      └── Tool call audit log
```

Two behaviors in one chat:

1. **RAG** — retrieve policy chunks → answer + cite document/section  
2. **Agent** — model selects tools → reads execute now; writes wait for UI confirmation  

See [PROJECT_PLAN.md](./PROJECT_PLAN.md) for the full design.

## Repository layout

```text
backend/                 Spring Boot API
  src/main/java/.../
    application/         Feature use-cases (auth, chat, leave, holidays, policies, actions)
    agent/               @Tool methods + eval runner
    rag/                 Chunk, ingest, search, citations
    config/              App, AI, cache, rate limit, seed
    domain/              JPA entities + repositories
    mockhr/              Fake HR services
    security/            JWT, filters, SecurityConfig
    web/                 Thin controllers + DTOs
frontend/                Next.js (JS) — modular folders
  app/                   Thin routes: /login, /chat, /policies
  src/component/         Screens by module (auth, chat, policies, layout, common)
  src/configuration/     AppSetting (API base URL)
  store/                 Feature *Actions (auth, chat, policies, actions)
  utils/api.js           HTTP client
  utils/apiRoutes/       routesMapping + Route() — all backend paths
eval/                    Agent eval catalog (JSON)
docs/policies/           Sample HR policy markdown (seed source)
docs/*-ARCHITECTURE-GUIDE.md  Folder-structure references
docs/LEARNING_LOG.md     Per-phase notes
docker-compose.yml       PostgreSQL + pgvector
docs/DEPLOY.md           Azure free Postgres + Container Apps + Vercel + CI/CD
.github/workflows/       CI + API deploy to Azure
PROJECT_PLAN.md          Full design + phases
```

## Prerequisites

- Java 21+, Maven
- Node.js 20+
- Docker Desktop running
- Gemini API key in `backend/.env` (see below)

## Quick start

### 1. Database

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend
copy .env.example .env
# edit .env and set GEMINI_API_KEY=your-real-key
mvn spring-boot:run
```

`application.yml` reads `${GEMINI_API_KEY}` from the environment; `spring-dotenv` loads `backend/.env` automatically. Never commit `.env`.

Health: http://localhost:8080/actuator/health  
Metrics: http://localhost:8080/actuator/metrics (authenticated stack; local demo may use health/info only)

Uploaded policies are also mirrored under `backend/data/policies/{id}/` (markdown + `meta.json`) so you can open them on disk even if Docker/Postgres is stopped. Chat/RAG still needs the DB while the API is running.

#### Local chat with Ollama (when Gemini quota is hit)

```bash
# Install/start Ollama, then:
ollama pull llama3.2
cd backend
set SPRING_PROFILES_ACTIVE=ollama
mvn spring-boot:run
```

Azure live URL still uses Gemini — it cannot call Ollama on your laptop.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: http://localhost:3000

### Demo logins

| User | Password | Role | Grade / location |
|------|----------|------|------------------|
| `priya.employee` | `password` | EMPLOYEE | G2 / Pune |
| `rahul.employee` | `password` | EMPLOYEE | G3 / Mumbai |
| `admin.hr` | `password` | HR_ADMIN | G4 / Pune |

## Demo script (≈5 minutes)

1. Login as **admin.hr** → **Policies** → upload a named policy (e.g. “WFH Policy 2026”) → show INDEXED.
2. Login as **priya.employee** → ask a question answered only from that upload → show **citation**.
3. Ask *"How many casual leaves do I have?"* → `getLeaveBalance`.
4. Ask *"Apply casual leave for next Monday"* → pending card → **Confirm** → success (re-confirm is idempotent).
5. Try non-admin upload / confirm another user’s pending action → refused.
6. Show **eval** table below (or run heuristic runner).
7. One sentence each: uploadable RAG, tools, RBAC, confirmation, evaluation.

## Phase status

- [x] Phase 1: Monorepo, JWT login + RBAC, Next.js login/chat, Gemini chat
- [x] Phase 2: Mock HR, `getLeaveBalance` / `getHolidayList`, tool audit, JDBC chat memory
- [x] Phase 2.5: Folder structure alignment (backend `application/`, frontend `apiRoutes` / `store` / `component`)
- [x] Phase 3: Uploadable policy library + RAG + citations (not leave/travel-only)
- [x] Phase 4: Write tools + confirmation (`applyLeave`, `raiseHelpdeskTicket`) + agent eval
- [x] Phase 5: Tests + rate limit + cache + LLM timeout + README polish

## Evaluation

| Set | Size | Metric | Baseline result | Notes |
|-----|------|--------|-----------------|-------|
| Agent tools | 25 scenarios in [`eval/agent-eval.json`](./eval/agent-eval.json) | Heuristic expected-tool hit-rate | ≥85% (CI) | No Gemini burn; live LLM scoring optional |
| Leave rules | JUnit (`LeaveRulesTest`) | Pass/fail | Green | Balance, holidays, notice, blackout |
| Actions / security | Testcontainers ITs | Confirm idempotent; foreign deny; non-admin upload 403 | Green when Docker is up | Skipped if Docker unavailable |

Heuristic runner (prints markdown table):

```bash
cd backend
mvn -q spring-boot:run -Dspring-boot.run.profiles=eval,dev
```

## What I learned

- **Uploadable policies** keep RAG correct when HR content changes — no redeploy of prompts.
- **Tools** own systems of record (balances, leave writes); **RAG** owns handbook Q&A.
- **Confirmation** + re-validation + idempotency are the interview-ready guardrails for HR writes.
- **RBAC + JWT identity in ToolContext** means the model never picks `employeeId`.
- **Rate limits and timeouts** protect free-tier Gemini quota during demos.

## Deploy (Azure free tier + Vercel)

Environments use Spring profiles — not a shared “env-type” token. DB is **Azure PostgreSQL Flexible Server** (free SKU on Azure Free account): **one server**, databases `hr_dev` / `hr_uat` / `hr_prod`.

| Branch | `SPRING_PROFILES_ACTIVE` | DB | API | Frontend |
|--------|--------------------------|----|-----|----------|
| `develop` | `dev` | Azure PG `hr_dev` | Container App | Vercel Hobby |
| `uat` | `uat` | Azure PG `hr_uat` | Container App | Vercel Hobby |
| `main` | `prod` | Azure PG `hr_prod` | Container App | Vercel Hobby |

Each env gets its own `SPRING_DATASOURCE_*` (DB name differs), `JWT_SECRET`, `GEMINI_API_KEY`, and `APP_CORS_ALLOWED_ORIGINS`. Frontend uses `NEXT_PUBLIC_API_BASE_URL`.

Full free-tier steps: [docs/DEPLOY.md](./docs/DEPLOY.md).

## License

Personal / portfolio use.
