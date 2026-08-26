import { API_BASE_URL } from './config.js';
import { auth } from './auth.svelte.js';

/**
 * Zentraler fetch-Wrapper fuer alle API-Aufrufe (ausser dem Login selbst,
 * der laeuft direkt ueber auth.svelte.js, weil er VOR einem Token
 * passiert). Haengt automatisch "Authorization: Bearer <token>" an, wirft
 * bei 401 automatisch einen Logout aus (Token abgelaufen/ungueltig) und
 * gibt bei Fehlern die Backend-Fehlermeldung weiter (siehe
 * GlobalExceptionHandler, ErrorResponse.message im Backend).
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

    if (res.status === 401) {
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
