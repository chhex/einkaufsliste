import { API_BASE_URL } from './config.js';

// Svelte-5-Runes ausserhalb einer Komponente (.svelte.js-Datei) - so laesst
// sich reaktiver State modulweit teilen, ohne einen Context-Provider o.ae.
// aufzusetzen. localStorage haelt den Login ueber Reloads hinweg (fuer eine
// private Familien-App ein vertretbarer Trade-off - siehe Anforderungsdoku,
// Abschnitt "Auth", zur Autorisierungs-Einschraenkung generell).
let token = $state(localStorage.getItem('jwt'));
let user = $state(JSON.parse(localStorage.getItem('user') ?? 'null'));

export const auth = {
    get token() {
        return token;
    },
    get user() {
        return user;
    },
    get isLoggedIn() {
        return token !== null;
    },

    /**
     * Nimmt ein echtes Google-ID-Token (vom Google-Login-Widget) entgegen,
     * schickt es ans Backend (POST /api/auth/google), das es verifiziert
     * und unser eigenes JWT ausstellt.
     */
    async loginWithGoogleIdToken(idToken) {
        const res = await fetch(`${API_BASE_URL}/api/auth/google`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ idToken })
        });

        if (!res.ok) {
            throw new Error('Login fehlgeschlagen (Google-Token ungueltig oder Backend nicht erreichbar)');
        }

        const data = await res.json();
        token = data.token;
        user = data.user;
        localStorage.setItem('jwt', token);
        localStorage.setItem('user', JSON.stringify(user));
    },

    logout() {
        token = null;
        user = null;
        localStorage.removeItem('jwt');
        localStorage.removeItem('user');
    }
};
