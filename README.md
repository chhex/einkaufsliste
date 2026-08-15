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
   (von Render), `DB_APP_USER`/`DB_APP_PASSWORD` (aus `.env.render`, Schritt 2)

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
