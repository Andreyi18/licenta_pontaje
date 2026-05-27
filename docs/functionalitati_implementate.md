# Documentație Funcționalități Implementate

Acest document ține evidența tuturor funcționalităților, reparațiilor și modulelor implementate pe parcursul dezvoltării aplicației, pentru a ușura redactarea lucrării de licență.

## 1. Configurare și Reparații Infrastructură (Docker)
**Data:** 16-17 Martie 2026

* **Problema:** Backend-ul de Spring Boot refuza să se compileze în containerul Docker rulat pe Windows/WSL2, aruncând eroarea `exec format error` cu exit code 1 în stagiul de build.
* **Soluția:** Am diagnosticat problema ca fiind o incompatibilitate arhitecturală a imaginii de bază `maven:3.9.6-eclipse-temurin-17`. Am modificat fișierul `backend/Dockerfile` să folosească imaginea `maven:3.9.6-amazoncorretto-17` ca builder, ceea ce a rezolvat compilarea și a permis pornirea corectă a containerului `upt-pontaje-backend`.
* **Beneficiu:** Aplicația (Frontend + Backend + Bază de Date) rulează acum fluid doar dintr-un singur `docker compose up -d`, indiferent de sistemul de operare.

## 2. Remediere Eroare Actualizare Profil (Eroare 500 Backend)
**Data:** 17 Martie 2026

* **Problema:** La încercarea de a edita profilul utilizatorului din frontend, backend-ul returna `500 Internal Server Error`, generând un `HttpMediaTypeNotAcceptableException`. 
* **Soluția:** După analiza log-urilor și a codului sursă (`AuthController.java` și `UserResponse.java`), s-a constatat că framework-ul de serializare (Jackson) nu putea converti obiectul `UserResponse` în JSON deoarece îi lipseau metodele "Getters". S-a rezolvat prin eliminarea dependenței de biblioteca `Lombok` și scrierea explicită a metodelor `get()` pentru toate proprietățile DTO-ului (ex: `getId()`, `getEmail()` etc).
* **Beneficiu:** Utilizatorii își pot edita și vizualiza acum propriul profil fără erori. 

## 3. Curățare și Configurare Git (`.gitignore`)
**Data:** 17 Martie 2026

* **Soluția:** S-a creat un fișier standard `.gitignore` specific mediului Java/Spring Boot în folderul `backend/`. Au fost eliminate din istoricul Git fișierele compilate nedorite (ex. `.class` și directoarele `target/`).
* **Beneficiu:** Menține repository-ul curat, previne conflictele de cod inutile pe fișiere generate automat și reduce dimensiunea totală a proiectului pe GitHub.

## 4. Panoul de Control al Secretariatului (Secretariat Dashboard)
**Data:** 17 Martie 2026

* **Componentă creată:** `SecretariatDashboardPage.tsx`
* **Descriere:** S-a creat interfața grafică completă necesară rolului de Secretariat pentru gestionarea pontajelor tuturor cadrelor didactice.
* **Funcționalități majore incluse:**
  1. **Selectoare și Filtre:** Permite filtrarea dinamică a pontajelor vizualizate în funcție de Lună, An și Status (Aprobate, Ciorne, Trimise).
  2. **Statistici în timp real (Cards):** Extrage și afișează numărul total de cadre didactice, pontajele trimise spre validare, cele deja aprobate și cele care lipsesc sau sunt doar ciornă.
  3. **Tabel Centralizator:** Un tabel responsiv care listează utilizatorii alături de totalul de ore lucrate, orele suplimentare și un badge vizual ce indică starea (statusul) pontajului.
  4. **Generare și Concatenare PDF:** S-a integrat un buton "Descarcă PDF Consolidat". La apăsare, backend-ul primește cererea (`SecretariatController.mergeDocuments`), lipește toate documentele pontajelor aferente acelei luni într-un singur fișier PDF continuu și forțează descărcarea acestuia în browser-ul secretarei, salvând un timp enorm în procesul de printare.
  5. **Securitate (Routing):** Pagina este protejată folosind `ProtectedRoute`, fiind accesibilă strict utilizatorilor autentificați ce dețin rolul `SECRETARIAT` sau `ADMIN`.


## 5. Rezoluții Tehnice & Fix-uri (Mentenanță)
**Data:** 17 Martie 2026

**Descriere**: Pe parcursul implementării au fost rezolvate mai multe blocaje critice de mediu și cod care împiedicau build-ul de producție.

**Fix-uri importante**:
- **Docker Network/DNS Fix**: Rezolvarea erorilor `DeadlineExceeded` și `i/o timeout` la download-ul de imagini Docker. S-a realizat prin configurarea manuală a DNS-ului în WSL (`resolv.conf`) și forțarea repornirii serviciului.
- **MUI v7 migration**: Adaptarea codului la noul standard `size` prop de la Material UI v7 pentru componenta `Grid`, asigurând un build fără erori TypeScript.
- **TypeScript Strict Mode**: Corectarea importurilor de tipuri (`import type`) și a neconcordanțelor de interfețe (ex: `userFullName` vs `userName`) conform cerințelor Vite 7.
- **Stabilitate Containere**: Depanarea stării "unhealthy" a bazei de date cauzată de timpii mari de startup, asigurând secvențialitatea corectă a pornirii serviciilor.

---
*Acest document va fi actualizat continuu pe parcursul adăugării noilor funcționalități.*
