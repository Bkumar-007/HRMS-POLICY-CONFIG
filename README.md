# HRMS Policy Config — HR Policy Assistant + Action Agent

Portfolio project: **RAG over HR policies** plus an **action agent** with tool calling, RBAC, and user confirmation for write operations.

> Confidentiality: uses **mock HR APIs**, fictional company data (e.g. Acme Corp), and authored sample policies — no real employer code or data.

## Features

| Capability | Description |
|------------|-------------|
| **RAG** | Answer questions on leave, travel, and other HR policies with **server-side citations** |
| **Agent tools** | `searchHrPolicies`, `getLeaveBalance`, `getHolidayList`, `applyLeave` (optional: `raiseHelpdeskTicket`) |
| **Confirmation** | Read tools run immediately; **write tools** create pending actions until the user confirms |
| **RBAC** | JWT + roles `EMPLOYEE` / `HR_ADMIN`; `employeeId` always from security context |
| **Eval** | Retrieval and agent evaluation harness (hit rate, tool-call accuracy) |

**Example:** *"Mere kitne leaves bache hain aur kal ka apply kar do"* → agent checks balance and policy, then proposes leave apply for confirmation.

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 3.4+, Spring AI, Spring Security |
| AI | Gemini (MVP chat + embeddings); Ollama optional later |
| Data | PostgreSQL + pgvector, JPA |
| Frontend | React 18, Vite, TypeScript |
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
backend/           Spring Boot API (Security, RAG, Agent, Actions)
frontend/          React (Vite) + TypeScript chat UI
docs/policies/     Sample HR policy markdown (10–15 files)
eval/              Retrieval + agent evaluation sets
docker-compose.yml PostgreSQL + pgvector
PROJECT_PLAN.md    Detailed plan and phases
```

## Prerequisites

- Java 21+
- Node.js 20+
- Docker (for PostgreSQL + pgvector)
- Gemini API key (for MVP)

## Quick start

> Scaffolding is in progress. Commands below are the target workflow once `backend/` and `frontend/` exist.

### 1. Database

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend
# set GEMINI_API_KEY (or provider key) in application.yml / env
./mvnw spring-boot:run
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

### 4. Demo logins (planned seed users)

| User | Role |
|------|------|
| `priya.employee` | EMPLOYEE |
| `admin.hr` | HR_ADMIN |

## API (target)

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/login` | Public | JWT |
| POST | `/api/chat` | EMPLOYEE+ | Chat + citations + pending actions |
| POST | `/api/actions/{id}/confirm` | EMPLOYEE+ | Execute pending write |
| POST | `/api/actions/{id}/cancel` | EMPLOYEE+ | Discard pending write |
| POST | `/api/admin/policies/ingest` | HR_ADMIN | Re-index policies |

## Demo script (target)

1. Login as employee  
2. Ask a leave-policy question → see answer + citations  
3. Ask for leave balance and apply for tomorrow  
4. Confirm the pending action in the UI  
5. (Optional) Login as HR admin and re-ingest policies  

## Evaluation

Eval sets under `eval/` measure retrieval quality and agent tool-call accuracy. Results and notes will be recorded here as the harness lands.

## What I learned

_Filled in as each phase ships — see learning log sections in [PROJECT_PLAN.md](./PROJECT_PLAN.md)._

## License

Personal / portfolio use.
