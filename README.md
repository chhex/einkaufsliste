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

### Backend + DB (via Docker)

```bash
docker compose up --build
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

## Entwicklungsprozess

Das Projekt wird iterativ in 7 Schritten aufgebaut (siehe Anforderungsdokument,
Abschnitt "Entwicklungsprozess"):

1. Projektgerüst → GitHub
2. Docker-Datenbank, testen, deployen
3. Daten-Access-Layer, testen
4. Services, testen
5. REST-Services, testen, deployen
6. Client mit Mock-Backend, testen, deployen
7. Client + echtes Backend, testen, deployen

## Deployment

- Frontend: Vercel (Root Directory: `frontend/`)
- Backend + Postgres: Render (Root Directory: `backend/`, Dockerfile-basiert)
