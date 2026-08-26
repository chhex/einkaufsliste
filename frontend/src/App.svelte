<script>
    import { auth } from './lib/auth.svelte.js';
    import GoogleLoginButton from './lib/GoogleLoginButton.svelte';
    import ListOverview from './lib/ListOverview.svelte';
    import ListDetail from './lib/ListDetail.svelte';
    import ThemeSwitcher from './lib/ThemeSwitcher.svelte';

    let selectedListId = $state(null);
</script>

<main class="mx-auto flex h-dvh max-w-md flex-col bg-base-100 p-4 text-base-content">
    <div class="mb-4 flex flex-shrink-0 items-center justify-between">
        <h1 class="text-2xl font-bold">🛒 Einkaufslisten</h1>
        <ThemeSwitcher />
    </div>

    {#if auth.isLoggedIn}
        <div class="mb-4 flex flex-shrink-0 items-center justify-between text-sm">
            <span>Eingeloggt als <strong>{auth.user.name}</strong></span>
            <button class="btn btn-ghost btn-sm" onclick={() => auth.logout()}>Logout</button>
        </div>

        <div class="flex min-h-0 flex-1 flex-col overflow-y-auto">
            {#if selectedListId}
                <ListDetail listId={selectedListId} onBack={() => (selectedListId = null)} />
            {:else}
                <ListOverview onSelectList={(id) => (selectedListId = id)} />
            {/if}
        </div>
    {:else}
        <p class="mb-3">Bitte einloggen:</p>
        <GoogleLoginButton />
    {/if}
</main>
