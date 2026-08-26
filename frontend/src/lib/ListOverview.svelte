<script>
    import { onMount } from 'svelte';
    import { apiFetch } from './api.js';

    let { onSelectList } = $props();

    let lists = $state([]);
    let loading = $state(true);
    let error = $state(null);
    let creating = $state(false);

    async function loadLists() {
        loading = true;
        error = null;
        try {
            lists = await apiFetch('/api/lists');
        } catch (e) {
            error = e.message;
        } finally {
            loading = false;
        }
    }

    /**
     * Bewusst reibungslos: ein Klick, keine Pflichtfelder. Name/Einkaufsdatum
     * lassen sich danach in der Detail-Ansicht noch anpassen.
     */
    async function createList() {
        creating = true;
        error = null;
        try {
            const newList = await apiFetch('/api/lists', {
                method: 'POST',
                body: JSON.stringify({})
            });
            await loadLists();
            onSelectList(newList.id);
        } catch (e) {
            error = e.message;
        } finally {
            creating = false;
        }
    }

    function displayLabel(list) {
        return list.name?.trim() ? list.name : formatDate(list.einkaufsdatum);
    }

    function formatDate(isoDate) {
        const [year, month, day] = isoDate.split('-');
        return `${day}.${month}.${year}`;
    }

    // Aktive zuerst, jeweils neuestes Einkaufsdatum oben.
    function byDateDesc(a, b) {
        return b.einkaufsdatum.localeCompare(a.einkaufsdatum);
    }

    function activeLists() {
        return lists.filter((l) => l.status !== 'ARCHIVIERT').sort(byDateDesc);
    }

    function archivedLists() {
        return lists.filter((l) => l.status === 'ARCHIVIERT').sort(byDateDesc);
    }

    /**
     * Hartes Loeschen ist unumkehrbar (im Gegensatz zu Archivieren) - daher
     * bewusst mit Bestaetigung, statt direkt zu loeschen.
     */
    async function deleteList(list, event) {
        event.stopPropagation(); // nicht auch onSelectList ausloesen
        const label = displayLabel(list);
        if (!confirm(`"${label}" endgültig löschen? Das kann nicht rückgängig gemacht werden.`)) {
            return;
        }
        error = null;
        try {
            await apiFetch(`/api/lists/${list.id}`, { method: 'DELETE' });
            await loadLists();
        } catch (e) {
            error = e.message;
        }
    }

    onMount(loadLists);
</script>

{#snippet listRow(list, archived)}
    <li>
        <button
            class="flex w-full items-center justify-between rounded-lg border border-base-300 bg-base-200 px-4 py-3 text-left hover:bg-base-300"
            class:opacity-50={archived}
            onclick={() => onSelectList(list.id)}
        >
            <span class="flex flex-col">
                <span class="font-medium">{displayLabel(list)}</span>
                {#if list.name?.trim()}
                    <span class="text-xs text-base-content/60">{formatDate(list.einkaufsdatum)}</span>
                {/if}
            </span>
            <span
                class="shrink-0 px-1 opacity-40 hover:opacity-100"
                role="button"
                tabindex="0"
                onclick={(e) => deleteList(list, e)}
                onkeydown={(e) => e.key === 'Enter' && deleteList(list, e)}
            >
                🗑
            </span>
        </button>
    </li>
{/snippet}

<div class="flex flex-col overflow-y-auto">
    <div class="mb-4 flex justify-end">
        <button class="btn btn-outline btn-sm" onclick={createList} disabled={creating}>
            + Neue Liste
        </button>
    </div>

    {#if error}
        <p class="mb-3 text-sm text-error">{error}</p>
    {/if}

    {#if loading}
        <p class="text-base-content/60">Lade Listen...</p>
    {:else if lists.length === 0}
        <p class="text-base-content/60">Noch keine Listen – leg oben Deine erste an.</p>
    {:else}
        {#if activeLists().length > 0}
            <ul class="flex flex-col gap-2">
                {#each activeLists() as list (list.id)}
                    {@render listRow(list, false)}
                {/each}
            </ul>
        {/if}

        {#if archivedLists().length > 0}
            <div class="divider text-xs uppercase text-base-content/50">Archiviert</div>
            <ul class="flex flex-col gap-2">
                {#each archivedLists() as list (list.id)}
                    {@render listRow(list, true)}
                {/each}
            </ul>
        {/if}
    {/if}
</div>
