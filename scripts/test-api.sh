#!/bin/bash
# Testet die wichtigsten End-to-End-Use-Cases gegen eine laufende Instanz.
# Nutzung: ./test-api.sh [BASE_URL]
# Beispiel lokal:  ./test-api.sh http://localhost:8080
# Beispiel Render: ./test-api.sh https://einkaufsliste-gnrc.onrender.com
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
echo "=== Teste gegen: $BASE_URL ==="

jq_get() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)"; }

echo -e "\n--- 1. Ping (oeffentlich, kein Token noetig) ---"
curl -sf "$BASE_URL/api/ping" | tee /dev/stderr | jq_get "['status']" > /dev/null

echo -e "\n--- 2. Dev-Login: Owner (liefert JWT, ersetzt spaeter echten Google-Login) ---"
OWNER_AUTH=$(curl -sf -X POST "$BASE_URL/api/dev/login" \
    -H "Content-Type: application/json" \
    -d '{"googleId":"test-owner-'"$(date +%s)"'","email":"owner@test.local","name":"Test Owner"}')
OWNER_TOKEN=$(echo "$OWNER_AUTH" | jq_get "['token']")
OWNER_ID=$(echo "$OWNER_AUTH" | jq_get "['user']['id']")
echo "Owner-ID: $OWNER_ID"

echo -e "\n--- 3. Dev-Login: Partner (fuer Member-Test) ---"
PARTNER_AUTH=$(curl -sf -X POST "$BASE_URL/api/dev/login" \
    -H "Content-Type: application/json" \
    -d '{"googleId":"test-partner-'"$(date +%s)"'","email":"partner@test.local","name":"Test Partner"}')
PARTNER_ID=$(echo "$PARTNER_AUTH" | jq_get "['user']['id']")
echo "Partner-ID: $PARTNER_ID"

AUTH_HEADER="Authorization: Bearer $OWNER_TOKEN"

echo -e "\n--- 4. OHNE Token: sollte 401/403 liefern ---"
curl -s -o /dev/stderr -w "HTTP %{http_code}\n" -X GET "$BASE_URL/api/lists?userId=$OWNER_ID"

echo -e "\n--- 5. Liste erstellen (mit Token, Owner kommt jetzt aus dem Token) ---"
LIST=$(curl -sf -X POST "$BASE_URL/api/lists" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"name":"Migros Test"}')
echo "$LIST"
LIST_ID=$(echo "$LIST" | jq_get "['id']")

echo -e "\n--- 6. Liste abrufen (leer) ---"
curl -sf "$BASE_URL/api/lists/$LIST_ID" -H "$AUTH_HEADER" | tee /dev/stderr > /dev/null

echo -e "\n--- 7. Item hinzufuegen ---"
ITEM=$(curl -sf -X POST "$BASE_URL/api/lists/$LIST_ID/items" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"bezeichnung":"Tomaten","menge":500,"einheit":"g","kategorie":"Gemüse"}')
echo "$ITEM"
ITEM_ID=$(echo "$ITEM" | jq_get "['id']")

echo -e "\n--- 8. Zweites Item hinzufuegen ---"
ITEM2=$(curl -sf -X POST "$BASE_URL/api/lists/$LIST_ID/items" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"bezeichnung":"Brot","menge":1,"einheit":"Stk"}')
ITEM2_ID=$(echo "$ITEM2" | jq_get "['id']")

echo -e "\n--- 9. Mitglied hinzufuegen ---"
curl -sf -X POST "$BASE_URL/api/lists/$LIST_ID/members" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"userId":'"$PARTNER_ID"'}' | tee /dev/stderr > /dev/null

echo -e "\n--- 10. Erwarteter Fehlerfall: leerer Name (sollte 400 liefern) ---"
curl -s -o /dev/stderr -w "HTTP %{http_code}\n" -X POST "$BASE_URL/api/lists" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"name":""}'

echo -e "\n--- 11. Item abhaken ---"
curl -sf -X PATCH "$BASE_URL/api/items/$ITEM_ID/abgehakt" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"abgehakt": true}' -w "HTTP %{http_code}\n"

echo -e "\n--- 12. Zweites Item abhaken -> sollte Liste automatisch archivieren ---"
curl -sf -X PATCH "$BASE_URL/api/items/$ITEM2_ID/abgehakt" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"abgehakt": true}' -w "HTTP %{http_code}\n"

echo -e "\n--- 13. Liste sollte jetzt ARCHIVIERT sein ---"
curl -sf "$BASE_URL/api/lists/$LIST_ID" -H "$AUTH_HEADER" | tee /dev/stderr | jq_get "['status']"

echo -e "\n--- 14. Reaktivieren ---"
curl -sf -X POST "$BASE_URL/api/lists/$LIST_ID/reactivate" -H "$AUTH_HEADER" -w "HTTP %{http_code}\n"

echo -e "\n--- 15. Liste sollte wieder AKTIV sein, Haken zurueckgesetzt ---"
curl -sf "$BASE_URL/api/lists/$LIST_ID" -H "$AUTH_HEADER" | tee /dev/stderr > /dev/null

echo -e "\n--- 16. Einheiten-Vorschlagsliste abrufen ---"
curl -sf "$BASE_URL/api/units" -H "$AUTH_HEADER" | tee /dev/stderr > /dev/null

echo -e "\n--- 17. Kategorien-Vorschlagsliste abrufen ---"
curl -sf "$BASE_URL/api/categories" -H "$AUTH_HEADER" | tee /dev/stderr > /dev/null

echo -e "\n--- 18. Import-Vorschau (NYT Cooking) ---"
curl -sf -X POST "$BASE_URL/api/import/NYT_COOKING" \
    -H "Content-Type: application/json" -H "$AUTH_HEADER" \
    -d '{"rawText":"Titel\n1 Portion\n-\n2 cups Mehl\n1 lime, halved\n----------\nFooter"}' \
    | tee /dev/stderr > /dev/null

echo -e "\n--- 19. Alle Listen des Owners ---"
curl -sf "$BASE_URL/api/lists?userId=$OWNER_ID" -H "$AUTH_HEADER" | tee /dev/stderr > /dev/null

echo -e "\n=== Fertig ==="
