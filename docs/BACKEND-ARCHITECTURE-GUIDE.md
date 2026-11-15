# Backend Architecture Guide

Reusable **folder / request-flow** reference for this portfolio app.  
Stack for *this* repo: Java 21 + Spring Boot (not ASP.NET). Patterns below describe **structure** — thin HTTP layer + feature application layer + domain.

---

## 1. High-level folder structure

```
backend/
  src/main/java/com/acme/hrassistant/
    HrAssistantApplication.java
    web/                               # HTTP layer only
      AuthController.java
      MeController.java
      ChatController.java
      PolicyController.java
      ActionController.java
      dto/
      GlobalExceptionHandler.java
    application/                       # Business use-cases by feature
      auth/
      chat/
      policies/
        queries/
        commands/
      actions/
        commands/
      leave/
      holidays/
    agent/                             # Tool methods + orchestration helpers
    rag/                               # ingest, chunk, embed, search
    mockhr/                            # fake HR services
    domain/                            # JPA entities + repositories
    security/                          # JWT, filters, SecurityConfig
    config/                            # beans, seed, AI, cache, rate-limit
      cache/
      ratelimit/
  src/main/resources/
    application.yml
  pom.xml
  Dockerfile
```

**Rule of thumb**

| Layer | Put here | Do NOT put here |
|---|---|---|
| `web/` | Controllers, DTOs, exception handler | SQL, RAG, leave rules |
| `application/<feature>/` | Queries / commands / services | HTTP routing details |
| `domain/` | Entities, repositories | Controllers |
| `config/` | Cache, rate limit, AI, seed | Feature business rules |

---

## 2. How routes / controllers are defined

Prefer clear REST paths:

```
POST   /api/auth/login
GET    /api/me
POST   /api/chat
GET    /api/policies
POST   /api/policies
DELETE /api/policies/{id}
POST   /api/policies/{id}/ingest
POST   /api/actions/{id}/confirm
POST   /api/actions/{id}/cancel
```

### Controllers stay thin

Bind input → call application layer → return DTO. No SQL or embedding logic in controllers.

### Verb + binding conventions

| Intent | HTTP | Binding | Application type |
|---|---|---|---|
| Read / list / detail | `GET` | query / path | `*Query` or read service |
| Create / upload / complex write | `POST` | body / multipart | `*Command` |
| Update | `PUT` / `PATCH` | body | `*Command` |
| Delete | `DELETE` | path | `*Command` |

---

## 3. Application layer (business logic)

Organize by feature. Reads under `queries/`, writes under `commands/` (CQRS-style folders even without a mediator library).

```
application/policies/
  queries/ListPoliciesQuery.java      # or ListPoliciesService
  commands/UploadPolicyCommand.java
  commands/DeletePolicyCommand.java
  commands/IngestPolicyCommand.java
```

Handlers/services:

- Resolve identity from Spring Security (never from client-supplied employee id for tools).
- Call `domain` repositories, `rag`, or `mockhr` as needed.
- Return API DTOs / records.

---

## 4. Response & errors

All JSON APIs use a shared envelope (`ApiResponse`):

```json
{
  "code": 1,
  "data": { },
  "message": "Success",
  "count": null
}
```

| Field | Meaning |
|-------|---------|
| `code` | **1** = success, **0** = failure |
| `data` | Payload (object, list, or null on failure) |
| `message` | Human-readable success or error text |
| `count` | Optional list total (omit/`null` when not needed) |

- Controllers return `ApiResponse.ok(payload)` or `ApiResponse.ok(payload, count)`.
- `GlobalExceptionHandler` returns `ApiResponse.fail(message)` with an appropriate HTTP status (401 / 400 / 503 / 500…).
- Frontend `commonRequest` treats `code === 0` as failure and resolves to `data` on success.

---

## 5. Identity (who is calling)

Use Spring Security context / `EmployeePrincipal`:

- `employeeId`, `username`, `role`, `grade`, `location`
- Controllers and tools never trust LLM- or body-supplied identity for authorization

---

## 6. Data access

- JPA repositories under `domain/`
- Vector data via Spring AI `PgVectorStore`
- No raw connections scattered in controllers

---

## 7. Cross-cutting (see PROJECT_PLAN §8.1)

Wire in `config/` and the filter chain:

1. CORS
2. Rate limit
3. JWT auth
4. Validation
5. Controllers

Also: Spring Cache (Caffeine MVP), LLM timeouts, Actuator health/metrics.

---

## 8. Checklist: add a new feature

1. **Model / entity** → `domain/` if persisted
2. **Query or Command** → `application/<feature>/`
3. **Controller action** → `web/*Controller` → application call only
4. **Frontend** → matching `apiRoutes` + `store` module
5. Tests for rules / security as needed

---

## 9. Mental model (request flow)

```
HTTP Request
  → JWT / RBAC
  → Rate limit + validation
  → Thin Controller
  → application Query/Command
       → Security context identity
       → domain / rag / mockhr
  → DTO response
```

Keep controllers thin; keep handlers focused; keep platform concerns in `config/`.
