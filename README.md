# HRMS Policy Config — HR Policy Assistant + Action Agent

Portfolio project: **RAG over HR policies** plus an **action agent** with tool calling, RBAC, and user confirmation for write operations.

> Confidentiality: uses **mock HR APIs**, fictional company data (e.g. Acme Corp), and authored sample policies — no real employer code or data.

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot, Spring AI, Spring Security (JWT) |
| AI | Gemini (MVP chat + embeddings); Ollama optional later |
| Data | PostgreSQL + pgvector, JPA |
| Frontend | Next.js (JavaScript) + feature `actions/` |
| Tests | JUnit 5, Mockito, Testcontainers |

## Architecture

```text
React Chat UI  →  Spring Boot API  →  RAG (pgvector) + Agent tools → Mock HR
                      │
                      ├── JWT / RBAC
                      ├── Pending actions (confirm / cancel)
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
    application/         Feature use-cases (auth, chat, leave, holidays; policies/actions stubs)
    agent/               @Tool methods
    config/              App config, AI, seed data
    domain/              JPA entities + repositories
    mockhr/              Fake HR services
    security/            JWT, filters, SecurityConfig
    web/                 Thin controllers + DTOs
frontend/                Next.js (JS) — modular folders
  app/                   Thin routes: /login, /chat
  src/component/         Screens by module (auth, chat, layout, common)
  src/configuration/     AppSetting (API base URL)
  store/                 Feature *Actions (auth, chat, policies, actions stubs)
  utils/api.js           HTTP client
  utils/apiRoutes/       routesMapping + Route() — all backend paths
docs/policies/           Sample HR policy markdown (seed)
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

### Phase 2 chat demos

- *"How many casual leaves do I have?"* → `getLeaveBalance`
- *"What are the holidays in November 2026?"* → `getHolidayList`
- Follow-up in the same thread uses server-side conversation memory

## Phase status

- [x] Phase 1: Monorepo, JWT login + RBAC, Next.js login/chat, Gemini chat
- [x] Phase 2: Mock HR, `getLeaveBalance` / `getHolidayList`, tool audit, JDBC chat memory
- [x] Phase 2.5: Folder structure alignment (backend `application/`, frontend `apiRoutes` / `store` / `component`)
- [ ] Phase 3: Uploadable policy library + RAG + citations (not leave/travel-only)
- [ ] Phase 4: Write tools + confirmation
- [ ] Phase 5: Tests + portfolio polish (rate limit, cache, timeouts, …)

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
