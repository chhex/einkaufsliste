<script>
    import { apiFetch } from './api.js';

    let { listId, members, ownerName, onChanged } = $props();

    let open = $state(false);
    let email = $state('');
    let adding = $state(false);
    let error = $state(null);

    async function addMember(event) {
        event.preventDefault();
        const trimmed = email.trim();
        if (!trimmed) return;

        adding = true;
        error = null;
        try {
            await apiFetch(`/api/lists/${listId}/members`, {
                method: 'POST',
                body: JSON.stringify({ email: trimmed })
            });
            email = '';
            onChanged();
        } catch (e) {
            error = e.message;
        } finally {
            adding = false;
        }
    }

    async function removeMember(member) {
        if (!confirm(`${member.name} aus der Liste entfernen?`)) return;
        error = null;
        try {
            await apiFetch(`/api/lists/${listId}/members/${member.userId}`, { method: 'DELETE' });
            onChanged();
        } catch (e) {
            error = e.message;
        }
    }
</script>

{#if !open}
    <div class="mb-4 flex justify-end">
        <button class="btn btn-outline btn-sm" onclick={() => (open = true)}>
            👥 Mitglieder ({members.length + 1})
        </button>
    </div>
{:else}
    <div class="mb-4 rounded-lg border border-base-300 bg-base-200 p-3">
        <div class="mb-2 flex items-center justify-between">
            <strong>Mitglieder</strong>
            <button class="btn btn-ghost btn-xs" onclick={() => (open = false)}>✕</button>
        </div>

        {#if error}
            <p class="mb-2 text-sm text-error">{error}</p>
        {/if}

        <ul class="mb-3 flex flex-col gap-1">
            <li class="flex items-center justify-between rounded bg-base-100 px-2 py-1 text-sm">
                <span>{ownerName} <span class="text-xs text-base-content/60">(Owner)</span></span>
            </li>
            {#each members as member (member.userId)}
                <li class="flex items-center justify-between rounded bg-base-100 px-2 py-1 text-sm">
                    <span>{member.name} <span class="text-xs text-base-content/60">{member.email}</span></span>
                    <button class="opacity-50 hover:opacity-100" onclick={() => removeMember(member)}>🗑</button>
                </li>
            {/each}
        </ul>

        <form class="flex gap-2" onsubmit={addMember}>
            <input
                class="input input-bordered input-sm min-w-0 flex-1"
                type="email"
                bind:value={email}
                placeholder="E-Mail-Adresse"
                disabled={adding}
            />
            <button class="btn btn-primary btn-sm" type="submit" disabled={adding || !email.trim()}>
                + Hinzufügen
            </button>
        </form>
        <p class="mt-2 text-xs text-base-content/60">
            Die Person muss sich mindestens einmal in der App eingeloggt haben, bevor sie hinzugefügt werden kann.
        </p>
    </div>
{/if}
