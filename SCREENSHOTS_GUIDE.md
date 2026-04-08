# Screenshot & Video Guide

## Screenshots to take (for README + LinkedIn)

Run the app locally, then capture these 6 screens:

---

### 1. Customer Dashboard (most important)
**URL:** localhost:3000 → logged in
**What to show:** Balance card, Total Sent/Received cards, Statement tab with entries
**Save as:** `docs/screenshots/customer-dashboard.png`

---

### 2. Send Money — OTP Gate
**URL:** localhost:3000 → Send money tab
**Steps:** Enter amount 1500000 paise (₹15,000), click Send → the amber OTP box appears
**What to show:** The orange "High-value transfer — OTP required" box with the 6-digit input
**Save as:** `docs/screenshots/customer-otp-gate.png`
> This is your most impressive screenshot — shows a real fintech pattern

---

### 3. Backoffice Accounts Table
**URL:** localhost:3001 → Accounts tab
**What to show:** The accounts table with Dep/Wdw/Ledger buttons, balance column, status badges
**Save as:** `docs/screenshots/backoffice-accounts.png`

---

### 4. Backoffice Ledger Drawer
**URL:** localhost:3001 → Accounts tab → click "Ledger" on any account
**What to show:** The bottom drawer with DEBIT/CREDIT entries, reference IDs, counterparty accounts
**Save as:** `docs/screenshots/backoffice-ledger.png`

---

### 5. Admin Analytics / Users
**URL:** localhost:3002 → login → Users or Analytics tab
**What to show:** Users table with roles, or analytics stats
**Save as:** `docs/screenshots/admin-users.png`

---

### 6. Architecture diagram (from the PPT)
Use slide 4 from PayLedger.pptx — open it, screenshot the Architecture slide
**Save as:** `docs/screenshots/architecture.png`

---

## How to take screenshots on Mac
- `Cmd + Shift + 4` → drag to select area → saves to Desktop
- Or `Cmd + Shift + 4 + Space` → click a window → captures just that window

---

## Video to record (for LinkedIn — 30–60 seconds)

### Use QuickTime → File → New Screen Recording

**Script for the video (do this in order, fast):**

```
1. Open localhost:3002 (Admin)
   → Login (admin / Admin@123)
   → Create CUSTOMER user (name: demo_user, phone: 9876543210)

2. Open localhost:3000 in another tab (Customer)
   → Enter phone 9876543210 → Send OTP
   → Enter 123456 → Register with username+password
   → Login

3. Back to localhost:3001 (Backoffice) in another tab
   → Login as your backoffice user
   → Open account for demo_user
   → Deposit ₹1,00,000 (enter 10000000)

4. Back to localhost:3000 (Customer)
   → Send money tab
   → Enter destination account number
   → Enter 1500000 paise (₹15,000)
   → Click Send → OTP gate fires (show the amber box)
   → Enter 123456 → Send again → Transfer complete ✅

5. Show the statement — DEBIT entry visible
```

**Total time: ~45 seconds**

---

## Create the docs/screenshots folder

```bash
mkdir -p docs/screenshots
# Then save your screenshots there
# Then git add docs/screenshots/
```

---

## Final GitHub README update

Once you have screenshots, replace the placeholder lines in README.md:

```markdown
| Customer Portal | Backoffice Portal | Admin Console |
|:-:|:-:|:-:|
| ![Customer](docs/screenshots/customer-dashboard.png) | ![Backoffice](docs/screenshots/backoffice-accounts.png) | ![Admin](docs/screenshots/admin-users.png) |
```

These already match the filenames above — just drop the PNGs in the right folder.
