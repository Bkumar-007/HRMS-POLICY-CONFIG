# Frontend Architecture Guide

Reusable **folder / request-flow** reference for this portfolio app.  
Stack for *this* repo: Next.js (App Router) + JavaScript. Patterns below are structural — adapt naming to modules you add (`auth`, `chat`, `policies`, `actions`).

---

## 1. High-level folder structure

```
frontend/
├── app/                           # File-based UI routes (URL = folder path)
│   ├── login/page.js
│   ├── chat/page.js
│   ├── policies/page.js
│   ├── layout.js
│   └── page.js
├── src/
│   ├── component/                 # UI screens & shared components (by module)
│   │   ├── auth/
│   │   ├── chat/
│   │   ├── policies/
│   │   ├── common/
│   │   └── layout/
│   └── configuration/
│       └── AppSetting.js          # env → API base URL
├── store/                         # Feature actions (+ optional reducers later)
│   ├── auth/
│   │   └── authActions.js
│   ├── chat/
│   │   └── chatActions.js
│   ├── policies/
│   │   └── policiesActions.js
│   └── actions/
│       └── actionsActions.js
├── utils/
│   ├── api.js                     # HTTP client + auth header
│   ├── helpers.js                 # commonRequest(...)
│   └── apiRoutes/
│       ├── index.js               # routesMapping + Route()
│       ├── authRoutes.js
│       ├── chatRoutes.js
│       ├── policiesRoutes.js
│       └── actionsRoutes.js
├── public/
├── jsconfig.json                  # path aliases
└── package.json
```

### Path aliases (`jsconfig.json`)

```json
{
  "@component/*": ["src/component/*"],
  "@store/*": ["store/*"],
  "@utils/*": ["utils/*"],
  "@configuration/*": ["src/configuration/*"]
}
```

---

## 2. Mental model (request flow)

```
app/<module>/page.js               thin page: layout + mount screen component
        ↓
src/component/<module>/...         UI, local state, dispatch/call actions
        ↓
store/<module>/<module>Actions.js  async feature actions
        ↓
commonRequest(apiClient, url, body, method, params)
        ↓
utils/apiRoutes/<module>Routes.js  relative path fragment
        + Route() / ApiRootUrl
        ↓
Backend REST
```

**Pages stay thin.** Real UI and API calls live in `src/component` + `store`.

---

## 3. How page routes are defined

| File | URL |
|---|---|
| `app/chat/page.js` | `/chat` |
| `app/policies/page.js` | `/policies` |
| `app/login/page.js` | `/login` |

Typical page: import a screen from `@component/...` and render it with light layout only.

---

## 4. How API routes are defined

### Step A — relative endpoint constants

File: `utils/apiRoutes/policiesRoutes.js`

```js
export const List = "api/policies";
export const Get = "api/policies";           // + /{id} in action
export const Upload = "api/policies";
export const Delete = "api/policies";        // + /{id}
export const Ingest = "api/policies";        // + /{id}/ingest
```

### Step B — register in `routesMapping`

File: `utils/apiRoutes/index.js`

```js
import * as authRoutes from "./authRoutes";
import * as chatRoutes from "./chatRoutes";
import * as policiesRoutes from "./policiesRoutes";
import * as actionsRoutes from "./actionsRoutes";
import { AppSetting } from "@configuration/AppSetting";

export const ApiRootUrl = AppSetting.ApiServerURL;

export const routesMapping = {
  auth: authRoutes,
  chat: chatRoutes,
  policies: policiesRoutes,
  actions: actionsRoutes,
};

export const Route = (route) => `${ApiRootUrl}${route}`;
```

Usage:

```js
Route(`${routesMapping.chat.Send}`)
// → http://localhost:8080/api/chat
```

### Base URL (`AppSetting.js`)

`NEXT_PUBLIC_API_BASE_URL` (or similar) selects local / deployed API host.

---

## 5. Feature actions

```js
import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export const sendChat = (body) =>
  commonRequest(apiClient, Route(routesMapping.chat.Send), body, "POST", {});
```

Prefer **no hardcoded hosts** inside actions.

---

## 6. Components vs pages

| Layer | Responsibility |
|---|---|
| `app/...` | URL, light layout wrapper, import screen |
| `src/component/<module>/...` | Screens, forms, tables |
| `src/component/common/` | Shared UI (dialogs, loaders) |
| `store/<module>/` | API actions |
| `utils/apiRoutes/` | Endpoint path fragments |

---

## 7. Checklist: add a new feature module

1. Create `utils/apiRoutes/myFeatureRoutes.js`
2. Register in `routesMapping`
3. Add `store/myFeature/myFeatureActions.js`
4. Build UI under `src/component/myFeature/`
5. Add thin `app/myFeature/page.js`
6. Wire nav / menu if needed

---

## 8. Do / Don’t

**Do**

- Keep pages thin; put UI in `src/component`
- Centralize URLs in `utils/apiRoutes` + `routesMapping`
- Use one authenticated HTTP helper for API calls

**Don’t**

- Put fetch/Axios directly inside page files
- Hardcode environment hosts inside actions
