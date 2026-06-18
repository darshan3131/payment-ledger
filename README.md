<div align="center">

# 💳 PayLedger
### Enterprise-grade Payment Infrastructure — Built from Scratch

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev)
[![TiDB Cloud](https://img.shields.io/badge/TiDB%20Cloud-MySQL--Compatible-FF2D20?style=flat-square&logo=mysql&logoColor=white)](https://tidbcloud.com)
[![Redis](https://img.shields.io/badge/Redis-Upstash-DC382D?style=flat-square&logo=redis&logoColor=white)](https://upstash.com)
[![Render](https://img.shields.io/badge/API-Render-46E3B7?style=flat-square&logo=render&logoColor=black)](https://render.com)
[![Vercel](https://img.shields.io/badge/Frontend-Vercel-000000?style=flat-square&logo=vercel&logoColor=white)](https://vercel.com)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

A production-grade, multi-portal payment system with double-entry accounting, OTP-gated transfers, multi-currency FX, Kafka transactional outbox, and optimistic locking — built entirely from scratch.

**Open all three portals. Follow the steps below. You'll see the full system in under 5 minutes.**

</div>

---

## 🚀 Try It — Full Demo in 5 Minutes

> **Open these three tabs before you start.** The experience only makes sense when you see all three portals react to each other.

| Tab | Portal | URL | Login |
|-----|--------|-----|-------|
| Tab 1 | 🧑 Customer | [payment-ledger-6rqk.vercel.app](https://payment-ledger-6rqk.vercel.app) | You'll register here |
| Tab 2 | 🏦 Backoffice | [payment-ledger-5qx9.vercel.app](https://payment-ledger-5qx9.vercel.app) | `backoffice1 / Backoffice@1234` |
| Tab 3 | 🔐 Admin | [payment-ledger-bt73.vercel.app](https://payment-ledger-bt73.vercel.app) | `admin / Admin@1234` |

> ⚠️ **First load is slow** — backend is on Render free tier (cold start ~15s). Once it responds, everything is fast.

---

### Step 1 — Register as a Customer `[Tab 1: Customer]`

1. Click **"Create account"**
2. Enter any **username** and **phone number** (format: `+91XXXXXXXXXX`)
3. Click **Send OTP** → enter **`123456`** (dev mode — no real SMS)
4. Set your **password** and complete registration
5. **Login** with your new credentials
6. You'll see: *"No account linked — contact back office to create one"* ✅ That's correct. Move to Step 2.

---

### Step 2 — Open a Bank Account for Yourself `[Tab 2: Backoffice]`

1. Login as `backoffice1 / Backoffice@1234`
2. Go to **Accounts** (already open by default)
3. In the **"Open new account"** panel on the left:
   - Toggle **"New customers only"** if needed
   - Select your username from the dropdown
   - Pick a **Currency** — try **USD** to see multi-currency in action
   - Click **Open account**
4. Your account appears in the table with balance `$0.00` ✅

---

### Step 3 — Deposit Money `[Tab 2: Backoffice]`

1. Find your account row in the table
2. Click the **↓ Dep** (Deposit) button
3. Enter `100000` in the amount field — that's **$1,000.00** (amounts are in cents/subunits)
   - Watch the live preview: `= $1,000.00`
4. Click **Deposit** → balance updates to `$1,000.00` ✅
5. Click **Transactions** in the sidebar — your DEPOSIT appears instantly

---

### Step 4 — Send Money `[Tab 1: Customer]`

1. Switch back to your Customer tab and **refresh**
2. Your dashboard now shows `$1,000.00` balance 🎉
3. Click **Send money** (or Transfer)
4. Enter **any existing account number** as destination (e.g. `ACC9FD9B99E754A`)
5. Enter amount `2000` (= $20.00) → Submit → **COMPLETED instantly** ✅
6. Now try a high-value transfer: amount `1500000` (= $15,000)
   → **OTP gate fires** — enter `123456` → **COMPLETED** ✅

---

### Step 5 — Watch the Admin See Everything `[Tab 3: Admin]`

1. Switch to the Admin tab-Login as `admin / Admin@1234`
2. Go to **Analytics** — your deposits and transfers appear in the charts
   - Total Volume updated (FX-normalized to INR for cross-currency comparison)
   - Top Senders / Receivers table shows your account with `$` symbol
3. Go to **Users** — your newly registered account is in the list
4. Go to **System health** — all services green, Outbox pipeline: 0 stuck

---

### Bonus — Freeze, Reverse, Support `[Tab 2: Backoffice]`

- **Freeze an account**: Accounts → click a row → Update status → FROZEN → customer can no longer transfer
- **Reverse a transfer**: Transactions → click any COMPLETED transfer → **Reverse** → money flows back
- **Support tickets**: Create a ticket in the Customer portal → resolve it in Backoffice

---

## 🌟 Why This Project Is Different

Most portfolio projects stop at CRUD. PayLedger deliberately solves the hard problems that show up in real production systems:

| Problem | PayLedger's Solution |
|---------|---------------------|
| 💸 Double-charge on network retry | **Idempotency keys** in Redis — same request returns cached result, DB never runs twice |
| ⚡ Race condition on concurrent transfers | **`@Version` optimistic locking** — second writer detects stale version, returns HTTP 409 immediately, no row locks |
| 📡 SMS/notification failure inside a transaction | **Kafka transactional outbox** — event written to DB atomically with payment; Kafka relay is async and retried |
| 💱 Multi-currency arithmetic errors | **Integer subunits only** (paise, cents) — `0.1 + 0.2 ≠ 0.3` in IEEE 754; Long arithmetic is exact |
| 🔐 Admin lockout with no DB access | **`SystemBootstrap`** re-seeds credentials on every JVM start and restores `enabled` flag — self-healing |
| 🔒 CORS security hole | **3-origin whitelist** in SecurityConfig — only the three Vercel URLs allowed, no wildcard `*` |
| 🔄 Stuck PENDING transactions | **`ReconciliationService`** marks PENDING txns older than 5 min as FAILED — automatic crash recovery |

---

## 🏗️ Architecture

```
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  🧑 Customer  │   │  🏦 Backoffice│   │  🔐 Admin     │
│  Vercel       │   │  Vercel       │   │  Vercel       │
│  React + Vite │   │  React + Vite │   │  React + Vite │
└──────┬────────┘   └──────┬────────┘   └──────┬────────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │  HTTPS REST · JWT Bearer
                           ▼
            ┌──────────────────────────┐
            │   Spring Boot API        │  ← Render (free tier)
            │                          │
            │  SecurityConfig          │  ← 3-origin CORS whitelist
            │  JwtFilter               │  ← Stateless auth
            │  GlobalExceptionHandler  │  ← Consistent error shape
            │                          │
            │  TransactionService      │  ← Double-entry + FX convert
            │  AccountService          │  ← Optimistic locking (@Version)
            │  ReconciliationService   │  ← Stuck-txn cleanup
            │  OutboxPoller            │  ← Kafka relay (async)
            └────┬──────┬──────┬───────┘
                 │      │      │
           ┌─────▼──┐ ┌─▼───┐ ┌▼──────┐
           │TiDB    │ │Redis│ │Kafka  │
           │Cloud   │ │OTP +│ │Outbox │
           │(MySQL) │ │Idem.│ │Events │
           └────────┘ └─────┘ └───────┘
```

---

## 📸 Screenshots

## 📸 Screenshots

## 📸 Screenshots

<table>
  <tr>
    <td align="center"><b>🔐 Admin · Analytics Dashboard</b></td>
    <td align="center"><b>👥 Admin · User Management</b></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/b7f449b0-e822-4cdb-9d41-289a6f0d25f1" width="1440" height="900" alt="Admin Analytics" style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 8px;" /></td>
    <td><img src="https://github.com/user-attachments/assets/219aa53f-326b-4954-b06e-1af43a73f7de" width="1440" height="900" alt="Admin Users" style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 8px;" /></td>
  </tr>
  <tr>
    <td align="center"><b>🩺 Admin · System Health</b></td>
    <td align="center"><b>🏦 Backoffice · Accounts</b></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/1f8f62c5-dc0a-40fd-bc1f-49d74ad47ad0" width="1440" height="900" alt="System Health" style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 8px;" /></td>
    <td><img src="https://github.com/user-attachments/assets/07fe0390-e0c6-4c8d-b8af-fc00ab687255" width="1440" height="900" alt="Backoffice Accounts" style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 8px;" /></td>
  </tr>
  <tr>
    <td align="center"><b>💸 Backoffice · Transactions</b></td>
    <td align="center"><b>🧑 Customer · Login</b></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/8b5d62b0-006c-424f-bd3c-5161663dc2e7" width="1440" height="900" alt="Backoffice Transactions" style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 8px;" /></td>
    <td><img src="https://github.com/user-attachments/assets/a89ac53b-de02-4630-9341-785e671bfaa3" width="1440" height="900" alt="Customer Login" style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 8px;" /></td>
  </tr>
</table>

---

## ⚙️ Tech Stack

### Backend
| Layer | Technology | Why |
|-------|-----------|-----|
| Framework | Spring Boot 3 | Industry-standard Java API framework |
| Auth | Spring Security + JWT | Stateless, scalable, role-enforced |
| ORM | JPA / Hibernate + `@Version` | Optimistic locking without DB-level row locks |
| Database | TiDB Cloud (MySQL-compatible) | Horizontally scalable, generous free tier |
| Cache / OTP | Upstash Redis (TTL-based) | OTP expiry + idempotency key storage |
| Messaging | Apache Kafka | Transactional outbox event relay |
| SMS | Twilio REST API | OTP + transaction notifications |
| Migrations | Flyway | Versioned, auditable schema changes |
| Build | Maven | Standard Java build tool |

### Frontend (×3 separate portals)
| | Technology | Why |
|--|-----------|-----|
| UI | React 18 | Component model, hooks, fast reconciler |
| Bundler | Vite | Sub-second HMR, fast production builds |
| HTTP | Axios interceptors | Auto-attach JWT, redirect on 401 |

### Infrastructure
| | Hosting | Notes |
|--|---------|-------|
| API | Render (free) | Cold start ~15s; no cost for a portfolio project |
| 3 Frontends | Vercel (free) | Instant global CDN, per-portal deploys |

---

## ✅ Feature Checklist

**Security**
- [x] JWT stateless auth (24h expiry), BCrypt (cost 10)
- [x] Role-based access (`ADMIN` / `BACKOFFICE` / `CUSTOMER`) via `@PreAuthorize`
- [x] OTP-gated registration + high-value transfers (Redis, 5-min TTL)
- [x] CORS locked to 3 specific Vercel origins — no wildcard
- [x] Self-healing admin credentials (`SystemBootstrap` runs on every startup)

**Financial Correctness**
- [x] Double-entry bookkeeping — every payment = 1 DEBIT + 1 CREDIT
- [x] `SYSTEM_CASH` float account — sole entry and exit point for money in the system
- [x] All amounts stored as integer subunits — zero floating-point arithmetic
- [x] 10-currency FX support (INR, USD, EUR, GBP, JPY, AED, SGD, SAR, CAD, AUD)
- [x] Cross-currency reversal — debits destination in its own currency
- [x] Transaction reversal with mirror ledger entries

**Reliability**
- [x] Optimistic locking — prevents lost-update races without row locks
- [x] Idempotency keys — duplicate requests return cached result, no double-charge
- [x] Kafka transactional outbox — events survive Kafka downtime, at-least-once delivery
- [x] ReconciliationService — auto-fails stuck PENDING transactions every 5 min
- [x] JOIN FETCH on analytics — no N+1 query problem

**Product**
- [x] 3 role-separated portals (each knows nothing about the others)
- [x] Paginated transaction history with search and filter
- [x] Account ledger statement (full DEBIT/CREDIT audit trail)
- [x] Account status management (ACTIVE → FROZEN → CLOSED)
- [x] Support ticket system (raise → resolve)
- [x] SMS notifications on transfer, deposit, withdrawal, OTP (Twilio)
- [x] Analytics dashboard — FX-normalized volume, top accounts

---

## 💡 Key Engineering Decisions

**Why store money as `Long` (paise/cents), not `Double` (rupees/dollars)?**
`0.1 + 0.2 = 0.30000000000000004` in IEEE 754. Financial systems require exact arithmetic. Every amount in the DB is the smallest currency unit — paise for INR, cents for USD. Arithmetic stays in integer space: exact, deterministic, auditable at every step.

**Why Kafka outbox instead of calling Twilio directly?**
Putting an external API call inside a DB transaction couples two systems that shouldn't be coupled. If Twilio is slow, your payment is slow. If Twilio throws, does the payment roll back? The outbox writes the event to the DB atomically with the payment. A background poller delivers it asynchronously. SMS is eventual but guaranteed — payment speed is unaffected.

**Why optimistic locking instead of pessimistic row locks?**
Pessimistic locks serialize all writers to the same account — throughput dies under any concurrency. Optimistic locking assumes no conflict (true for 99% of transfers between *different* accounts) and only pays the cost when an actual race occurs. Lower latency, no deadlocks, automatic retry possible.

**Why three separate React apps instead of one with role-based routes?**
Role separation at the network level, not just the route level. The customer bundle has zero knowledge of admin endpoints — there's nothing to intercept or exploit. Each portal has its own Vercel deployment, CSP headers, and CORS policy. This mirrors how real fintech teams structure their internal tooling.

---

## 🗂️ Project Structure

```
payment-ledger/
├── src/main/java/com/darshan/payment_ledger/
│   ├── config/          # SecurityConfig, KafkaConfig, SystemBootstrap
│   ├── controller/      # REST controllers — Account, Auth, Transaction, Analytics…
│   ├── dto/             # Request/Response DTOs, AnalyticsResponse, PagedResponse
│   ├── entity/          # Account, User, Transaction, LedgerEntry, OutboxEvent
│   ├── enums/           # AccountStatus, Currency (10), Role, TransactionType
│   ├── exception/       # GlobalExceptionHandler + custom exceptions
│   ├── repository/      # Spring Data JPA repos + custom @Query methods
│   ├── security/        # JwtFilter, JwtUtil, UserDetailsServiceImpl
│   ├── service/         # AccountService, TransactionService, AnalyticsService…
│   └── util/            # CurrencyConverter — FX rates, subunit arithmetic
├── src/main/resources/
│   └── db/migration/    # Flyway: V1 baseline → V2 transactions → V3 ledger…
├── frontend/
│   ├── customer/        # React 18 + Vite  (port 3000)
│   ├── backoffice/      # React 18 + Vite  (port 3001)
│   └── admin/           # React 18 + Vite  (port 3002)
├── render.yaml          # Render free-tier deployment config
└── pom.xml
```

---

## 🏃 Local Setup

### Prerequisites
Java 17+, Maven, Node 18+, MySQL 8, Redis 7

### Backend
```bash
git clone https://github.com/darshan3131/payment-ledger.git
cd payment-ledger

# Edit src/main/resources/application.properties:
#   spring.datasource.url=jdbc:mysql://localhost:3306/payment_ledger_db?createDatabaseIfNotExist=true
#   spring.datasource.username=root
#   spring.datasource.password=YOUR_PASSWORD
#   otp.dev-mode=true   ← OTP is always 123456 in dev

./mvnw clean package -DskipTests
java -jar target/payment-ledger-0.0.1-SNAPSHOT.jar
```

### Frontend (3 terminals)
```bash
cd frontend/customer   && npm install && npm run dev   # http://localhost:3000
cd frontend/backoffice && npm install && npm run dev   # http://localhost:3001
cd frontend/admin      && npm install && npm run dev   # http://localhost:3002
```

---

## 🗺️ API Reference

<details>
<summary><b>Auth</b></summary>

```
POST  /api/v1/auth/send-otp          Send OTP to phone
POST  /api/v1/auth/verify-otp        Verify OTP
POST  /api/v1/auth/register          Complete registration
POST  /api/v1/auth/login             Login → JWT
POST  /api/v1/auth/forgot-password   Password reset OTP
POST  /api/v1/auth/reset-password    Set new password
POST  /api/v1/auth/change-password   Change password (authenticated)
```
</details>

<details>
<summary><b>Accounts</b></summary>

```
GET   /api/v1/accounts               All accounts (BACKOFFICE/ADMIN, paginated)
POST  /api/v1/accounts               Open new account
GET   /api/v1/accounts/my            My accounts (CUSTOMER)
GET   /api/v1/accounts/{id}          By ID
GET   /api/v1/accounts/number/{n}    By account number
PATCH /api/v1/accounts/{id}/status   Update status (FROZEN / CLOSED / ACTIVE)
GET   /api/v1/accounts/{id}/ledger   Account statement
```
</details>

<details>
<summary><b>Transactions</b></summary>

```
POST  /api/v1/transactions              Transfer (CUSTOMER)
POST  /api/v1/transactions/request-otp  Request high-value OTP
POST  /api/v1/transactions/deposit      Deposit (BACKOFFICE/ADMIN)
POST  /api/v1/transactions/withdraw     Withdraw (BACKOFFICE/ADMIN)
POST  /api/v1/transactions/{ref}/reverse  Reverse transfer (BACKOFFICE/ADMIN)
GET   /api/v1/transactions              All transactions (paginated)
GET   /api/v1/transactions/{ref}        By reference ID
GET   /api/v1/transactions/account/{id} By account (paginated)
```
</details>

<details>
<summary><b>Other</b></summary>

```
GET   /api/v1/analytics              System-wide stats (ADMIN)
GET   /api/v1/ledger/{accountId}     Ledger entries by account
POST  /api/v1/support                Raise support ticket (CUSTOMER)
GET   /api/v1/support                All tickets (BACKOFFICE/ADMIN)
PATCH /api/v1/support/{id}/resolve   Resolve ticket
GET   /api/v1/users                  All users (ADMIN)
POST  /api/v1/users                  Create user (ADMIN)
GET   /actuator/health               Service health (public)
```
</details>

---

## 👤 Author

**K C Darshan** · Full-Stack Developer · Bengaluru, India

[![GitHub](https://img.shields.io/badge/GitHub-darshan3131-181717?style=flat-square&logo=github)](https://github.com/darshan3131)
[![Email](https://img.shields.io/badge/Email-darshansiddarth05%40gmail.com-EA4335?style=flat-square&logo=gmail&logoColor=white)](mailto:darshansiddarth05@gmail.com)

---

<div align="center">
<i>Built to demonstrate that a junior engineer can ship production-grade architecture — not just CRUD.</i>

⭐ Star this repo if it taught you something.
</div>
