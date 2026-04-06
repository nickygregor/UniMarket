# UniMarket — CSE 3310 Spring 2026

**Team #12** | Soham Panchal · Jaskirat Singh · Neeve Shahi · Prisca Vanelle Djilo Djuidje

A full-stack campus marketplace Android application with a Kotlin/Ktor backend, Jetpack Compose Android app, and React web frontend.

---

## 🗂 Project Structure

```
unimarket/
├── backend/          ← Kotlin + Ktor REST API (SQLite via Exposed ORM)
├── android/          ← Android app (Kotlin + Jetpack Compose + MVVM)
└── frontend/         ← Web admin/buyer/seller UI (Vanilla HTML + JS)
```

---

## 🚀 Tech Stack (Resume-Worthy)

| Layer    | Tech                                              |
|----------|---------------------------------------------------|
| Backend  | **Kotlin**, **Ktor**, **Exposed ORM**, **SQLite**, **JWT**, **BCrypt** |
| Android  | **Kotlin**, **Jetpack Compose**, **MVVM**, **Retrofit**, **Room DB**, **DataStore** |
| Frontend | **HTML5/CSS3/JS**, **REST API integration**       |
| Auth     | **JWT (JSON Web Tokens)**, **BCrypt password hashing** |
| Arch     | **Clean Architecture**, **Repository Pattern**    |

---

## ⚙️ Backend Setup

### Prerequisites
- JDK 17+
- Gradle 8+

### Run
```bash
cd backend
./gradlew run
```

Server starts at **http://localhost:8080**

**Default admin credentials (auto-seeded):**
```
User ID:  admin
Password: admin123
```

### API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Register as Buyer or Seller |
| POST | `/auth/login`    | Public | Login, receive JWT |
| GET  | `/listings`      | Public | Browse listings (`?keyword=&category=`) |
| GET  | `/listings/{id}` | Public | Get listing detail |
| GET  | `/seller/listings`    | Seller JWT | Get my listings |
| POST | `/seller/listings`    | Seller JWT | Create listing |
| PUT  | `/seller/listings/{id}` | Seller JWT | Update listing |
| DELETE | `/seller/listings/{id}` | Seller JWT | Delete listing |
| GET  | `/buyer/cart`           | Buyer JWT | View cart |
| POST | `/buyer/cart/add`       | Buyer JWT | Add item to cart |
| DELETE | `/buyer/cart/remove/{id}` | Buyer JWT | Remove cart item |
| POST | `/buyer/orders/checkout`  | Buyer JWT | Mock checkout |
| GET  | `/buyer/orders`           | Buyer JWT | Order history |
| GET  | `/admin/users`            | Admin JWT | List all users |
| PUT  | `/admin/users/{id}/activate`   | Admin JWT | Activate user |
| PUT  | `/admin/users/{id}/deactivate` | Admin JWT | Deactivate user |
| DELETE | `/admin/listings/{id}`     | Admin JWT | Remove listing |

---

## 📱 Android App Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK API 33+
- Java 17

### Setup
1. Open `android/` folder in Android Studio
2. Sync Gradle
3. Start the backend (above) so the emulator can reach it
4. Run on an **Android Emulator** (the backend URL `10.0.2.2:8080` maps to your localhost)

> **Note:** If running on a physical device, update `BASE_URL` in `app/build.gradle.kts`  
> to your machine's local IP address (e.g., `http://192.168.1.100:8080/`)

### App Screens
- **Login / Register** — role-based (Buyer, Seller, Admin)
- **Buyer** — Browse listings grid, search, filter by category, add to cart, checkout, order history
- **Seller** — My listings dashboard, create/edit/delete listings
- **Admin** — User management (activate/deactivate), platform stats

---

## 🌐 Frontend Web UI Setup

No build step needed — pure HTML/JS.

1. Make sure the backend is running
2. Open `frontend/index.html` in a browser **or** serve it:
   ```bash
   # Python simple server
   cd frontend
   python3 -m http.server 3000
   # Then open http://localhost:3000
   ```

### Features
- Login / Register with role selection
- **Buyer:** Browse products, search + filter, cart management, checkout, order history
- **Seller:** Listings dashboard, create/edit/delete via modal, live preview
- **Admin:** User stats cards, user management with activate/deactivate toggles

---

## 🏛 Architecture

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                  │
│   Jetpack Compose Screens + ViewModels (MVVM)        │
└────────────────────┬────────────────────────────────┘
                     │ StateFlow / UiState
┌────────────────────▼────────────────────────────────┐
│                  DOMAIN LAYER                        │
│   Models (Kotlin data classes) + Repository interfaces│
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                  DATA LAYER                          │
│  ┌──────────────────┐   ┌─────────────────────────┐ │
│  │  Remote (Retrofit)│   │  Local (Room + DataStore)│ │
│  │  Ktor REST API    │   │  SQLite cache + JWT store│ │
│  └──────────────────┘   └─────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 🔐 Authentication Flow

1. User registers → password hashed with **BCrypt** → stored in SQLite
2. User logs in → credentials validated → **JWT token** returned
3. All protected requests include `Authorization: Bearer <token>`
4. Token stored in Android **DataStore** (encrypted preferences)
5. Backend validates JWT on every protected endpoint

---

## 👥 User Roles

| Role   | Capabilities |
|--------|-------------|
| Buyer  | Browse, search, cart, checkout, order history |
| Seller | All buyer views + create/edit/delete own listings |
| Admin  | Manage all users, deactivate accounts, remove listings |

---

## 📋 SRA Requirements Coverage

| Req # | Requirement | Status |
|-------|-------------|--------|
| 001 | User Registration | ✅ Implemented |
| 002 | User Login | ✅ Implemented |
| 003 | Role-Based Access Control | ✅ Implemented |
| 004 | Listing Management | ✅ Implemented |
| 005 | Search Functionality | ✅ Implemented |
| 006 | Cart Management | ✅ Implemented |
| 007 | Order Processing (Mock) | ✅ Implemented |
| 008 | Administrative Management | ✅ Implemented |
