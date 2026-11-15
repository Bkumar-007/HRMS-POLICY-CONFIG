# Deploy: Azure free tier + Vercel

**Goal:** stay on **Azure free / free-account** resources only (no Neon).  
This project uses **one Spring profile per environment**. Each API deploy gets its **own** secrets — do not use a single shared token that “picks” a database at runtime.

> **Current recommended free path (after Flexible Server SKU issues):**  
> Postgres = **Azure Container Instances** (`pgvector/pgvector`) + API = **Container Apps** + images in **ACR**.  
> Script: [`scripts/deploy-api-dev.ps1`](../scripts/deploy-api-dev.ps1).

| Git branch | Spring profile (`SPRING_PROFILES_ACTIVE`) | Meaning |
|------------|-------------------------------------------|---------|
| `develop`  | `dev`                                     | Development |
| `uat`      | `uat`                                     | User acceptance |
| `main`     | `prod`                                    | Live / production |

```text
Vercel free (Next.js)  →  Azure Container Apps (API)  →  Azure PostgreSQL Flexible Server (free SKU)
     ↑                           ↑                              ↑
  NEXT_PUBLIC_             SPRING_PROFILES_ACTIVE         one server, 3 databases
  API_BASE_URL             + datasource secrets           hr_dev / hr_uat / hr_prod
```

### Free-tier rules (important)

| Resource | Free approach |
|----------|----------------|
| **Postgres** | Azure Free account → **Flexible Server** Burstable **B1ms**, ~32 GB, **12 months** free (then stop/delete or pay). **One server only**; three DBs on it. |
| **API** | Container Apps **consumption** plan (monthly free grant). Keep CPU/memory low (`0.25`–`0.5` vCPU, `0.5`–`1` Gi). Prefer **one** app while learning (`hr-api-dev`), add uat/prod only if grant allows. |
| **Images** | Prefer **GitHub Container Registry** (`ghcr.io`) — free for public repos — to avoid ACR Basic cost. ACR is OK only while Azure free credit lasts. |
| **Frontend** | Vercel **Hobby** (free). |
| **Local** | Keep [`docker-compose.yml`](../docker-compose.yml) — always free. |

After the 12‑month Flexible Server free period: **stop or delete** the server when not demoing, or accept paid SKU. There is no forever-free managed Postgres on Azure.

---

## 1. Environment variables

### API (Azure Container App / local Docker)

| Variable | Required | Purpose |
|----------|----------|---------|
| `SPRING_PROFILES_ACTIVE` | yes | `dev` / `uat` / `prod` |
| `SPRING_DATASOURCE_URL` | yes (hosted) | JDBC URL for **this** env’s database on Flexible Server |
| `SPRING_DATASOURCE_USERNAME` | yes (hosted) | Postgres admin/app user |
| `SPRING_DATASOURCE_PASSWORD` | yes (hosted) | Password |
| `JWT_SECRET` | yes (prod) | Per-env JWT signing secret (≥32 chars) |
| `GEMINI_API_KEY` | yes | Google Gemini API key |
| `APP_CORS_ALLOWED_ORIGINS` | yes (hosted) | Comma-separated Vercel origin(s) for this env |
| `SERVER_PORT` | no | Default `8080` |

Example JDBC URL (SSL required by Azure):

```text
jdbc:postgresql://hr-pg-free.postgres.database.azure.com:5432/hr_dev?sslmode=require
```

Local defaults live in [`backend/src/main/resources/application.yml`](../backend/src/main/resources/application.yml). Profiles: `application-dev.yml`, `application-uat.yml`, `application-prod.yml`.

### Frontend (Vercel / local)

| Variable | Purpose |
|----------|---------|
| `NEXT_PUBLIC_API_BASE_URL` | Base URL of the matching API (no trailing slash) |

Example: [`frontend/.env.local.example`](../frontend/.env.local.example).

---

## 2. Azure Database for PostgreSQL (free SKU)

Use **one** Flexible Server so you stay inside the free allowance; create **three databases** on it.

### 2.1 Create server (portal or CLI)

Prerequisites: [Azure free account](https://azure.microsoft.com/free/).

```bash
az group create -n rg-hr-assistant -l eastus

az postgres flexible-server create \
  --resource-group rg-hr-assistant \
  --name hr-pg-free \
  --location eastus \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 16 \
  --admin-user hr_admin \
  --admin-password 'REPLACE_WITH_STRONG_PASSWORD' \
  --public-access 0.0.0.0 \
  --yes
```

Notes:

- Pick a region that still offers the **free Flexible Server** benefit on your subscription.
- `Standard_B1ms` + 32 GB matches the typical free-account offer; do **not** pick a larger SKU.
- `--public-access 0.0.0.0` allows Azure services / your IP to connect for learning. Tighten firewall rules to your IP + Container Apps outbound later.

### 2.2 Databases + pgvector

```bash
az postgres flexible-server db create -g rg-hr-assistant -s hr-pg-free -d hr_dev
az postgres flexible-server db create -g rg-hr-assistant -s hr-pg-free -d hr_uat
az postgres flexible-server db create -g rg-hr-assistant -s hr-pg-free -d hr_prod
```

Connect (Azure Cloud Shell or `psql`) and enable vector (Phase 3 RAG):

```sql
\c hr_dev
CREATE EXTENSION IF NOT EXISTS vector;
\c hr_uat
CREATE EXTENSION IF NOT EXISTS vector;
\c hr_prod
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2.3 Map env → database

| Profile | Database | Example `SPRING_DATASOURCE_URL` |
|---------|----------|--------------------------------|
| `dev` | `hr_dev` | `jdbc:postgresql://hr-pg-free.postgres.database.azure.com:5432/hr_dev?sslmode=require` |
| `uat` | `hr_uat` | `.../hr_uat?sslmode=require` |
| `prod` | `hr_prod` | `.../hr_prod?sslmode=require` |

Same username/password for all three is fine on a learning server; still use **different** `JWT_SECRET` per API env.

Local laptop against Azure `hr_dev`:

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL="jdbc:postgresql://hr-pg-free.postgres.database.azure.com:5432/hr_dev?sslmode=require"
export SPRING_DATASOURCE_USERNAME="hr_admin"
export SPRING_DATASOURCE_PASSWORD="..."
cd backend && mvn spring-boot:run
```

---

## 3. Docker (API) — local smoke test

```bash
# Terminal 1: local Postgres (free)
docker compose up -d

# Terminal 2: build and run API container
cd backend
docker build -t hr-assistant-api:local .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/hr_assistant \
  -e SPRING_DATASOURCE_USERNAME=hr_user \
  -e SPRING_DATASOURCE_PASSWORD=hr_pass \
  -e GEMINI_API_KEY \
  -e JWT_SECRET=local-dev-secret-change-me-32chars-min \
  -e APP_CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  hr-assistant-api:local
```

Health: http://localhost:8080/actuator/health

---

## 4. Azure Container Apps (API) — free consumption

Start with **dev only**.

### 4.1 Resources (minimal cost)

1. Resource group `rg-hr-assistant`.
2. Container Apps Environment (consumption).
3. Container App `hr-api-dev` (add `hr-api-uat` / `hr-api-prod` later only if free grant covers them).
4. Image registry: **`ghcr.io`** (recommended on free) or ACR Basic while credit remains.
5. Service principal → `AZURE_CREDENTIALS` for GitHub Actions.

### 4.2 First Container App (manual sketch)

```bash
az containerapp env create -g rg-hr-assistant -n hr-cae --location eastus

az containerapp create \
  -g rg-hr-assistant -n hr-api-dev \
  --environment hr-cae \
  --image ghcr.io/YOUR_GITHUB_USER/hr-assistant-api:dev \
  --target-port 8080 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi
```

Then set secrets / env (`SPRING_PROFILES_ACTIVE=dev`, datasource → `hr_dev`, etc.). See [`.github/workflows/deploy-api.yml`](../.github/workflows/deploy-api.yml).

If you use ACR instead of GHCR, create it only while free credit lasts:

```bash
az acr create -g rg-hr-assistant -n YOURACR --sku Basic
```

### 4.3 GitHub Environments

Create Environments: `dev`, `uat`, `prod`.

**Secrets (per environment):** `AZURE_CREDENTIALS`, registry login (`ACR_*` or GHCR token), `SPRING_DATASOURCE_URL` (pointing at `hr_dev` / `hr_uat` / `hr_prod`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `GEMINI_API_KEY`, `APP_CORS_ALLOWED_ORIGINS`.

**Variables:** `AZURE_RESOURCE_GROUP`, `CONTAINER_APP_NAME`, `CONTAINER_APP_ENVIRONMENT`, optional `IMAGE_NAME`.

---

## 5. Vercel (frontend) — Hobby free

1. Import the GitHub repo; **Root Directory** = `frontend`.
2. Env mapping:
   - `develop` → `NEXT_PUBLIC_API_BASE_URL` = `hr-api-dev` HTTPS URL
   - `uat` → `hr-api-uat` (when created)
   - `main` → `hr-api-prod` (when created)
3. Redeploy after changing `NEXT_PUBLIC_*` (build-time).

Set each API’s `APP_CORS_ALLOWED_ORIGINS` to the matching Vercel origin.

---

## 6. CI/CD overview

| Workflow | Trigger | Action |
|----------|---------|--------|
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | PR + push to `main` / `develop` / `uat` | Maven `verify` + Next.js `build` |
| [`.github/workflows/deploy-api.yml`](../.github/workflows/deploy-api.yml) | Push (backend paths) or manual | Build/push image → sync secrets → deploy Container App |
| Vercel Git integration | Same branches | Build/deploy frontend |

---

## 7. Suggested rollout order

1. Spring profiles + env vars (done in repo).
2. Local Docker smoke test against Compose.
3. Azure Free account → **one** Flexible Server B1ms → DBs `hr_dev` / `hr_uat` / `hr_prod` + `vector` extension.
4. Point local or Container App **dev** at `hr_dev`; verify `/actuator/health`.
5. Enable GitHub Actions CI; deploy **dev** API only.
6. Vercel Hobby → wire `develop` frontend to Azure `dev` API.
7. Add uat/prod Container Apps only if free grant / credit allows (same Postgres server, different DB names).

---

## 8. Security & cost notes

- Never commit `.env`, DB passwords, or `JWT_SECRET`.
- Different `JWT_SECRET` per API environment.
- Prefer Container App **secret refs** for passwords and API keys.
- Stop the Flexible Server when idle to protect free hours / avoid surprise bills after month 12.
- Do not upgrade SKU, storage, or HA — that leaves the free offer.
- Keep `ddl-auto: update` only for early MVP; add Flyway/Liquibase before hardening prod.
