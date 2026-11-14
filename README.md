# HRMS Policy Config — HR Policy Assistant + Action Agent

Portfolio project: **RAG over HR policies** plus an **action agent** with tool calling, RBAC, and user confirmation for write operations.

> Confidentiality: uses **mock HR APIs**, fictional company data (e.g. Acme Corp), and authored sample policies — no real employer code or data.

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot, Spring AI, Spring Security (JWT) |
| AI | Gemini (MVP chat + embeddings); Ollama optional later |
| Data | PostgreSQL + pgvector |
| Frontend | **Next.js** (JavaScript) + feature `actions/` |

## Repository layout

```text
backend/                 Spring Boot API
  src/main/java/.../
    config/              App config, seed data
    domain/              JPA entities + repositories
    security/            JWT, filters, SecurityConfig
    service/             Business logic
    web/                 Controllers + DTOs
frontend/                Next.js (JS)
  app/                   Routes (App Router): /login, /chat
  src/
    api/httpClient.js    Low-level fetch only
    actions/             Feature actions (auth, chat) — pages call these
    components/          UI components
docs/policies/           Sample HR policy markdown
docker-compose.yml       PostgreSQL + pgvector
PROJECT_PLAN.md          Full design + phases
```

## Prerequisites

- Java 21+, Maven
- Node.js 20+
- Docker Desktop running
- `GEMINI_API_KEY` set in your user environment

## Quick start

### 1. Database

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Health: http://localhost:8080/actuator/health

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: http://localhost:3000

### Demo logins

| User | Password | Role |
|------|----------|------|
| `priya.employee` | `password` | EMPLOYEE |
| `admin.hr` | `password` | HR_ADMIN |

## Phase 1 status

- [x] Monorepo `backend/` + `frontend/`
- [x] JWT login + RBAC roles
- [x] Next.js login + chat UI (actions layer, no direct API from pages)
- [x] Gemini chat (no tools/RAG yet)
- [ ] Phase 2: leave tools, holidays
- [ ] Phase 3: RAG + citations

## License

Personal / portfolio use.
