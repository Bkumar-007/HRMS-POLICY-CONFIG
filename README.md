# Recoil HR — Policy Assistant + Action Agent

Portfolio project: an employee HR portal with **RAG over HR policies** and an **action agent** that calls tools, enforces RBAC, and asks the user to confirm writes.

> Confidentiality: uses **mock HR APIs**, fictional company data (Acme Corp), and authored sample policies — no real employer code or data.

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 3.4, Spring AI, Spring Security (JWT) |
| AI | Gemini chat + embeddings (`gemini-3.5-flash-lite`, `gemini-embedding-001`); Ollama optional locally |
| Data | PostgreSQL + pgvector, JPA |
| Auth extras | Password reset OTP via email (SMTP), Fast2SMS, or Twilio |
| Cache / limits | Caffeine, Bucket4j |
| Frontend | Next.js 14 (JavaScript) — Recoil HR portal |
| Tests | JUnit 5, Mockito, Testcontainers |

## Architecture

```text
Recoil HR portal  →  Spring Boot API  →  RAG (pgvector) + Agent tools → Mock HR
                         │
                         ├── JWT / RBAC (EMPLOYEE, MANAGER, HR_ADMIN)
                         ├── Pending actions (confirm / cancel)
                         ├── Leave approvals, people, breaches, helpdesk inbox
                         ├── Password-reset OTP (email or SMS)
                         ├── Rate limit + Caffeine cache
                         └── Tool call audit log
```

Two behaviors in one chat:

1. **RAG** — retrieve policy chunks → answer + cite document/section
2. **Agent** — model selects tools → reads execute now; writes wait for UI confirmation

Identity (`employeeId`, grade, location) comes from the JWT tool context. The model never chooses whose data to read or write.

See [PROJECT_PLAN.md](./PROJECT_PLAN.md) for the full design.

## Portal

| Screen | Path | Who | What |
|--------|------|-----|------|
| Assistant | `/chat` | Everyone | Policy Q&A with citations; leave and helpdesk actions that need Confirm |
| Leave | `/leave` | Everyone | Own requests; managers and HR approve, reject, or cancel |
| Policies | `/policies` | Everyone browses; HR uploads | Library of indexed policies (md, txt, html, rtf, pdf, doc, docx, odt) |
| People | `/people` | Manager, HR | Directory and team; HR assigns roles and managers |
| Breaches | `/breaches` | Manager, HR | Log and update policy-violation status |
| Helpdesk | `/helpdesk` | Manager, HR | Inbox of tickets opened after chat confirmation |
| Profile | `/profile` | Everyone | Own contact details |
| Login | `/login` | Public | Sign in, or reset password with email/SMS OTP |

Agent tools (`HrTools`):

| Tool | Effect |
|------|--------|
| `getLeaveBalance` | Read the signed-in employee's balances |
| `getHolidayList` | Company holidays, optional month, location-aware |
| `searchHrPolicies` | Vector search with grade/location filters; citations |
| `applyLeave` | Validate rules, then stage a pending action |
| `raiseHelpdeskTicket` | Stage a ticket; it opens only after Confirm |

## Repository layout

```text
backend/                 Spring Boot API
  src/main/java/.../
    application/         Use-cases (auth, chat, leave, holidays, policies, actions, people, breaches)
    agent/               @Tool methods + eval runner
    rag/                 Chunk, ingest, search, citations
    config/              App, AI, cache, rate limit, seed
    domain/              JPA entities + repositories
    mockhr/              Fake HR services
    security/            JWT, filters, SecurityConfig
    web/                 Thin controllers + DTOs
frontend/                Next.js (JS)
  app/                   Routes: /login, /chat, /leave, /policies, /people, /breaches, /helpdesk, /profile
  src/component/         Screens by module (auth, chat, leave, policies, people, breaches, helpdesk, profile, layout)
  src/configuration/     AppSetting (API base URL) + roleAccess
  store/                 Feature *Actions
  utils/api.js           HTTP client
  utils/apiRoutes/       routesMapping + Route() — all backend paths
eval/                    Agent eval catalog (JSON)
docs/policies/           Sample HR policy markdown (seed source)
docs/*-ARCHITECTURE-GUIDE.md
docs/DEPLOY.md           Azure free Postgres + Container Apps + Vercel + CI/CD
docs/LEARNING_LOG.md     Per-phase notes
docker-compose.yml       PostgreSQL + pgvector
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

Uploaded policies are mirrored under `backend/data/policies/{id}/` (extracted text + `meta.json`) so you can open them on disk even if Docker/Postgres is stopped. Chat and RAG still need the database while the API is running.

On an empty policy library, seeded markdown from `docs/policies/` is ingested automatically (`APP_RAG_SEED_ON_EMPTY=true`).

#### Local chat with Ollama (when Gemini quota is hit)

```bash
# Install/start Ollama, then:
ollama pull llama3.2
cd backend
set SPRING_PROFILES_ACTIVE=ollama
mvn spring-boot:run
```

Azure live URL still uses Gemini — it cannot call Ollama on your laptop.

#### Password reset OTP

With no SMS or SMTP credentials, `APP_OTP_DEV_FALLBACK=true` accepts the fixed demo code **123456**.

To send a real OTP, set one provider in `backend/.env` and restart:

- **Fast2SMS** (India): `FAST2SMS_API_KEY`
- **Twilio**: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`
- **Email**: `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM`

Set `APP_OTP_DEV_FALLBACK=false` once a provider is configured. Demo contacts for `priya.employee` and `admin.hr` are `priya.sharma@acme.example` / `9876543210` (fictional). Other seeded users share `demo.employee@acme.example` / `9000000000`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: http://localhost:3000

### Demo logins

Password for every account: `password`

| User | Role | Grade / location | Reports to |
|------|------|------------------|------------|
| `priya.employee` | EMPLOYEE | G2 / Pune | Neha |
| `amit.employee` | EMPLOYEE | G2 / Pune | Neha |
| `rahul.employee` | EMPLOYEE | G3 / Mumbai | Vikram |
| `sara.employee` | EMPLOYEE | G3 / Mumbai | Vikram |
| `neha.manager` | MANAGER | G4 / Pune | — |
| `vikram.manager` | MANAGER | G4 / Mumbai | — |
| `admin.hr` | HR_ADMIN | G4 / Pune | — |

Priya's seeded balances: casual 8, sick 10, earned 12. Rahul: casual 5, sick 7, earned 15. Everyone else: casual 12, sick 10, earned 15.

## Demo script (≈5 minutes)

1. Login as **admin.hr** → **Policies** → upload a named policy (for example “WFH Policy 2026”) → show INDEXED.
2. Login as **priya.employee** → ask a question answered only from that upload → show the **citation**.
3. Ask *"How many casual leaves do I have?"* → `getLeaveBalance`.
4. Ask *"Apply casual leave for next Monday"* → pending card → **Confirm** → success (confirming again is idempotent).
5. Login as **neha.manager** → **Leave** → approve that request. Open **People** and **Helpdesk** to show the manager inbox.
6. Try a non-admin upload, or confirm another user’s pending action → refused.
7. On the login screen, **Forgot password** for `priya.employee` → OTP **123456** (dev fallback) → set a new password.
8. One sentence each: uploadable RAG, tools, RBAC, confirmation, evaluation.

## Phase status

- [x] Phase 1: Monorepo, JWT login + RBAC, Next.js login/chat, Gemini chat
- [x] Phase 2: Mock HR, `getLeaveBalance` / `getHolidayList`, tool audit, JDBC chat memory
- [x] Phase 2.5: Folder structure alignment (backend `application/`, frontend `apiRoutes` / `store` / `component`)
- [x] Phase 3: Uploadable policy library + RAG + citations
- [x] Phase 4: Write tools + confirmation (`applyLeave`, `raiseHelpdeskTicket`) + agent eval
- [x] Phase 5: Tests + rate limit + cache + LLM timeout
- [x] Portal: leave approvals, people directory, policy breaches, helpdesk inbox, profile, password-reset OTP

## Evaluation

| Set | Size | Metric | Baseline result | Notes |
|-----|------|--------|-----------------|-------|
| Agent tools | 25 scenarios in [`eval/agent-eval.json`](./eval/agent-eval.json) | Heuristic expected-tool hit-rate | ≥85% (CI) | No Gemini burn; live LLM scoring optional |
| Leave rules | JUnit (`LeaveRulesTest`) | Pass/fail | Green | Balance, holidays, notice, blackout |
| Actions / security | Testcontainers ITs | Confirm idempotent; foreign deny; non-admin upload 403 | Green when Docker is up | Skipped if Docker unavailable |

Heuristic runner (prints a markdown table):

```bash
cd backend
mvn -q spring-boot:run -Dspring-boot.run.profiles=eval,dev
```

## What I learned

- **Uploadable policies** keep RAG correct when HR content changes — no redeploy of prompts.
- **Tools** own systems of record (balances, leave writes); **RAG** owns handbook Q&A.
- **Confirmation** + re-validation + idempotency are the guardrails for HR writes.
- **RBAC + JWT identity in ToolContext** means the model never picks `employeeId`.
- **Rate limits and timeouts** protect free-tier Gemini quota during demos.
- **OTP fallback** lets the reset flow be demoed without burning SMS or SMTP credits.

## Deploy (Azure free tier + Vercel)

Environments use Spring profiles. DB is **Azure PostgreSQL Flexible Server** (free SKU on an Azure Free account): **one server**, databases `hr_dev` / `hr_uat` / `hr_prod`.

| Branch | `SPRING_PROFILES_ACTIVE` | DB | API | Frontend |
|--------|--------------------------|----|-----|----------|
| `develop` | `dev` | Azure PG `hr_dev` | Container App | Vercel Hobby |
| `uat` | `uat` | Azure PG `hr_uat` | Container App | Vercel Hobby |
| `main` | `prod` | Azure PG `hr_prod` | Container App | Vercel Hobby |

Each env gets its own `SPRING_DATASOURCE_*` (DB name differs), `JWT_SECRET`, `GEMINI_API_KEY`, and `APP_CORS_ALLOWED_ORIGINS`. Frontend uses `NEXT_PUBLIC_API_BASE_URL`.

Full free-tier steps: [docs/DEPLOY.md](./docs/DEPLOY.md).

## License

Personal / portfolio use.
