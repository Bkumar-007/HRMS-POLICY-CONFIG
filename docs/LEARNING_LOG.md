# Learning log

Short notes captured while building each phase (interview talking points).

## Phase 1 — Walking skeleton

- LLM chat calls need a **system prompt** (role/rules), a **user message**, and model options such as **temperature**.
- **Context window** is finite; every turn (system + history + tools) consumes tokens.
- JWT puts identity in the security context so later tools never trust the model for `employeeId`.

## Phase 2 — Tool calling

- The model does not execute code. It emits a **tool call** (name + JSON args). The framework runs the Java method and returns the result as another message; the model then writes the final answer.
- **`@Tool` / `ChatClient.defaultTools`** register schemas the model can choose from. Optional args use `@ToolParam(required = false)`.
- **`ToolContext`** carries server-only data (employeeId, conversationId, location). Those keys are **not** in the tool schema, so prompt injection cannot pick another employee.
- **Read tools** run immediately; writes will be gated behind pending actions in Phase 4.
- **Chat memory** (`MessageChatMemoryAdvisor` + JDBC repository) stores prior user/assistant turns keyed by `conversationId`. Ownership is checked in `chat_conversations` so one user cannot resume another’s thread.
- **`ToolCallLog`** records every tool invocation for debugging and interview answers on agent control.

## Phase 2.5 — Folder structure

- **Thin pages/controllers**: routes only mount screens or call application services — no fetch/SQL in the route layer.
- **Centralized API paths** (`utils/apiRoutes` + `routesMapping` / `Route()`) keep backend URLs in one place so features share the same contract.
- **`application/<feature>`** on the backend separates use-cases from HTTP (`web/`) and from persistence (`domain/` / `mockhr/`).

## API response envelope

- Every endpoint returns `{ code, data, message, count? }` — **code 1 = success**, **code 0 = failure**.
- Keeps the UI contract stable as new modules (policies, actions) are added; list endpoints can set `count`.

## Phase 3 — Uploadable policy library + RAG + citations

- **Embeddings** turn text into vectors; **pgvector** stores them so similar questions retrieve nearby chunks (cosine distance).
- **Ingest flow:** upload/seed → store raw file → chunk by headings/size → embed → write metadata (`policyId`, `policyName`, `section`, optional grade/location) → status `INDEXED`.
- **Delete/re-ingest** removes old chunks for that `policyId` first so the index never drifts from the library.
- **`searchHrPolicies`** is a tool (not a hidden prompt dump): the model must call it for policy Q&A; the server builds **citations** from retrieval metadata so the UI never trusts invented source names.
- Quality checks for retrieval stay **ad-hoc / live demos** (upload a policy, ask about it, confirm citation) — no checked-in question bank.
