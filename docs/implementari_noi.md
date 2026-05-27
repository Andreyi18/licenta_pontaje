# Implementări Noi — Milestone 7 & 8

**Data:** 5 Aprilie 2026

Acest document descrie funcționalitățile implementate în sesiunea curentă de dezvoltare, care completează aplicația cu paginile lipsă din interfața cadrului didactic și cu fluxul de aprobare pentru secretariat.

---

## 7. Pagina Pontaj Lunar (`TimesheetPage.tsx`)

**Rută:** `/timesheet` · **Rol:** `CADRU_DIDACTIC`

**Descriere:** Cadul didactic poate vizualiza și completa pontajul lunar printr-un calendar interactiv.

**Funcționalități:**
1. **Navigare luni** — butoane `<` / `>` pentru a schimba luna și anul afișat.
2. **Calendar lunar** — grilă de tip calendar (Luni→Duminică) cu toate zilele lunii; weekendurile sunt marcate diferit; ziua curentă este evidențiată.
3. **Vizualizare ore pe zi** — fiecare celulă afișează intervalele orare înregistrate, colorate după tipul orei (verde = normă, albastru = plată cu ora).
4. **Dialog de completare** — click pe orice zi deschide un dialog cu:
   - Lista orelor deja înregistrate în acea zi (cu buton de ștergere)
   - Formular de adăugare: interval orar (dropdown), tip oră (radio NORMA / PLATA_ORA), activitate (text liber)
5. **Sumar ore** — afișare în timp real a totalului de ore normă, plată cu ora și total.
6. **Trimitere pontaj** — buton „Trimite pontajul" activ când statusul este DRAFT și există cel puțin o intrare.
7. **Mod readonly** — când pontajul este SUBMITTED sau APPROVED, dialogul nu mai permite modificări.

**Conexiuni backend:**
- `POST /api/timesheets` — getOrCreate
- `POST /api/timesheets/{id}/entries` — adaugă oră
- `DELETE /api/timesheets/{id}/entries/{entryId}` — șterge oră
- `POST /api/timesheets/{id}/submit` — trimite pontaj

---

## 8. Pagina Orar Săptămânal (`SchedulePage.tsx`)

**Rută:** `/schedule` · **Rol:** `CADRU_DIDACTIC`

**Descriere:** Gestionarea completă a orarului săptămânal al cadrului didactic.

**Funcționalități:**
1. **Vizualizare pe zile** — activitățile sunt grupate pe zile (Luni → Duminică), sortate după interval orar.
2. **Adăugare activitate** — dialog cu câmpuri: zi, interval orar, disciplină, sală (opțional), tip activitate (Curs/Seminar/Laborator/Proiect).
3. **Editare activitate** — click pe iconul de editare deschide dialogul pre-completat cu datele existente.
4. **Ștergere activitate** — buton de ștergere pe fiecare rând, cu loading state.
5. **Ștergere totală** — buton cu confirmare pentru a șterge întreg orarul.
6. **Import CSV / Excel** — butoane de upload pentru importul orarului din fișiere, cu feedback la numărul de activități importate.
7. **Badge culori tip activitate** — fiecare tip de activitate are o culoare distinctă (Curs=albastru închis, Seminar=albastru, Laborator=verde, Proiect=portocaliu).

**Conexiuni backend:**
- `GET /api/schedules` — lista orar
- `POST /api/schedules` — creare
- `PUT /api/schedules/{id}` — editare
- `DELETE /api/schedules/{id}` — ștergere
- `DELETE /api/schedules/all` — ștergere totală
- `POST /api/schedules/import/csv` — import CSV
- `POST /api/schedules/import/excel` — import Excel

---

## 9. Pagina Documente & Anexe (`DocumentsPage.tsx`)

**Rută:** `/documents` · **Rol:** `CADRU_DIDACTIC`

**Descriere:** Generarea și descărcarea anexelor salariale PDF (Anexa 1 și Anexa 3) pentru pontajele finalizate.

**Funcționalități:**
1. **Selectare perioadă** — selectoare lună/an pentru a alege pontajul dorit.
2. **Info pontaj** — card cu statusul pontajului, orele totale pe tipuri.
3. **Generare Anexa 1** — buton care apelează backend-ul pentru generarea PDF-ului cu situația orelor în normă. Butonul devine „Regenerează" dacă documentul există deja.
4. **Generare Anexa 3** — similar pentru orele cu plată cu ora.
5. **Restricție generare** — butoanele de generare sunt active doar când pontajul este SUBMITTED sau APPROVED (nu DRAFT).
6. **Lista documente** — tabel cu toate documentele generate pentru perioada selectată: nume fișier, tip, data generării, buton descărcare.
7. **Descărcare directă** — `download()` trigger în browser pentru fiecare document PDF.

**Conexiuni backend:**
- `GET /api/timesheets/{month}/{year}` — pontaj perioadă
- `GET /api/documents` — lista documente
- `POST /api/documents/generate` — generare PDF
- `GET /api/documents/{id}/download` — descărcare PDF

---

## 10. Pagina Gestionare Utilizatori (`AdminUsersPage.tsx`)

**Rută:** `/admin/users` · **Rol:** `ADMIN`

**Descriere:** Panou de administrare a tuturor conturilor din sistem.

**Funcționalități:**
1. **Tabel utilizatori** — afișează toți utilizatorii cu: avatar inițiale, nume, email, rol (badge colorat), departament, status (Activ/Inactiv).
2. **Căutare** — input search cu filtrare live după nume sau email.
3. **Filtru rol** — dropdown pentru filtrarea după rol (Cadru Didactic / Secretariat / Admin).
4. **Creare utilizator** — dialog cu câmpuri: prenume, nume, email, rol, parolă.
5. **Editare utilizator** — dialog pre-completat; parola este opțională la editare.
6. **Activare/Dezactivare** — toggle rapid cu confirmare vizuală (iconiță diferită pentru activ/inactiv).
7. **Ștergere cu confirmare** — dialog de confirmare înainte de ștergerea definitivă.

**Conexiuni backend:**
- `GET /api/users` — lista utilizatori (cu filtre)
- `POST /api/users` — creare
- `PUT /api/users/{id}` — editare
- `PATCH /api/users/{id}/status` — activare/dezactivare
- `DELETE /api/users/{id}` — ștergere

---

## 11. Flux Aprobare Pontaj (Secretariat)

**Rută:** `/secretariat/timesheets/:id` · **Rol:** `SECRETARIAT`, `ADMIN`

**Descriere:** Secretariatul poate acum aproba pontajele trimise direct din pagina de detalii.

**Modificări:**
- `SecretariatTimesheetDetailsPage.tsx` — adăugat buton „Aprobă pontajul" (vizibil doar când status = SUBMITTED), cu loading state și actualizare imediată a statusului în UI după aprobare.
- `SecretariatController.java` — adăugat endpoint `POST /api/secretariat/timesheets/{id}/approve`.
- `TimesheetService.java` — adăugată metoda `approve(UUID timesheetId)` cu validare (acceptă doar SUBMITTED → APPROVED).
- `api.ts` — adăugată funcția `secretariatApi.approveTimesheet(id)`.

---

## Modificări infrastructură

### `App.tsx`
- Înlocuite placeholder-ele `<div>...-Todo</div>` cu componentele reale importate.
- Eliminată ruta `/settings` (neimplementată, va fi adăugată ulterior).

### `api.ts`
- Adăugat `secretariatApi.approveTimesheet(timesheetId)`.

---

## 12. Sistem Notificări In-App

**Data:** 5 Aprilie 2026

### Backend
- **`NotificationRepository.java`** — repository JPA cu metode: `findByUserIdOrderByCreatedAtDesc`, `countByUserIdAndIsReadFalse`, `markAllReadByUserId` (query JPQL bulk update).
- **`NotificationService.java`** — serviciu cu: `getForUser`, `getUnreadCount`, `markAsRead`, `markAllAsRead`, `create`.
- **`NotificationController.java`** — 4 endpoint-uri:
  - `GET /api/notifications` — lista notificărilor utilizatorului curent
  - `GET /api/notifications/unread-count` — număr necitite
  - `PATCH /api/notifications/{id}/read` — marchează una ca citită
  - `PATCH /api/notifications/read-all` — marchează toate ca citite
- **Notificări automate** în `TimesheetService`:
  - La `submit()` → notificare tip SYSTEM: „Pontaj trimis"
  - La `approve()` → notificare tip SYSTEM: „Pontaj aprobat" (trimisă utilizatorului al cărui pontaj a fost aprobat)

### Frontend
- **`MainLayout.tsx`** — iconița de notificări din AppBar acum afișează un badge roșu cu numărul de notificări necitite. Click deschide un Popover cu:
  - Lista notificărilor (cele necitite au fundal albastru deschis)
  - Click pe o notificare necitită → o marchează ca citită
  - Buton „Marchează toate" pentru bulk read
  - Polling automat la 60 secunde
- **`types/index.ts`** — adăugat interfața `AppNotification`
- **`api.ts`** — adăugat `notificationsApi` cu toate cele 4 operații

---

## 13. Paginare Tabele

**Data:** 5 Aprilie 2026

- **`AdminUsersPage.tsx`** — `TablePagination` MUI cu 5/10/25 rânduri pe pagină, etichete în română.
- **`SecretariatDashboardPage.tsx`** — `TablePagination` similar, afișat doar când există date.

---

## 14. Empty States Îmbunătățite

**Data:** 5 Aprilie 2026

- **`SchedulePage.tsx`** — empty state cu iconiță CalendarMonth, mesaj descriptiv și buton direct de adăugare.
- **`DocumentsPage.tsx`** — empty state cu iconiță PDF și mesaj contextual (indică să trimită pontajul dacă e DRAFT).

---

*Document actualizat pe 5 Aprilie 2026 — Milestone 7–14 complete.*
