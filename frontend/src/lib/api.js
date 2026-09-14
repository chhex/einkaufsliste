import { API_BASE_URL } from './config.js';
import { auth } from './auth.svelte.js';

/**
 * Zentraler fetch-Wrapper fuer alle API-Aufrufe (ausser dem Login selbst,
 * der laeuft direkt ueber auth.svelte.js, weil er VOR einem Token
 * passiert). Haengt automatisch "Authorization: Bearer <token>" an.
 * <p>
 * 401/403 werden anhand des Response-Bodies unterschieden: ohne JSON-Body
 * (Spring Securitys eigene Ablehnung bei fehlendem/ungueltigem/abgelaufenem
 * Token) -> automatischer Logout. Mit JSON-Body (unsere eigene
 * ForbiddenException, z.B. fremde Liste) -> User bleibt eingeloggt, nur die
 * Fehlermeldung wird weitergereicht (siehe GlobalExceptionHandler im
 * Backend).
 */
export async function apiFetch(path, options = {}) {
    const headers = { ...options.headers };

    if (auth.token) {
        headers['Authorization'] = `Bearer ${auth.token}`;
    }
    if (options.body && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }

    const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

    if (res.status === 401 || res.status === 403) {
        // Zwei grundverschiedene Faelle liefern beide 403 (!):
        // - Spring Security selbst lehnt ab (kein/ungueltiges/abgelaufenes Token)
        //   -> IMMER ohne JSON-Body (siehe Http403ForbiddenEntryPoint im Backend)
        //   -> echter Session-Fall, sauber ausloggen
        // - Unsere eigene ForbiddenException (User eingeloggt, aber nicht
        //   Owner/Mitglied DIESER Liste) -> IMMER mit JSON-Body (message)
        //   -> User bleibt eingeloggt, nur diese eine Aktion war nicht erlaubt
        let message = null;
        try {
            const body = await res.json();
            message = body.message;
        } catch {
            // kein JSON-Body - typisch fuer Spring Securitys eigene Ablehnung
        }

        if (message) {
            throw new Error(message);
        }

        auth.logout();
        throw new Error('Sitzung abgelaufen - bitte erneut einloggen');
    }

    if (!res.ok) {
        let message = `Fehler (HTTP ${res.status})`;
        try {
            const body = await res.json();
            if (body.message) {
                message = body.message;
            }
        } catch {
            // Antwort war kein JSON - Default-Message bleibt stehen
        }
        throw new Error(message);
    }

    if (res.status === 204) {
        return null;
    }

    return res.json();
}
