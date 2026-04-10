# PayLedger

> **Enterprise-grade payment infrastructure — built from scratch**

A full-stack payment management system simulating real-world banking operations across three dedicated role-based portals. Built with Spring Boot 4, React 18, and production-grade patterns including JWT auth, double-entry ledger, OTP verification, Kafka outbox, and optimistic locking.

---

## Links

**GitHub:** https://github.com/darshan3131/payment-ledger

## Local Demo

| Portal | URL | Credentials |
|--------|-----|-------------|
| 🧑 Customer | `localhost:3000` | Register via OTP |
| 🏦 Backoffice | `localhost:3001` | Created by Admin |
| 🔐 Admin | `localhost:3002` | `admin / Admin@1234` |

> **OTP in dev mode is always `123456`**

---

## Screenshots

<!-- Add your screenshots here after running the app -->

| Customer Portal | Backoffice Portal | Admin Console |
|:-:|:-:|:-:|
| ![Customer](docs/screenshots/customer-dashboard.png) | ![Backoffice](docs/screenshots/backoffice-accounts.png) | ![Admin](docs/screenshots/admin-users.png) |

---

## Architecture

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Customer    │  │  Backoffice  │  │    Admin     │
│  :3000       │  │  :3001       │  │  :3002       │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │  REST API (JWT + CORS)
                         ▼
              ┌──────────────────────┐
              │  Spring Boot  :8080  │
              │  SecurityConfig      │
              │  JwtFilter           │
              │  Controllers         │
              │  Services            │
              │  Repositories        │
              └───┬──────┬──────┬───┘
                  │      │      │
            ┌─────┘  ┌───┘  ┌──┘
            ▼        ▼      ▼
          MySQL    Redis   Kafka
          (JPA)   (OTP +  (Outbox
                  Idem.)   Events)
```

---

## Tech Stack

### Backend
| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4 |
| Auth | Spring Security + JWT (stateless, 24h) |
| ORM | JPA / Hibernate (`ddl-auto=update`) |
| Database | MySQL 8 |
| Cache / OTP | Redis (5-min TTL, key prefix namespacing) |
| Messaging | Apache Kafka (outbox pattern) |
| SMS | Twilio REST API |
| Build | Maven (mvnw wrapper) |

### Frontend
| Portal | Stack |
|--------|-------|
| Customer (`:3000`) | React 18, Vite, Axios interceptors |
| Backoffice (`:3001`) | React 18, Vite, Axios interceptors |
| Admin (`:3002`) | React 18, Vite, Axios interceptors |

### Infrastructure
| Tool | Purpose |
|------|---------|
| PM2 | Process manager for all 4 processes |
| Redis | OTP storage + idempotency key store |
| Kafka | Outbox event relay (SMS notifications) |
| BCrypt | Password hashing (cost factor 10) |

---

## Key Features

### Double-Entry Ledger
Every transfer creates exactly **2 ledger entries** — one DEBIT, one CREDIT. No money is ever created or destroyed; it only moves between accounts. A special `SYSTEM_CASH` float account is the source for all deposits and sink for all withdrawals.

```
Customer deposits ₹10,000:
  DEBIT  → SYSTEM_CASH        ₹10,000
  CREDIT → Customer Account   ₹10,000
```

### OTP-Gated High-Value Transfers
Transfers ≥ ₹10,000 return **HTTP 428 Precondition Required**. The client must:
1. `POST /api/v1/transactions/request-otp` → OTP stored in Redis (`txn-otp:` prefix, 5-min TTL)
2. Resubmit the transfer with the OTP field populated

```java
if (request.getAmount() >= highValueThreshold) {
    if (request.getOtp() == null || request.getOtp().isBlank()) {
        throw new IllegalArgumentException("OTP_REQUIRED:...");  // → HTTP 428
    }
    otpService.verifyAndConsume("txn-otp:" + accountNumber, request.getOtp());
}
```

### Idempotency Keys
Every transfer can include an `idempotencyKey`. Duplicate requests with the same key return the original response — preventing double-charges on network retries.

### Optimistic Locking
`@Version` on the `Account` entity prevents the **lost update** problem. If two concurrent requests read the same account (version=5) and both try to update, the second write detects version mismatch and throws `ObjectOptimisticLockingFailureException` → HTTP 409.

### Kafka Outbox Pattern
Transactions write to an `outbox_events` table **within the same DB transaction**. A scheduler polls the outbox and publishes to Kafka. A consumer reads from Kafka and sends SMS/webhook. This guarantees at-least-once delivery even if Kafka is down.

### Role-Based Access Control
| Role | Can Do |
|------|--------|
| `CUSTOMER` | Register, login, view own accounts, send money, raise tickets |
| `BACKOFFICE` | Open accounts, deposit/withdraw, reverse transactions, manage tickets |
| `ADMIN` | Create users, manage roles, view analytics, freeze/close accounts |

---

## API Reference

### Auth
```
POST   /api/v1/auth/register          Register (step 2 of OTP flow)
POST   /api/v1/auth/login             Login → JWT
POST   /api/v1/auth/send-otp          Send OTP to phone
POST   /api/v1/auth/verify-otp        Verify OTP
POST   /api/v1/auth/forgot-password   Send reset OTP
POST   /api/v1/auth/reset-password    Set new password
POST   /api/v1/auth/change-password   Change password (authenticated)
```

### Accounts
```
GET    /api/v1/accounts               Paginated list (BACKOFFICE/ADMIN)
POST   /api/v1/accounts               Create account
GET    /api/v1/accounts/my            My accounts (CUSTOMER)
GET    /api/v1/accounts/{id}          Get by ID
GET    /api/v1/accounts/number/{n}    Get by account number
PATCH  /api/v1/accounts/{id}/status   Update status (FROZEN, CLOSED, ACTIVE)
GET    /api/v1/accounts/{id}/ledger   Account statement
```

### Transactions
```
POST   /api/v1/transactions           Process transfer (CUSTOMER)
POST   /api/v1/transactions/request-otp  Request high-value OTP
POST   /api/v1/transactions/deposit   Deposit (BACKOFFICE/ADMIN)
POST   /api/v1/transactions/withdraw  Withdraw (BACKOFFICE/ADMIN)
POST   /api/v1/transactions/{ref}/reverse  Reverse (BACKOFFICE/ADMIN)
GET    /api/v1/transactions           Paginated list
GET    /api/v1/transactions/{ref}     Get by reference ID
GET    /api/v1/transactions/account/{id}  By account (paginated)
```

### Analytics (ADMIN)
```
GET    /api/v1/analytics              System-wide stats
```

---

## Local Setup

### Prerequisites
- Java 17+
- Node 18+
- MySQL 8 (running locally — create DB `payment_ledger_db`)
- Redis 7 (running locally on port 6379)
- Maven (or use `./mvnw`)

> Kafka is optional for local dev. The outbox poller will log warnings but all core features (auth, transfers, OTP, ledger) work without it.

### 1. Configure backend
Edit `src/main/resources/application.properties` with your local MySQL credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_ledger_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your-mysql-password

otp.dev-mode=true   # OTP hardcoded to 123456 — no SMS needed in dev
```

### 2. Build and run backend
```bash
./mvnw clean package -DskipTests
pm2 start "java -jar target/payment-ledger-0.0.1-SNAPSHOT.jar" --name spring-boot
```

### 3. Run frontend portals
```bash
cd frontend/customer   && npm install && pm2 start "npm run dev" --name customer
cd ../backoffice       && npm install && pm2 start "npm run dev" --name backoffice
cd ../admin            && npm install && pm2 start "npm run dev" --name admin
```

### 4. Verify all processes
```bash
pm2 list
# spring-boot  online
# customer     online
# backoffice   online
# admin        online
```

---

## End-to-End Test Flow

```
1. Admin (localhost:3002)
   └─ Create BACKOFFICE user (e.g. ops1 / Backoffice@123)
   └─ Create CUSTOMER user (e.g. cust1, phone: 9876543210)

2. Customer (localhost:3000)
   └─ Register cust1 (OTP: 123456) → set password → login

3. Backoffice (localhost:3001)
   └─ Login as ops1
   └─ Open account for cust1 → auto-generates ACC-XXXX
   └─ Deposit ₹1,00,000 (enter 10000000 paise)

4. Customer (localhost:3000)
   └─ Send ₹500 → completes instantly
   └─ Send ₹15,000 → OTP gate → enter 123456 → completes
   └─ View statement → see DEBIT entries

5. Backoffice (localhost:3001)
   └─ Reverse the ₹15,000 transaction
   └─ Freeze cust1's account

6. Customer (localhost:3000)
   └─ Try transfer → blocked (account frozen)
   └─ Raise support ticket

7. Backoffice (localhost:3001)
   └─ Resolve support ticket

8. Admin (localhost:3002)
   └─ Unfreeze account → set status ACTIVE
   └─ View analytics dashboard
```

---

## Project Structure

```
payment-ledger/
├── src/main/java/com/darshan/payment_ledger/
│   ├── config/          # Security, Kafka, Redis, SystemBootstrap
│   ├── controller/      # REST controllers (Account, Auth, Transaction, etc.)
│   ├── dto/             # Request/Response DTOs + PagedResponse
│   ├── entity/          # JPA entities (Account, User, Transaction, LedgerEntry…)
│   ├── enums/           # AccountStatus, Currency, Role, TransactionType…
│   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JwtFilter, JwtUtil, UserDetailsServiceImpl
│   └── service/         # Business logic (AccountService, TransactionService…)
├── frontend/
│   ├── customer/        # React app (Vite) — port 3000
│   ├── backoffice/      # React app (Vite) — port 3001
│   └── admin/           # React app (Vite) — port 3002
├── render.yaml          # Render deployment config (free tier)
├── DEPLOY_FREE.md       # Step-by-step free deployment guide (Render + Vercel)
└── pom.xml
```

---

## Design Decisions

**Why balances in paise (Long) not rupees (Double)?**
Floating-point arithmetic is non-deterministic for money. `0.1 + 0.2 = 0.30000000000000004` in Java. Long integer arithmetic is exact. All amounts are stored as the smallest currency unit (paise for INR, cents for USD) and formatted only at the display layer.

**Why Kafka outbox instead of direct SMS?**
Calling Twilio inside a DB transaction couples two external systems. If Twilio fails, should the transaction fail? No. The outbox pattern writes the event to the DB within the same transaction (atomically), then a separate process retries Kafka publish until it succeeds. SMS is eventual but guaranteed.

**Why optimistic locking instead of pessimistic?**
Pessimistic locking (DB row lock) serializes all transfers — terrible for throughput. Optimistic locking assumes no conflict (which is true for 99% of transfers between different accounts) and only fails on actual simultaneous writes to the same account. Lower latency, no deadlocks.

**Why three separate React apps instead of one?**
Role separation is enforced at the network level. A customer can't accidentally stumble onto the admin portal. In production each portal gets its own subdomain with separate CORS and auth policies. Reflects how real fintech companies separate internal tooling from customer-facing products.

---

## Author

**K C Darshan Siddarth**
Full Stack Developer · Bengaluru, India
📧 darshansiddarth05@gmail.com

---

*Built as a comprehensive portfolio project demonstrating enterprise Spring Boot patterns, double-entry accounting, and production-grade security.*
