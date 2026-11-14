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
