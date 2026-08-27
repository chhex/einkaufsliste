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
  name: string?                                   // optional, siehe Abschnitt "Anlegen"
  einkaufsdatum: date                              // Default: Erfassungsdatum, aenderbar
  owner: User
  members: List<User>
  status: AKTIV | ARCHIVIERT
  sortierung: kategorie | bezeichnung | einheit   // Default-/Owner-Praeferenz
  archivedAt: timestamp?
}

Item {
  id
  list: List
  bezeichnung: string        // "Tomaten"
  menge: number               // 500
  einheit: string              // Freitext, z.B. "kg", "cups", "Flasche"
  kategorie: string?           // Freitext, z.B. "Gemüse" — nullable
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
- Ein Item hat: Bezeichnung (Pflicht), Menge, Einheit, Kategorie (alle drei optional, jederzeit nachtragbar — Feedback aus dem ersten echten Einsatz: schnelles Erfassen im Laden wichtiger als Vollständigkeit beim Anlegen)
- **Kein** Fälligkeitsdatum, keine Erinnerungen, keine Unterschritte
- Items können abgehakt werden

### Abhaken-Verhalten
- Abgehakte Items rutschen sichtbar nach unten (durchgestrichen/ausgegraut), bleiben aber in der Liste sichtbar
- Items werden **nicht automatisch gelöscht**
- Erst wenn **alle** Items einer Liste abgehakt sind, "verschwindet" die Liste automatisch ins **Archiv**
- Archivierte Listen sind einsehbar und können **reaktiviert** werden
- Bei Reaktivierung (expliziter Button): alle Haken werden zurückgesetzt (Liste startet wieder "leer" abgehakt)
- Wird stattdessen ein **einzelnes** Item auf einer archivierten Liste wieder aufgehakt, reaktiviert sich die Liste automatisch — ohne die anderen Haken zurückzusetzen — und das Einkaufsdatum wird auf heute aktualisiert (faktisch ein neuer, an die alte Liste anknüpfender Einkaufsgang)

### Kategorisierung & Sortierung
- Item-Kategorie ist **Freitext**, kein festes Vokabular/kein FK — analog zu Einheiten: Kategorisierung ist subjektiv/situativ und soll individuell überschreibbar sein
- Zusätzlich eine kleine `category`-Tabelle als reine Vorschlagsliste/Autocomplete in der UI (z. B. "Gemüse", "Tiefkühl", "Getränke")
- Sortierung der Items erfolgt (da `kategorie`/`bezeichnung`/`einheit` Freitext sind) einfach per `ORDER BY` auf eines dieser Felder — keine eigene Positions-Tabelle nötig
- Die **Sortierpräferenz wird pro Liste persistiert**: `list.sortierung` ist die Default-/Owner-Präferenz, jedes Mitglied kann sie über `list_member.sortierung` individuell für sich überschreiben (z. B. Du sortierst nach Kategorie, Deine Frau nach Bezeichnung — auf derselben Liste)
- Bei Sortierung nach Kategorie: Items werden in der UI zusätzlich visuell in Abschnitte mit Kategorie-Überschriften gruppiert (abgehakte Items bleiben eine eigene, ungruppierte Gruppe ganz unten)
- Einheit/Kategorie werden in der UI als Kombi-Feld (Eingabe + Dropdown-Vorschläge aus `unit`/`category`) dargestellt — kombiniert "feste Werte schnell auswählbar" mit der Freiheit, trotzdem beliebigen Text einzugeben (wichtig für Imports)

### Einheiten
- Item-Einheit ist **Freitext**, kein festes Vokabular/kein FK — wichtig für Imports mit fremden Masssystemen (z. B. "cups", "tbsp" aus NYT Cooking) und individuelle Angaben ("Flasche", "Prise")
- Zusätzlich eine kleine, **erweiterbare** `unit`-Tabelle als reine Vorschlagsliste/Autocomplete in der UI (kein Zwang, nur Hilfestellung)
- Wird mit sinnvollen Standardwerten vorbefüllt: Stk, kg, g, l, ml, Becher, Bund, Packung, Dose, ...
- Nutzer können bei Bedarf eigene Einheiten zur Vorschlagsliste ergänzen, sind aber nie darauf beschränkt

### Anlegen einer Liste
- Bewusst reibungslos: **kein Pflicht-Name** — man will einfach eine neue Einkaufsliste machen, ohne vorher einen Namen zu überlegen
- Stattdessen ein **Einkaufsdatum** (Default: Erfassungsdatum), das jederzeit geändert werden kann (z. B. Liste im Voraus für einen späteren Einkauf anlegen)
- Ohne Namen zeigt die UI das Einkaufsdatum als Bezeichner an
- Einkaufsorte sind für später vorgemerkt, bewusst nicht Teil des ersten Wurfs

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
| REST-API | DTOs (Java Records) statt Entities über die Grenze — Entities verlassen nie die Transaktion; einheitliches Fehlerformat via @RestControllerAdvice (IllegalArgumentException → 400); `userId` aktuell als Query-/Body-Parameter (Platzhalter bis Google-OAuth2 verdrahtet ist, danach aus dem authentifizierten Principal) |
| Auth | Frontend macht den Google-Login (Google Identity Services), schickt nur das ID-Token ans Backend; Backend verifiziert es (google-api-client), provisioniert/findet den User, stellt ein eigenes, stateless JWT aus (jjwt, HMAC) für alle weiteren API-Aufrufe. Kein Server-Session-Speicher nötig (robust bei Render-Cold-Starts). Separates `test`-Spring-Profil (TestSecurityConfig, permissiv) für IT-/Unit-Tests — kein echter Google-Login beim Testen nötig |
| Lokale Entwicklung | docker-compose (Postgres + Spring Boot), 1:1 auf Render übertragbar |
| DB-Migration | Flyway (Community Edition) — plain SQL, passt zum Single-Database-Setup (Postgres), kein Multi-DB-Bedarf |
| DB-User-Trennung | Admin-User (besitzt Schema, führt Flyway-Migrationen aus) + separater Schema-/App-User (nur DML-Rechte: SELECT/INSERT/UPDATE/DELETE, keine DDL-Rechte) für die Laufzeit-Verbindung der App |
| App-User-Bootstrap | Einmaliges, plattformunabhängiges Skript (`bootstrap-app-user.sh`, Standard-`psql`-Env-Vars) statt automatischem Docker-Init — funktioniert identisch lokal und bei gemanagten DBs (Render), die kein Init-Skript unterstützen; Admin-User wird von der jeweiligen Plattform bereitgestellt (Docker-Image bzw. Render) |
| Secrets | Lokal über `.env` (nicht committed, `.env.example` als Vorlage im Repo); bei Render über Dashboard-Env-Vars — nirgends Klartext-Credentials im Code/Repo |
| Test-Kategorien | `*Test` (reine Unit-Tests, kein Docker, laufen bei `mvn test`) vs. `*IT` (Testcontainers-Integrationstests, laufen bei `mvn verify`) — Domänenlogik in Entities wird so ohne Docker-Abhängigkeit testbar |
| Repo-Struktur | Monorepo (`backend/`, `frontend/`, `docker-compose.yml` im Root); Render/Vercel nutzen Root-Directory-Einstellung |

## 5. Entwicklungsprozess (iterativ)

1. Projektgerüst → GitHub (Backend- + Frontend-Skeleton, Monorepo)
2. Docker-Datenbank (Postgres), Admin-/App-User-Trennung, testen, deployen
3. Daten-Access-Layer, getestet in zwei Teilschritten:
   - **3a** – Domain-Schema (Flyway-Migration: `list`, `item`, `category`, `unit`, `app_user`, `list_member`), separat getestet (Schema-Struktur, Constraints)
   - **3b** – JPA-Entities + Repositories passend zum in 3a angelegten Schema, getestet via Testcontainers
4. Services (Business-Logik: Archivierung, Sortierung etc.), testen
5. REST-Services (Controller, DTOs), testen, deployen
6. Client mit Mock-Backend, testen, deployen
7. Client + echtes Backend, testen, deployen

Jeder Schritt wird getestet (wo sinnvoll auch deployt), bevor der nächste beginnt.
Schema wird ausschliesslich per Flyway-Migration (up front, explizites SQL) angelegt;
Hibernate läuft strikt mit `ddl-auto: validate` und generiert nie selbst Schema.

## 6. Import von Text-Einkaufslisten

Zwei unterstützte Formate (per Strategy Pattern, explizite Auswahl durch den Nutzer):

### NYT Cooking

Rezept-Exporte ("Your Grocery List") per Copy-Paste importieren.

**Herausforderungen im Rohtext**
- Kopfzeile (Rezeptname, Portionenanzahl) ist Kontext, kein Item
- Mengenangaben oft als Unicode-Brüche (½, ¼, ⅓) statt Dezimalzahlen
- Zusätze wie "plus more" / "plus more as needed" – keine feste Menge
- Alternativen in einem Item ("or turkey or chicken, or vegan meat") – mehrdeutig
- Freitext-Hinweise wie "for serving" gehören oft noch zum vorherigen Item
- Footer mit Rezept-Link soll ignoriert werden

**Ablauf**
1. Zutaten stehen zwischen der ersten Zeile, die nur aus einem einzelnen `-` besteht, und der Zeile, die nur aus mehreren `-` besteht (`----------`) – alles davor/danach wird ignoriert
2. Menge (Unicode-Brüche → Dezimal) und Einheit werden pro Zeile erkannt, Rest wird Bezeichnung
3. Kategorie bleibt immer leer ("Unkategorisiert") – NYT liefert keine Kategorien

### Obsidian Markdown

Markdown-Checklisten (`- [ ]` / `- [x]`) aus Obsidian-Notizen importieren.

**Ablauf**
1. Das Checkbox-Muster selbst ist der Marker – jede Zeile, die nicht `- [ ]`/`- [x]` entspricht (Frontmatter, Titel, Prosa), wird ignoriert; kein Start-/End-Suchen wie bei NYT nötig
2. Markdown-Überschriften (`## Gemüse`) werden als **Kategorie** für alle nachfolgenden Items übernommen, bis zur nächsten Überschrift
3. Abhak-Status (`[x]`) wird **bewusst ignoriert** – importierte Items starten immer offen (Haken in einer wiederverwendeten Vorlage spiegeln meist den letzten Durchgang, nicht den aktuellen Stand)
4. Menge/Einheit-Erkennung nutzt dieselbe Logik wie NYT Cooking (gemeinsame Utility-Klasse, keine Duplikation)

### Gemeinsam für beide Formate

- **Vorschau vor Übernahme** – geparste Items (inkl. Kategorie) werden angezeigt und können vom Nutzer korrigiert werden, einzelne Zeilen abwählbar
- Nicht erkennbare Mengen/Einheiten fallen auf Menge 1 / Einheit "Stk" zurück, ganze Zeile wird Bezeichnung

## 7. Offene Punkte (für später)

- Polling-Intervall genau festlegen
- Einladung von Mitgliedern: per E-Mail-Adresse oder Link-Share?
- Soll es eine Undo-Funktion beim versehentlichen Abhaken/Archivieren geben?
- Reihenfolge der Items **innerhalb** einer Kategorie: frei per Drag&Drop oder alphabetisch?
- ~~Kein hartes Löschen von Listen über die App~~ — revidiert: explizites, manuelles Löschen (mit Bestätigung) ist jetzt möglich, DELETE /api/lists/{id}. Automatisches/versehentliches Löschen bleibt ausgeschlossen; ein Offline-Job für automatisches Aufräumen sehr alter archivierter Listen ist weiterhin offen für später
- **Autorisierungs-Lücke**: Authentifizierung (JWT + Google-Login) ist implementiert, aber es gibt noch keine Prüfung, ob ein User Owner/Member der Liste ist, die er per ID anspricht — jeder eingeloggte User kann aktuell jede Liste ändern, wenn er deren ID kennt. Für den privaten Rahmen (Familie) vertretbar, sollte vor breiterer Nutzung ergänzt werden
- Einkaufsorte (z. B. Migros, Bauhaus) als eigenes Feld/Konzept — bewusst nicht Teil des ersten Wurfs beim Anlegen einer Liste
- Autocomplete/"Self-Learning" für Item-Bezeichnungen (z. B. "Ap" → "Apfel" schlägt vor) basierend auf bisher verwendeten Namen — neuer Endpoint nötig (z. B. `GET /api/items/suggestions?query=`); Einheiten/Kategorien haben die Vorschlagslisten (`/api/units`, `/api/categories`) bereits im Backend, im Frontend aber noch nicht als Autocomplete genutzt
- PWA-Fähigkeit (manifest.json + Service Worker) für "Zum Home-Bildschirm hinzufügen" auf iPhone — echte native App (App Store) als deutlich größerer Schritt bewusst nicht geplant, PWA gilt als ausreichend für den privaten Rahmen
