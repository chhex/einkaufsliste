# Einkaufsliste

Multiuser-Einkaufslisten-App (Svelte + Spring Boot). Details siehe
[`docs/anforderungen.md`](./docs/anforderungen.md).

## Struktur

```
einkaufsliste/
├── backend/    # Spring Boot (Java 21, Postgres, Flyway)
├── frontend/   # Svelte (Vite)
├── docker-compose.yml
└── docs/
    └── anforderungen.md
```

## Lokale Entwicklung

### Setup (einmalig)

```bash
cp .env.example .env
# .env mit echten Werten fuellen (Admin-/App-User-Credentials frei waehlbar)
```

### Datenbank + App-User bootstrappen (einmalig)

```bash
docker compose up -d db
./bootstrap-local.sh
```

Liest Admin- und App-User-Credentials automatisch aus `.env` — keine
manuelle Eingabe, kein Risiko eines Credential-Mismatches zwischen `.env`
und dem Bootstrap-Aufruf.

**Warum überhaupt ein separater Schritt?** Eine gemanagte Datenbank (z. B.
Render) stellt nur einen Admin-User bereit und unterstützt keine
automatischen Init-Skripte beim ersten Start. Damit der Prozess lokal und
in der Cloud identisch bleibt, nutzen beide dasselbe zugrundeliegende
[`backend/db-init/bootstrap-app-user.sh`](./backend/db-init/bootstrap-app-user.sh) —
`bootstrap-local.sh` ist nur ein bequemer lokaler Wrapper drumherum, der
`.env` liest statt Werte erneut abzufragen.

### Backend + DB starten

```bash
docker compose up --build -d
```

Backend läuft danach auf `http://localhost:8080`, Smoke-Test unter
`http://localhost:8080/api/ping`.

### Frontend (Dev-Server)

```bash
cd frontend
npm install
npm run dev
```

Frontend läuft auf `http://localhost:5173`, proxied `/api`-Requests an
den Backend-Port 8080.

### Backend-Tests

Zwei Kategorien, per Namenskonvention getrennt:

```bash
cd backend
mvn test      # nur *Test-Klassen: reine Unit-Tests, KEIN Docker nötig, schnell
mvn verify    # zusätzlich *IT-Klassen: Testcontainers-Integrationstests, braucht laufendes Docker
```

`mvn verify` startet automatisch eine temporäre Postgres-Instanz, wendet
alle Flyway-Migrationen an und testet Repositories/Services dagegen —
unabhängig vom `docker-compose`-Setup, kein Bootstrap-Schritt nötig
(Testcontainer nutzt einen einzigen User für Admin+App). Business-Logik,
die keine Datenbank braucht (z. B. `ShoppingListTest`), läuft dagegen
schon bei `mvn test`.

## Deployment auf Render (identischer Bootstrap-Prozess)

1. Postgres-Instanz auf Render anlegen → liefert Admin-Connection-Info
2. Einmalig App-User anlegen:
   ```bash
   cp .env.render.example .env.render
   # .env.render mit den Render-Connection-Daten + selbst gewähltem App-Passwort füllen
   ./bootstrap-render.sh
   ```
3. Backend als Web Service anlegen (Root Directory `backend/`, Dockerfile wird erkannt)
4. Env-Vars im Render-Dashboard setzen: `DB_URL`, `DB_ADMIN_USER`/`DB_ADMIN_PASSWORD`
   (von Render), `DB_APP_USER`/`DB_APP_PASSWORD` (aus `.env.render`, Schritt 2),
   `JWT_SECRET` (eigener langer Zufallsstring, z. B. `openssl rand -base64 48` —
   **nicht** der unsichere Default aus `application.yml`), `GOOGLE_CLIENT_ID`
   (siehe Abschnitt "Auth" unten)

## Auth

Frontend macht den eigentlichen Google-Login (Google Identity Services),
schickt nur das resultierende ID-Token an `POST /api/auth/google`. Backend
verifiziert es, provisioniert/findet den User (`UserService`), stellt ein
eigenes JWT aus. Alle weiteren Requests: `Authorization: Bearer <token>`.

- **Google-Cloud-Setup**: OAuth2-Client-ID vom Typ "Web Application" anlegen,
  Frontend-Origin(s) autorisieren, Client-ID als `GOOGLE_CLIENT_ID` setzen
- **`app/api/dev/login`**: provisorischer Ersatz für den echten Google-Login
  (liefert ebenfalls ein JWT) — nützlich für curl-Tests, **muss vor
  breiterer Nutzung entfernt oder abgesichert werden**
- **Tests**: eigenes `test`-Spring-Profil (`TestSecurityConfig`, permissiv)
  aktiv über `@ActiveProfiles("test")` in `AbstractIntegrationTest` — IT-/
  MockMvc-Tests brauchen keinen echten Google-Login

### API per curl testen

```bash
chmod +x scripts/test-api.sh
./scripts/test-api.sh                                          # lokal
./scripts/test-api.sh https://einkaufsliste-gnrc.onrender.com   # Render
```

### Echten Google-Login testen (ohne Svelte-Client)

Standalone-Testseite (`scripts/auth-test/index.html`) für den kompletten
echten Fluss (Google-Button → ID-Token → Backend-Verifikation → JWT):

```bash
cd scripts/auth-test
python3 -m http.server 8000
# Browser: http://localhost:8000
```

Voraussetzungen:
- `GOOGLE_CLIENT_ID` muss im Backend gesetzt sein (`.env` bzw. Render-Env-Var)
- In der Google-Cloud-Console bei der OAuth2-Client-ID unter "Authorized
  JavaScript origins" `http://localhost:8000` eintragen
- Backend-URL und Client-ID auf der Testseite selbst eintragen (Felder oben)

## Entwicklungsprozess

Das Projekt wird iterativ in 7 Schritten aufgebaut (siehe Anforderungsdokument,
Abschnitt "Entwicklungsprozess"):

1. ✅ Projektgerüst → GitHub
2. ✅ Docker-Datenbank, Admin-/App-User-Trennung, lokal getestet UND auf Render deployt (`https://einkaufsliste-gnrc.onrender.com`)
3. ✅ Daten-Access-Layer
   - 3a ✅ Domain-Schema (Flyway-Migration), lokal getestet
   - 3b ✅ JPA-Entities + Repositories, getestet via Testcontainers
4. ✅ Services (UserService, ListService, ItemService, UnitService, CategoryService, ImportService), getestet
5. ✅ REST-Services (Controller, DTOs), lokal getestet — Deployment auf Render noch offen (aktueller Render-Stand ist Schritt 2/3)
6. ⏳ Client mit Mock-Backend, testen, deployen
7. ⏳ Client + echtes Backend, testen, deployen

## Deployment

- Frontend: Vercel (Root Directory: `frontend/`)
- Backend + Postgres: Render (siehe "Deployment auf Render" oben)
