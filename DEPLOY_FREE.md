# PayLedger — Free Deployment Guide (₹0)

No Railway credits needed. Genuine free tiers that don't expire.

| Service | Hosts | Free Tier |
|---------|-------|-----------|
| [Render](https://render.com) | Spring Boot API | 750 hrs/month |
| [PlanetScale](https://planetscale.com) | MySQL 8 | 5 GB, 1B row reads/month |
| [Upstash Redis](https://upstash.com) | Redis | 10,000 cmds/day, 256 MB |
| [Vercel](https://vercel.com) | 3 React portals | Unlimited |

**Total: ₹0/month**

> Render free tier spins down after 15 min idle — cold start ~30 sec. Pay $7/mo to keep always-on.

---

## Step 1 — PlanetScale (MySQL)

1. Sign up → https://planetscale.com (GitHub OAuth)
2. New database → name: `payledger` → region: AWS ap-south-1 (Mumbai)
3. Get connection string → select **Java / Spring**
4. Note down: `DB_URL` (replace `mysql://` → `jdbc:mysql://`), `DB_USER`, `DB_PASS`

---

## Step 2 — Upstash (Redis)

1. Sign up → https://upstash.com (GitHub OAuth)
2. New Redis → `payledger-redis` → ap-south-1
3. Note down: `REDIS_HOST`, `REDIS_PORT` (6379), `REDIS_PASSWORD`

---

## Step 3 — Render (Spring Boot backend)

1. Sign up → https://render.com (GitHub OAuth)
2. New → Web Service → connect your `payment-ledger` repo
3. Configure:
   - Runtime: **Java**
   - Build: `./mvnw clean package -DskipTests`
   - Start: `java -jar target/payment-ledger-0.0.1-SNAPSHOT.jar`
   - Plan: **Free**
4. Add env vars:
   ```
   DB_URL           jdbc:mysql://aws.connect.psdb.cloud/payledger?sslMode=VERIFY_IDENTITY&serverTimezone=UTC
   DB_USER          <PlanetScale>
   DB_PASS          <PlanetScale>
   REDIS_HOST       <Upstash>
   REDIS_PORT       6379
   REDIS_PASSWORD   <Upstash>
   JWT_SECRET       <openssl rand -base64 64>
   OTP_DEV_MODE     false
   ```
5. Deploy → API lives at `https://payledger-api.onrender.com`

**Update application.properties to read env vars:**
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.password=${REDIS_PASSWORD}
app.jwt.secret=${JWT_SECRET}
otp.dev-mode=${OTP_DEV_MODE:false}
```

---

## Step 4 — Vercel (3 React portals)

Repeat for each frontend:

| Portal | Root Directory |
|--------|----------------|
| Customer | `frontend/customer` |
| Backoffice | `frontend/backoffice` |
| Admin | `frontend/admin` |

Framework: **Vite** · Env var: `VITE_API_URL = https://payledger-api.onrender.com`

Your live URLs:
```
https://payledger-customer.vercel.app
https://payledger-backoffice.vercel.app
https://payledger-admin.vercel.app
```

---

## Step 5 — Smoke Test

```bash
curl https://payledger-api.onrender.com/actuator/health
# → {"status":"UP"}
```

1. Admin portal → create CUSTOMER user
2. Backoffice → open account for that user
3. Customer portal → send ≥ ₹10,000 → OTP gate fires
4. Check Render logs for OTP → submit → transfer completes

---

## Kafka (optional)

Core features work without Kafka. To enable:
- Use **Upstash Kafka** (free: 10 GB/month)
- Add `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_API_KEY`, `KAFKA_API_SECRET` on Render

---

## Local Dev

Requires MySQL 8 and Redis 7 running locally.

```bash
./mvnw spring-boot:run
cd frontend/customer   && npm run dev   # :3000
cd frontend/backoffice && npm run dev   # :3001
cd frontend/admin      && npm run dev   # :3002
```
