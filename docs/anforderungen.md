# Anforderungen: Einkaufslisten-App

Kleine, fokussierte Multi-User-Einkaufslisten-App als Svelte/Spring-Boot-Wochenendprojekt. Inspiration: Struktur/Trennung von Microsoft To Do, aber ohne dessen Task-Management-Overhead (keine Fälligkeitsdaten, keine Schritte).

## 1. Grundidee

- Fokus: **Liste statt Kacheln** – klassische, vertikale Item-Liste (à la Microsoft To Do), keine Karten-/Grid-Darstellung wie Bring!
- Trennung Liste ↔ Detail (Listenübersicht vs. einzelne Liste mit Items)

## 2. Domänenmodell (Entwurf)

```
User {
  id
  googleId
  email
  name
}

List (Einkaufsliste) {
  id
  name
  owner: User
  members: List<User>
  status: AKTIV | ARCHIVIERT
  archivedAt: timestamp?
}

Item {
  id
  list: List
  bezeichnung: string        // "Tomaten"
  menge: number               // 500
  einheit: Unit
  kategorie: Category
  abgehakt: boolean
  abgehaktAm: timestamp?
  reihenfolge: int            // freie Sortierung innerhalb der Liste
}

Category (global) {
  id
  name                        // z.B. "Gemüse", "Milchprodukte"
}

Unit (erweiterbar, vorbefüllt) {
  id
  name                        // "Stk", "kg", "g", "l", "Becher", "Bund", "Packung", ...
  abbreviation
}
```

## 3. Funktionale Anforderungen

### Items
- Ein Item hat: Bezeichnung, Menge, Einheit, Kategorie
- **Kein** Fälligkeitsdatum, keine Erinnerungen, keine Unterschritte
- Items können abgehakt werden

### Abhaken-Verhalten
- Abgehakte Items rutschen sichtbar nach unten (durchgestrichen/ausgegraut), bleiben aber in der Liste sichtbar
- Items werden **nicht automatisch gelöscht**
- Erst wenn **alle** Items einer Liste abgehakt sind, "verschwindet" die Liste automatisch ins **Archiv**
- Archivierte Listen sind einsehbar und können **reaktiviert** werden
- Bei Reaktivierung: alle Haken werden zurückgesetzt (Liste startet wieder "leer" abgehakt)

### Kategorisierung
- Kategorien sind **global** (eine Kategorie-Tabelle für alle Listen, z. B. "Gemüse", "Tiefkühl", "Getränke")
- Die **Sortierung/Reihenfolge der Kategorien ist pro Liste frei wählbar** (nicht global fix)
- Items sortieren sich innerhalb der Liste nach der listen-eigenen Kategorie-Reihenfolge

### Einheiten
- Kleine, **erweiterbare** Tabelle (kein hartcodiertes Enum)
- Wird mit sinnvollen Standardwerten vorbefüllt: Stk, kg, g, l, ml, Becher, Bund, Packung, Dose, ...
- Nutzer können bei Bedarf eigene Einheiten ergänzen

### Multiuser & Sharing
- Jede Liste hat genau einen **Owner** und eine **Liste von Mitgliedern**
- Login/Auth via **Google OAuth2**
- Mitglieder einer Liste können Items gemeinsam bearbeiten/abhaken

### Sync
- **Polling** (kein WebSocket) reicht für den Start – einfacher Intervall-Refresh der Listendaten

## 4. Nicht-funktionale Anforderungen / Tech-Stack

| Bereich | Wahl |
|---|---|
| Backend | Spring Boot, REST-API |
| Auth | Spring Security + Google OAuth2 |
| Frontend | Svelte (PWA-fähig für Homescreen-Installation) |
| Datenhaltung | Postgres via Docker (von Anfang an, keine H2-Zwischenlösung) |
| Sync | Polling-Intervall (z. B. alle 5–10s), kein WebSocket vorerst |
| Deployment Frontend | Vercel (GitHub-Anbindung, wie beim andi-Projekt) |
| Deployment Backend + DB | Render (Dockerfile-basiert, managed Postgres inkl. Backups; guter Free-Einstieg, gute EU-Nähe, Cold-Start nach Inaktivität bei privater Nutzung vernachlässigbar) |
| Lokale Entwicklung | docker-compose (Postgres + Spring Boot), 1:1 auf Render übertragbar |
| DB-Migration | Flyway (Community Edition) — plain SQL, passt zum Single-Database-Setup (Postgres), kein Multi-DB-Bedarf |
| Repo-Struktur | Monorepo (`backend/`, `frontend/`, `docker-compose.yml` im Root); Render/Vercel nutzen Root-Directory-Einstellung |

## 5. Entwicklungsprozess (iterativ)

1. Projektgerüst → GitHub (Backend- + Frontend-Skeleton, Monorepo)
2. Docker-Datenbank (Postgres), testen, deployen
3. Daten-Access-Layer (JPA-Entities, Repositories), testen
4. Services (Business-Logik: Archivierung, Sortierung etc.), testen
5. REST-Services (Controller, DTOs), testen, deployen
6. Client mit Mock-Backend, testen, deployen
7. Client + echtes Backend, testen, deployen

Jeder Schritt wird getestet (wo sinnvoll auch deployt), bevor der nächste beginnt.

## 6. Import von Text-Einkaufslisten (z. B. NYT Cooking)

Rezept-/App-Exporte (z. B. NYT Cooking "Grocery List") sollen sich per Copy-Paste importieren lassen.

**Herausforderungen im Rohtext**
- Kopfzeile (Rezeptname, Portionenanzahl) ist Kontext, kein Item
- Mengenangaben oft als Unicode-Brüche (½, ¼, ⅓) statt Dezimalzahlen
- Zusätze wie "plus more" / "plus more as needed" – keine feste Menge
- Alternativen in einem Item ("or turkey or chicken, or vegan meat") – mehrdeutig
- Freitext-Hinweise wie "for serving" gehören oft noch zum vorherigen Item
- Footer mit Rezept-Link soll ignoriert werden

**Ablauf**
1. **Paste-Import**: Freitext einfügen, App parst zeilenweise
2. Parser extrahiert pro Zeile: `Menge` (Unicode-Brüche → Dezimal konvertieren), `Einheit` (Abgleich mit Einheiten-Tabelle), `Bezeichnung` (Rest)
3. **Vorschau vor Übernahme** – geparste Items werden angezeigt und können vom Nutzer korrigiert werden (wichtig bei Mehrdeutigkeiten wie "plus more" oder "or ...")
4. Kategorie bleibt beim Import zunächst "Unkategorisiert" (später ggf. einfaches Keyword-Mapping, z. B. "Zwiebel" → Gemüse)
5. Kopfzeile und Footer/Links werden über Marker (Trennlinien, Leerzeilen) erkannt und ignoriert

## 7. Offene Punkte (für später)

- Polling-Intervall genau festlegen
- Einladung von Mitgliedern: per E-Mail-Adresse oder Link-Share?
- Soll es eine Undo-Funktion beim versehentlichen Abhaken/Archivieren geben?
- Reihenfolge der Items **innerhalb** einer Kategorie: frei per Drag&Drop oder alphabetisch?
