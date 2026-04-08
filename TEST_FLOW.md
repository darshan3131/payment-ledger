# PayLedger — Complete Test & Feature Flow

> OTP is hardcoded to **123456** in dev mode. Use this for all OTP prompts.
> Admin password: `admin` / `Admin@123`

---

## Portals

| Portal | URL | Who |
|---|---|---|
| Customer | http://localhost:3000 | End customers |
| Backoffice | http://localhost:3001 | Ops staff + Admin |
| Admin Console | http://localhost:3002 | Admins only |

---

## 1. Admin Portal — Create Staff & Customers

**URL:** http://localhost:3002 → Login: `admin` / `Admin@123`

### Create a Backoffice user
1. Users tab → **New user**
2. Username: `ops_staff`, Password: `Pass@123`, Role: `BACKOFFICE`, Phone: *(optional)*
3. Click **Create user** → appears in table

### Create a Customer (admin-side)
1. Users tab → **New user**
2. Username: `john_doe`, Password: `Pass@123`, Role: `CUSTOMER`, Phone: `+91XXXXXXXXXX`
3. Click **Create user**

> Customer now appears in Backoffice dropdown for account creation.

---

## 2. Customer Portal — Self-Registration (OTP Flow)

**URL:** http://localhost:3000

### Step 1 — Enter details
1. Click **Register**
2. Fill: Username, Password, Phone (`+91XXXXXXXXXX`)
3. Click **Send OTP**

### Step 2 — Verify OTP
1. Enter **123456** (dev mode hardcoded)
2. Click **Verify & create account**
3. Redirected to login

### Login
1. Enter username + password → **Sign in**
2. Dashboard loads — tabs: Statement | Send Money | Support | Security

---

## 3. Backoffice — Open Account for Customer

**URL:** http://localhost:3001 → Login with `ops_staff` / `Pass@123` (or admin)

1. Accounts tab → **Open new account**
2. Toggle "New customers only" ON → dropdown shows only customers without accounts
3. Select customer, choose currency (INR/USD/EUR)
4. Click **Open account** → account created with ₹0 balance

---

## 4. Backoffice — Deposit / Withdraw

In the **All accounts** table:
1. Find the customer's account row
2. Click **↓ Dep** → enter amount in paise (e.g. `500000` = ₹5,000) → **Deposit**
3. Click **↑ Wdw** → enter amount → **Withdraw**
4. Balance updates immediately in the table

---

## 5. Customer Portal — Send Money

**URL:** http://localhost:3000 → Login → **Send Money** tab

### Normal transfer (< ₹10,000)
1. Enter destination account number (e.g. `ACC10A3A419B9EF`)
2. Enter amount in paise (e.g. `100000` = ₹1,000)
3. Add description *(optional)*
4. Click **Send** → success, balance updated

### High-value transfer (≥ ₹10,000 = 1,000,000 paise)
1. Enter destination + amount `1000000`+
2. Click **Send** → OTP box appears (HTTP 428 intercepted)
3. Enter **123456** → Click **Verify & Send**
4. Transfer completes

---

## 6. Customer Portal — Statement

1. **Statement** tab → paginated transaction history
2. Shows: date, type, amount, counterparty, status

---

## 7. Backoffice — Reverse a Transaction

1. **Transactions** tab → find a COMPLETED TRANSFER row
2. Click **Reverse** → enter reason → confirm
3. Original transaction marked **REVERSED**, new REVERSAL entry created
4. Both sender and receiver balances corrected
5. SMS notification sent to both parties

---

## 8. Backoffice — Freeze / Close an Account

1. **Accounts** tab → click any row (auto-fills Account ID field)
2. Select status: **FROZEN** or **CLOSED**
3. Click **Update status**
4. Account frozen — deposit/withdraw buttons disable
5. SMS notification sent to account owner

---

## 9. Customer Portal — Support Tickets

1. **Support** tab → fill subject + message → **Submit ticket**
2. Ticket appears in history with status **OPEN**

### Backoffice resolves ticket
1. **Support** tab → find ticket → **Manage**
2. Change status to `IN_PROGRESS` or `RESOLVED`
3. Add resolution note → **Save**
4. Customer sees updated status + resolution in their portal

---

## 10. Customer Portal — Security (Change Password)

1. **Security** tab → enter current password + new password
2. Click **Change password** → success toast
3. Old password no longer works

---

## 11. Forgot Password Flow

1. Customer portal login page → **Forgot password**
2. Enter registered phone → OTP sent (enter **123456**)
3. Enter new password → **Reset**
4. Login with new password

---

## 12. Admin Portal — Account Status Management

1. Login to http://localhost:3002
2. **Account Management** tab → accounts table loads
3. Select customer from dropdown → **Open Account**
4. Enter Account ID → select FROZEN/CLOSED → **Update Status**

---

## 13. Ledger View (Double-Entry Audit)

1. Backoffice → Accounts → click **Ledger** on any row
2. Drawer opens showing all DEBIT / CREDIT entries
3. Every transfer creates exactly 2 entries (debit sender + credit receiver)

---

## 14. Analytics (Admin)

Admin portal → **Analytics** tab:
- Total accounts, total volume, transaction count
- Transaction status breakdown chart (COMPLETED / PENDING / FAILED)
- Account status breakdown chart
- Top senders + top receivers by volume

---

## System Health Check

Admin portal → **System Health**:
- MySQL ✅, Redis ✅, Spring Boot API ✅
- Outbox pending events count
- Reconciliation alerts

---

# Deployment Guide

## Local Prerequisites
- Java 17+
- Maven (or use `./mvnw`)
- MySQL 8.x
- Redis
- Node.js 18+
- PM2 (`npm install -g pm2`)

## Environment Setup

### 1. MySQL
```sql
CREATE DATABASE IF NOT EXISTS payment_ledger_db;
```

### 2. application.properties (key values)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_ledger_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=my-secret-pw
spring.jpa.hibernate.ddl-auto=update

jwt.secret=payment-ledger-super-secret-key-minimum-32-chars-long
jwt.expiration=86400000

otp.dev-mode=true          # Set false in production
twilio.account-sid=...
twilio.auth-token=...
twilio.from-number=+15108008591

payment.high-value-threshold=1000000   # ₹10,000 in paise
```

### 3. Build & Run (Local)
```bash
# Backend
./mvnw clean package -DskipTests
pm2 start "java -jar target/payment-ledger-0.0.1-SNAPSHOT.jar" --name spring-boot

# Frontends
pm2 start "npm run dev" --name customer   --cwd frontend/customer
pm2 start "npm run dev" --name backoffice --cwd frontend/backoffice
pm2 start "npm run dev" --name admin      --cwd frontend/admin

pm2 save
```

## Production Deployment (Ubuntu VPS)

### 1. Server prep
```bash
sudo apt update && sudo apt install -y openjdk-17-jdk mysql-server redis-server nodejs npm
npm install -g pm2
```

### 2. Build frontends for production
```bash
# In each frontend directory
cd frontend/customer && npm install && npm run build
cd frontend/backoffice && npm install && npm run build
cd frontend/admin && npm install && npm run build
```

### 3. Serve with Nginx

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # API
    location /api/ {
        proxy_pass http://localhost:8080;
    }

    # Customer portal
    location / {
        root /var/www/payment-ledger/customer/dist;
        try_files $uri $uri/ /index.html;
    }
}

server {
    listen 80;
    server_name backoffice.yourdomain.com;
    location / {
        root /var/www/payment-ledger/backoffice/dist;
        try_files $uri $uri/ /index.html;
    }
}

server {
    listen 80;
    server_name admin.yourdomain.com;
    location / {
        root /var/www/payment-ledger/admin/dist;
        try_files $uri $uri/ /index.html;
    }
}
```

### 4. Production flags
```properties
otp.dev-mode=false
spring.jpa.hibernate.ddl-auto=validate
```

### 5. SSL (Let's Encrypt)
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

---

## Quick Smoke Test After Deploy
```bash
# Backend health
curl http://localhost:8080/api/v1/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
# Expected: JWT token in response

# Redis
redis-cli ping  # Expected: PONG

# MySQL
mysql -u root -p -e "SHOW DATABASES;" | grep payment_ledger_db
```
