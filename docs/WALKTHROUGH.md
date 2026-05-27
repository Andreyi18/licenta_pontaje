# UPT Pontaje - Walkthrough

## Rezumat

Acest document descrie implementarea aplicației **UPT Pontaje**, un sistem de gestionare a pontajelor și anexelor salariale pentru cadrele didactice ale Universității Politehnica Timișoara.

---

## Structura Proiectului

```
licenta_iova/
├── backend/                   # Spring Boot 3.4.2
│   ├── src/main/java/ro/upt/pontaje/
│   │   ├── config/           # Security, JWT, CORS
│   │   ├── controller/       # REST API endpoints
│   │   ├── entity/           # JPA entities
│   │   ├── repository/       # Spring Data JPA
│   │   ├── service/          # Business logic
│   │   └── dto/              # Data transfer objects
│   └── pom.xml
├── frontend/                  # React + TypeScript + Vite
│   ├── src/
│   │   ├── api/              # API client functions
│   │   ├── components/       # Reusable components
│   │   ├── context/          # Auth context
│   │   ├── pages/            # Page components
│   │   ├── theme/            # MUI theme
│   │   └── types/            # TypeScript types
│   ├── Dockerfile
│   └── nginx.conf
├── docs/                      # Documentație proiect
│   ├── IMPLEMENTATION_PLAN.md
│   ├── TASK_PROGRESS.md
│   └── WALKTHROUGH.md
└── docker-compose.yml         # PostgreSQL + Backend + Frontend
```

---

## Backend (Spring Boot 3.4.2)

### Tehnologii Folosite

- Java 17
- Spring Boot 3.4.2
- Spring Data JPA
- Spring Security
- PostgreSQL
- JWT (jjwt 0.12.6)
- Apache PDFBox (PDF generation)
- Lombok

### Entități JPA

| Entitate       | Descriere               | Fișier                       |
| -------------- | ----------------------- | ---------------------------- |
| User           | Utilizator cu roluri    | `entity/User.java`           |
| Department     | Departament universitar | `entity/Department.java`     |
| Schedule       | Orar săptămânal         | `entity/Schedule.java`       |
| Timesheet      | Pontaj lunar            | `entity/Timesheet.java`      |
| TimesheetEntry | Intrări în pontaj       | `entity/TimesheetEntry.java` |
| Document       | Documente PDF generate  | `entity/Document.java`       |

### Securitate

- **JWT Authentication**: Token-based auth cu expirare configurabilă
- **Role-based Access**: CADRU_DIDACTIC, SECRETARIAT, ADMIN
- **Password Encoding**: BCrypt

### API Endpoints Principale

```
POST   /api/auth/login          # Autentificare
POST   /api/auth/register       # Înregistrare
GET    /api/auth/me             # Profil curent
PUT    /api/auth/profile        # Actualizare profil (nume, email, parolă)

GET    /api/schedules           # Lista orar utilizator
POST   /api/schedules           # Creare intrare orar
PUT    /api/schedules/{id}      # Editare intrare
DELETE /api/schedules/{id}      # Ștergere intrare

GET    /api/timesheets          # Lista pontaje
GET    /api/timesheets/current  # Pontaj luna curentă
POST   /api/timesheets/{id}/entries  # Adaugă ore
POST   /api/timesheets/{id}/submit   # Trimite pentru aprobare

POST   /api/documents/generate  # Generare PDF Anexe
GET    /api/documents/{id}/download  # Descărcare PDF
```

---

## Frontend (React + TypeScript + Vite)

### Tehnologii Folosite

- React 18
- TypeScript 5
- Vite 7
- Material-UI (MUI) v7
- React Router v6
- TanStack Query
- Axios

### Design System UPT

| Element       | Valoare            |
| ------------- | ------------------ |
| Primary Color | #003366 (UPT Blue) |
| Accent Color  | #0066cc            |
| CTA Color     | #F97316 (Orange)   |
| Font          | Inter              |

### Pagini Implementate

| Pagină            | Funcționalitate                                                              |
| ----------------- | ---------------------------------------------------------------------------- |
| **LoginPage**     | Autentificare cu branding UPT, validări                                      |
| **DashboardPage** | Overview cu statistici reale (ore, activități, documente), acțiuni rapide    |
| **SchedulePage**  | Grid săptămânal cu CRUD pentru activități                                    |
| **TimesheetPage** | Calendar lunar cu marcare ore și progres în timp real                        |
| **ProfilePage**   | Management cont: editare date personale, schimbare parolă cu sincronizare DB |

### Componente Cheie

- **MainLayout**: Layout principal cu sidebar responsive
- **ProtectedRoute**: Guard pentru rute protejate
- **AuthContext**: Context pentru starea de autentificare

---

## Docker Setup

### Servicii

| Serviciu | Port | Descriere              |
| -------- | ---- | ---------------------- |
| postgres | 5432 | PostgreSQL 16 database |
| backend  | 8080 | Spring Boot API        |
| frontend | 80   | Nginx + React SPA      |

### Comenzi

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild after changes
docker-compose up -d --build
```

### Environment Variables

Copiază `.env.example` în `.env` și configurează:

```env
DB_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_key
MAIL_HOST=smtp.example.com
MAIL_USERNAME=user@example.com
MAIL_PASSWORD=mail_password
```

---

## Verificări Efectuate

### Frontend Build ✅

```bash
cd frontend && npm run build
# ✓ 11749 modules transformed
# ✓ built in 3.89s
```

### TypeScript Fixes Applied

1. **Import paths** - Corectat căile relative
2. **Type-only imports** - Pentru `verbatimModuleSyntax`
3. **MUI Grid v7** - Sintaxa `size={{ xs: "grow" }}`
4. **Enum support** - Ajustat `erasableSyntaxOnly`

---

## Pași Următori

1. **Milestone 4**: Implementare funcționalități core ✅
   - CRUD complet Schedule
   - Marcare ore Timesheet
   - Submit pontaj
   - **DB Sync Profile**: Sincronizare profil și securitate parolă

2. **Milestone 5**: Generare PDF ✅
   - Template Anexa 1
   - Template Anexa 3

3. **Milestone 6**: Dashboard Secretariat ✅
   - Overview pontaje în timp real pe Dashboard
   - Statistici automate (submitted, draft, missing)

---

## Note Tehnice

### MUI Grid v7

```tsx
// Sintaxa corectă pentru coloane flexibile:
<Grid size={{ xs: "grow" }}>  // NU xs={true}

// Sintaxa pentru dimensiuni fixe:
<Grid size={{ xs: 12, md: 6 }}>
```

### TypeScript verbatimModuleSyntax

```tsx
// Tipurile trebuie importate separat:
import type { User, Schedule } from "./types";
import { UserRole, ActivityType } from "./types";
```

---

how to run backend:
./mvnw spring-boot:run

## Ultima Actualizare

**Data:** 2026-03-03  
**Versiune:** 1.0.0 (Milestone 1-6 Complete)
