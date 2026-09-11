# Einkaufsliste

Multiuser-Einkaufslisten-App (Svelte + Spring Boot). Details siehe
[`docs/anforderungen.md`](./docs/anforderungen.md).

Live: **https://comprarli.com**

## Struktur

```
einkaufsliste/
├── backend/               # Spring Boot (Java 21, Postgres, Flyway)
├── frontend/               # Svelte (Vite), Dockerfile.prod fuer Produktion
├── docker-compose.yml       # Lokale Entwicklung
├── docker-compose.prod.yml  # Produktion (Hetzner)
├── .github/workflows/
│   └── ci-cd.yml             # Tests → Deploy bei Push auf main
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

**Warum überhaupt ein separater Schritt?** Eine gemanagte Datenbank stellt
i. d. R. nur einen Admin-User bereit und unterstützt keine automatischen
Init-Skripte beim ersten Start. Damit der Prozess überall identisch bleibt
(lokal wie auf dem eigenen Server), nutzen beide dasselbe zugrundeliegende
[`backend/db-init/bootstrap-app-user.sh`](./backend/db-init/bootstrap-app-user.sh) —
`bootstrap-local.sh`/`bootstrap-prod.sh` sind nur bequeme Wrapper drumherum,
die `.env` lesen statt Werte erneut abzufragen.

### Backend + DB starten

```bash
docker compose up --build -d
```

Backend läuft danach auf `http://localhost:8080`, Smoke-Test unter
`http://localhost:8080/api/ping`.

### Frontend (Dev-Server)

```bash
cd frontend
cp .env.example .env
# .env: VITE_GOOGLE_CLIENT_ID mit der echten Google-Client-ID füllen
npm install
npm run dev
```

Frontend läuft auf `http://localhost:5173` (in Google Cloud Console als
"Authorized JavaScript origin" hinterlegt, siehe Abschnitt "Auth" unten).

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

## Deployment (Hetzner)

Eigener VPS, alles per Docker: Postgres + Backend + Svelte-Frontend
(gebaut, per nginx ausgeliefert) hinter TLS (Let's Encrypt/Certbot),
eigene Domain `comprarli.com`.

### Architektur

```
Internet → nginx (Port 80/443, TLS) → /api/*  → Backend-Container (intern, Port 8080)
                                     → sonst   → gebautes Svelte-Bundle (statisch)
                                                  ↓
                                              Postgres-Container (intern)
```

Das Frontend ruft die API **same-origin** auf (`/api/...`, keine separate
Domain wie früher bei Vercel+Render) — nginx reicht das intern über das
Docker-Netzwerk an den Backend-Container weiter. Der Backend-Container hat
deshalb **keinen** nach aussen offenen Port mehr; nur nginx (80/443) ist
öffentlich erreichbar.

### Einmaliges Setup auf dem Server

1. Repo klonen nach `/apps/einkaufsliste`
2. `.env` anlegen (siehe `.env.example`) — inkl. `VITE_API_BASE_URL=` (**leer
   lassen**, damit das Frontend relative `/api/...`-Pfade nutzt, die nginx
   dann proxied)
3. Zertifikat besorgen (Certbot/Let's Encrypt) → landet unter
   `/etc/letsencrypt/live/comprarli.com/`, wird von `docker-compose.prod.yml`
   read-only in den Frontend-Container gemountet
4. DB + App-User bootstrappen:
   ```bash
   docker compose -f docker-compose.prod.yml up -d db
   ./bootstrap-prod.sh
   ```
5. Alles starten: `docker compose -f docker-compose.prod.yml up --build -d`

### Laufende Deploys (automatisch)

`.github/workflows/ci-cd.yml`: bei jedem Push auf `main` laufen zuerst die
Backend-Tests (`mvn verify`); nur wenn die **grün** sind, verbindet sich
GitHub Actions per SSH zum Server, holt den neuesten Code und startet die
Container neu (`docker compose -f docker-compose.prod.yml up --build -d`,
bewusst **ohne** vorheriges `down` — nur geänderte Services werden neu
gebaut, die DB läuft währenddessen ohne Unterbruch weiter).

Benötigte GitHub-Secrets: `VPS_HOST`, `VPS_SSH_KEY`.

## Auth

Frontend macht den eigentlichen Google-Login (Google Identity Services),
schickt nur das resultierende ID-Token an `POST /api/auth/google`. Backend
verifiziert es, provisioniert/findet den User (`UserService`), stellt ein
eigenes JWT aus. Alle weiteren Requests: `Authorization: Bearer <token>`.

- **Google-Cloud-Setup**: OAuth2-Client-ID vom Typ "Web Application" anlegen,
  Frontend-Origin(s) autorisieren (`https://comprarli.com`,
  `http://localhost:5173`, ...), Client-ID als `GOOGLE_CLIENT_ID` setzen
- **`/api/dev/login`**: provisorischer Ersatz für den echten Google-Login
  (liefert ebenfalls ein JWT) — nützlich für curl-Tests, **muss vor
  breiterer Nutzung entfernt oder abgesichert werden** (noch offen)
- **Autorisierung**: aktuell prüft das Backend nur, ob ein Request
  überhaupt authentifiziert ist — **nicht**, ob der User Owner/Member der
  angefragten Liste ist (noch offen, siehe `docs/anforderungen.md`)
- **Tests**: eigenes `test`-Spring-Profil (`TestSecurityConfig`, permissiv)
  aktiv über `@ActiveProfiles("test")` in `AbstractIntegrationTest` — IT-/
  MockMvc-Tests brauchen keinen echten Google-Login

### API per curl testen

```bash
chmod +x scripts/test-api.sh
./scripts/test-api.sh                        # lokal
./scripts/test-api.sh https://comprarli.com   # Produktion
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
- `GOOGLE_CLIENT_ID` muss im Backend gesetzt sein (`.env`)
- In der Google-Cloud-Console bei der OAuth2-Client-ID unter "Authorized
  JavaScript origins" `http://localhost:8000` eintragen
- Backend-URL und Client-ID auf der Testseite selbst eintragen (Felder oben)

## Entwicklungsprozess

Das Projekt wurde iterativ in 7 Schritten aufgebaut (siehe
Anforderungsdokument, Abschnitt "Entwicklungsprozess") — alle abgeschlossen:

1. ✅ Projektgerüst → GitHub
2. ✅ Docker-Datenbank, Admin-/App-User-Trennung
3. ✅ Daten-Access-Layer (3a Domain-Schema/Flyway, 3b JPA-Entities/Repositories)
4. ✅ Services (User/List/Item/Unit/Category/Import)
5. ✅ REST-Services (Controller, DTOs)
6. ✅ Client (Auth, Listen-Übersicht, Listen-Detail, Import-Flow, Mitglieder-Verwaltung, Styling/Themes)
7. ✅ Client + echtes Backend, deployt

**Offen (siehe `docs/anforderungen.md`, Abschnitt "Offene Punkte")**:
Autorisierungslücke schliessen, `/api/dev/login` absichern, PWA,
Autocomplete/Self-Learning für Artikelnamen, Mehrsprachigkeit,
Einkaufsorte.
