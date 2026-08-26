<script>
    import { onMount } from 'svelte';
    import { auth } from './auth.svelte.js';
    import { GOOGLE_CLIENT_ID } from './config.js';

    let container;
    let error = $state(null);

    onMount(() => {
        if (!GOOGLE_CLIENT_ID) {
            error = 'VITE_GOOGLE_CLIENT_ID fehlt - siehe frontend/.env.example';
            return;
        }

        // Google Identity Services laedt sich selbst nach; falls schon von
        // einer frueheren Komponente geladen, direkt initialisieren.
        if (window.google?.accounts?.id) {
            initGoogleButton();
            return;
        }

        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.onload = initGoogleButton;
        script.onerror = () => {
            error = 'Google Identity Services konnte nicht geladen werden';
        };
        document.head.appendChild(script);
    });

    function initGoogleButton() {
        window.google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: handleCredential
        });
        window.google.accounts.id.renderButton(container, {
            theme: 'outline',
            size: 'large'
        });
    }

    async function handleCredential(response) {
        error = null;
        try {
            await auth.loginWithGoogleIdToken(response.credential);
        } catch (e) {
            error = e.message;
        }
    }
</script>

<div bind:this={container}></div>
{#if error}
    <p class="mt-2 text-sm text-error">{error}</p>
{/if}
