# Funcționalități Noi — Trimitere Raport pe Email & Asistent AI

**Data:** 22 Iunie 2026

Acest document descrie două funcționalități adăugate aplicației „Pontaje UPT": (1) trimiterea pe email a raportului centralizat de către secretariat și (2) un asistent virtual bazat pe inteligență artificială (LLM), cu răspuns în flux (streaming). Documentul prezintă arhitectura, fișierele implicate, fluxul de date, deciziile tehnice și modul de configurare/testare.

---

## A. Trimitere Raport Centralizat pe Email (Secretariat)

### A.1. Descriere generală

Secretariatul putea anterior doar să **descarce** raportul centralizat (PDF-ul cu pontajele consolidate). Noua funcționalitate permite **trimiterea acestui raport direct pe email**, cu PDF-ul atașat automat, către unul sau mai mulți destinatari (ex: decanat, director de departament), fără a mai descărca și atașa manual fișierul.

### A.2. Arhitectură

Trimiterea efectivă a emailului se face pe **backend** (Spring Boot), nu în browser. Motivul este securitatea: credențialele serverului SMTP nu trebuie să ajungă niciodată pe client. Fluxul este:

```
Frontend (React)                Backend (Spring Boot)              Server SMTP
  SendReportDialog  ── POST ──►  SecretariatController  ── SMTP ──►  MailHog / Gmail
                                 ├─ generează PDF (PdfGeneratorService)
                                 └─ trimite email (EmailService)
```

### A.3. Componente backend

- **`EmailService.java`** (`service/`) — serviciu nou care trimite emailuri cu atașament PDF folosind `JavaMailSender` (din `spring-boot-starter-mail`). Construiește un `MimeMessage` multipart (`MimeMessageHelper`), setează expeditor/destinatari/subiect/corp și adaugă PDF-ul ca `ByteArrayResource`. Metode:
  - `isConfigured()` — verifică dacă serviciul are credențiale (adresa expeditorului este setată);
  - `getDefaultRecipient()` — întoarce destinatarul implicit configurat pe server;
  - `sendReportWithAttachment(to, cc, subject, body, attachment, fileName)` — trimite efectiv emailul; aruncă `BadRequestException` cu mesaj clar dacă serviciul nu e configurat sau dacă trimiterea SMTP eșuează.

- **`SendReportRequest.java`** (`dto/secretariat/`) — DTO pentru cerere, cu validare (`@NotEmpty` pe lista de destinatari, `@Email` pe fiecare adresă). Câmpuri: `to`, `cc`, `subject`, `body`, `departmentId`.

- **`SecretariatController.java`** — au fost adăugate:
  - extragerea logicii de generare a raportului consolidat într-o metodă reutilizabilă `buildMergedReport(month, year, departmentId)` (folosită atât de descărcare cât și de trimiterea pe email);
  - `GET /api/secretariat/reports/email-config` — întoarce dacă emailul e configurat și destinatarul implicit (pentru UI);
  - `POST /api/secretariat/reports/send` — generează raportul și îl trimite pe email; subiectul și corpul sunt precompletate automat în română (ex: „Raport pontaje iunie 2026") dacă nu sunt furnizate.

### A.4. Componente frontend

- **`SendReportDialog.tsx`** (`components/secretariat/`) — dialog nou cu:
  - câmpuri **To** și **Cc** (mai multe adrese separate prin virgulă, validate live, afișate ca chips);
  - subiect și mesaj precompletate, editabile;
  - afișarea numelui PDF-ului care va fi atașat;
  - avertizare dacă serviciul de email nu e configurat pe server;
  - precompletarea automată a câmpului **To** cu destinatarul implicit (citit la runtime din backend).

- **`SecretariatDashboardPage.tsx`** — buton nou „Trimite pe email" lângă „Descarcă Centralizator PDF".

- **`api.ts`** — funcții noi în `secretariatApi`: `getEmailConfig()` și `sendReport(month, year, payload)`.

### A.5. Configurare

Configurarea SMTP se face prin variabile de mediu (fișierul `.env`), citite în `application.yml`:

| Variabilă | Rol |
|-----------|-----|
| `MAIL_HOST` / `MAIL_PORT` | serverul SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | credențiale (și adresa expeditorului) |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | autentificare și TLS (configurabile) |
| `REPORT_DEFAULT_RECIPIENT` | destinatar implicit precompletat în câmpul To |

### A.6. Testare cu MailHog

Pentru testare fără credențiale reale și fără a trimite emailuri către adrese reale, s-a adăugat în `docker-compose.yml` serviciul **MailHog** — un server SMTP de test care prinde toate emailurile și le afișează într-o interfață web:
- SMTP pe portul `1025`, interfață web pe `http://localhost:8025`;
- backend-ul folosește implicit MailHog (`MAIL_HOST=mailhog`, fără autentificare/TLS);
- pentru emailuri reale prin Gmail se completează în `.env` credențialele Gmail (cu un „App Password" de 16 caractere) și se activează `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`.

### A.7. Tehnologii

`spring-boot-starter-mail` (Jakarta Mail), `JavaMailSender`, `MimeMessageHelper`, MailHog (test), MUI (dialog frontend).

---

## B. Asistent Virtual AI (Chatbot)

### B.1. Descriere generală

În colțul din dreapta-jos al aplicației există un asistent virtual flotant. Inițial acesta funcționa pe bază de **reguli hardcodate** (potrivire de cuvinte-cheie → răspunsuri fixe). A fost înlocuit cu un **model de limbaj real (LLM)**, care înțelege întrebări formulate liber și răspunde contextual despre funcțiile aplicației. Răspunsul apare **token cu token (streaming)**, ca la ChatGPT.

### B.2. Furnizorul AI — Groq

Se folosește **Groq** (https://groq.com), un furnizor de inferență LLM cu nivel gratuit, foarte rapid, care expune un **API compatibil OpenAI**. Modelul implicit este `llama-3.3-70b-versatile`. Compatibilitatea cu API-ul OpenAI înseamnă că aceeași implementare poate fi reorientată ușor către alt furnizor (OpenAI, OpenRouter etc.) doar schimbând URL-ul și cheia.

### B.3. Arhitectură (cheia rămâne pe server)

Apelul către Groq se face din **backend**, nu din browser. Cheia API este un secret și nu trebuie expusă în codul client. Frontend-ul apelează backend-ul propriu, care acționează ca **proxy** către Groq:

```
Frontend (AssistantWidget) ──► Backend (AssistantController) ──► Groq API (LLM)
        chat / chatStream            AssistantService               (compatibil OpenAI)
```

### B.4. Grounding prin „system prompt"

Pentru ca AI-ul să răspundă corect despre aplicație (și să nu „inventeze" funcții), `AssistantService` construiește un **mesaj de sistem** care descrie:
- ce este aplicația (sistem de pontaje pentru cadrele didactice și secretariatul UPT);
- funcționalitățile reale (Orar, Pontaj, Documente/Anexa 1, Centralizator, Profil);
- **rolul și numele utilizatorului curent** (cadru didactic / secretariat / administrator), pentru răspunsuri personalizate;
- instrucțiuni de stil (răspunsuri scurte, în română, cu diacritice, fără a inventa funcții).

Astfel, „datele" nu mai sunt hardcodate ca răspunsuri fixe, ci sunt furnizate ca **context** unui model care formulează singur răspunsul.

### B.5. Componente backend

- **`AssistantService.java`** (`service/`):
  - `chat(history, user)` — apel non-streaming către Groq prin `RestClient` (Spring 6.1); întoarce textul complet;
  - `streamChat(history, user, outputStream)` — apel streaming: trimite către Groq `"stream": true`, citește răspunsul SSE linie-cu-linie cu `java.net.http.HttpClient` (`BodyHandlers.ofLines()`) și retransmite fiecare token către client în format **NDJSON** (`{"t":"..."}` pentru fiecare bucată, `{"done":true}` la final, `{"error":"..."}` la eroare);
  - `buildMessages(...)` — compune system prompt + ultimele ~12 mesaje din istoric (pentru a limita dimensiunea cererii);
  - `isConfigured()` — verifică dacă există cheia `GROQ_API_KEY`.

- **`AssistantController.java`** (`controller/`):
  - `GET /api/assistant/config` — dacă asistentul e activat (are cheie);
  - `POST /api/assistant/chat` — răspuns complet (non-streaming);
  - `POST /api/assistant/chat/stream` — răspuns în flux, folosind `StreamingResponseBody` (scriere incrementală cu flush, `Content-Type: application/x-ndjson`).

- **DTO-uri** (`dto/assistant/`): `ChatMessage(role, content)`, `ChatRequest(messages)`, `ChatResponse(reply)`.

- Toate rutele `/api/assistant/**` sunt protejate cu JWT (`anyRequest().authenticated()` în `SecurityConfig`), deci doar utilizatorii autentificați pot folosi asistentul, iar backend-ul cunoaște utilizatorul curent.

### B.6. Componente frontend

- **`AssistantWidget.tsx`** (`components/common/`) — a fost rescris:
  - eliminate complet regulile/intențiile hardcodate; textul liber este trimis către AI împreună cu istoricul conversației;
  - păstrate scurtăturile de navigare (chips) care duc direct la paginile aplicației (UX, nu „răspunsuri");
  - răspunsul AI apare **progresiv** (token cu token) — textul botului crește pe ecran pe măsură ce sosesc bucățile;
  - **fallback transparent**: dacă streaming-ul eșuează complet, se folosește automat varianta non-streaming, fără a afișa eroare.

- **`api.ts`** — `assistantApi` cu: `getConfig()`, `chat(messages)` și `chatStream(messages, onToken, signal)`. Funcția de streaming citește răspunsul cu `fetch` + `ReadableStream`, parsează liniile NDJSON și apelează `onToken` pentru fiecare bucată de text.

### B.7. Streaming — detaliu tehnic și o problemă rezolvată

Răspunsurile streaming folosesc transfer „chunked" (HTTP `Transfer-Encoding: chunked`). La final, browserul poate semnala `ERR_INCOMPLETE_CHUNKED_ENCODING` (o eroare de rețea apărută *după* ce tot textul a fost primit), din cauza modului în care se închide conexiunea. Pentru a evita ca utilizatorul să vadă o falsă „network error", s-au aplicat:
- pe **frontend**: la primirea marcajului `{"done":true}` se anulează cititorul; orice eroare apărută *după* ce răspunsul a fost deja livrat este ignorată; dacă streaming-ul nu pornește deloc, se face fallback la varianta non-streaming;
- pe **backend**: stream-ul către Groq este închis corect (try-with-resources), pentru o terminare curată a transferului.

### B.8. Configurare

| Variabilă | Rol |
|-----------|-----|
| `GROQ_API_KEY` | cheia API Groq (gratuită, de la https://console.groq.com/keys) |
| `GROQ_MODEL` | modelul folosit (implicit `llama-3.3-70b-versatile`) |

Cheia se pune în fișierul `.env` (ignorat de git, deci nu ajunge în repository). Dacă lipsește, asistentul răspunde cu un mesaj clar că nu este configurat, iar restul aplicației funcționează normal.

### B.9. Tehnologii

LLM prin **Groq** (API compatibil OpenAI, model Llama 3.3 70B), Spring `RestClient` (non-streaming) și `java.net.http.HttpClient` + `StreamingResponseBody` (streaming), format **NDJSON** pentru transmiterea tokenilor, `fetch` + `ReadableStream` pe frontend, MUI pentru interfața de chat.

---

## C. Rezumat fișiere modificate/adăugate

**Backend (nou):**
- `service/EmailService.java`, `dto/secretariat/SendReportRequest.java`
- `service/AssistantService.java`, `controller/AssistantController.java`, `dto/assistant/{ChatMessage,ChatRequest,ChatResponse}.java`

**Backend (modificat):**
- `controller/SecretariatController.java` (rute email + refactorizare generare raport)
- `resources/application.yml` (config mail + assistant)

**Frontend (nou):**
- `components/secretariat/SendReportDialog.tsx`
- (rescris) `components/common/AssistantWidget.tsx`

**Frontend (modificat):**
- `pages/SecretariatDashboardPage.tsx`, `api/api.ts`

**Infrastructură:**
- `docker-compose.yml` (serviciu MailHog + variabile MAIL_*, REPORT_DEFAULT_RECIPIENT, GROQ_*)
- `.env` / `.env.example` (variabile de configurare)

---

## D. Aspecte de securitate (relevante pentru lucrare)

1. **Secretele rămân pe server** — atât credențialele SMTP, cât și cheia API Groq sunt citite din mediul backend-ului; nu ajung niciodată în codul client.
2. **Autentificare** — toate endpoint-urile noi necesită un token JWT valid; cele de raport sunt restricționate la rolurile `SECRETARIAT` / `ADMIN`.
3. **Validare** — adresele de email sunt validate (format și prezență) atât pe frontend, cât și pe backend.
4. **Fără secrete în repository** — cheile reale se țin în `.env` (negitat); `.env.example` conține doar placeholder-e.

---

*Document creat pe 22 Iunie 2026 — funcționalități: trimitere raport pe email + asistent AI cu streaming.*
