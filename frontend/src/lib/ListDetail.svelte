<script>
    import { onMount } from 'svelte';
    import { apiFetch } from './api.js';
    import ImportPanel from './ImportPanel.svelte';
    import MembersPanel from './MembersPanel.svelte';

    let { listId, onBack } = $props();

    let list = $state(null);
    let loading = $state(true);
    let error = $state(null);

    let newBezeichnung = $state('');
    let newMenge = $state('');
    let newEinheit = $state('');
    let newKategorie = $state('');
    let adding = $state(false);

    let editingHeader = $state(false);
    let nameInput = $state('');
    let dateInput = $state('');
    let savingHeader = $state(false);

    // Inline-Editieren eines bestehenden Items (nicht nur bei der Import-
    // Vorschau, sondern jederzeit fuer bereits in der Liste vorhandene Items).
    let editingItemId = $state(null);
    let editBezeichnung = $state('');
    let editMenge = $state('');
    let editEinheit = $state('');
    let editKategorie = $state('');
    let savingItem = $state(false);

    function formatDate(isoDate) {
        const [year, month, day] = isoDate.split('-');
        return `${day}.${month}.${year}`;
    }

    async function load() {
        loading = true;
        error = null;
        try {
            list = await apiFetch(`/api/lists/${listId}`);
            nameInput = list.name ?? '';
            dateInput = list.einkaufsdatum;
        } catch (e) {
            error = e.message;
        } finally {
            loading = false;
        }
    }

    function sortedItems() {
        if (!list) return [];
        const key = list.sortierung.toLowerCase();
        const cmp = (a, b) =>
            (a[key] ?? '').localeCompare(b[key] ?? '', 'de', { sensitivity: 'base' });
        const offen = list.items.filter((i) => !i.abgehakt).sort(cmp);
        const abgehakt = list.items.filter((i) => i.abgehakt).sort(cmp);
        return [...offen, ...abgehakt];
    }

    async function addItem(event) {
        event.preventDefault();
        const bezeichnung = newBezeichnung.trim();
        const einheit = newEinheit.trim();
        if (!bezeichnung || !newMenge || !einheit) return;

        adding = true;
        error = null;
        try {
            await apiFetch(`/api/lists/${listId}/items`, {
                method: 'POST',
                body: JSON.stringify({
                    bezeichnung,
                    menge: Number(newMenge),
                    einheit,
                    kategorie: newKategorie.trim() || null
                })
            });
            newBezeichnung = '';
            newMenge = '';
            newEinheit = '';
            newKategorie = '';
            await load();
        } catch (e) {
            error = e.message;
        } finally {
            adding = false;
        }
    }

    async function toggleAbgehakt(item) {
        error = null;
        try {
            await apiFetch(`/api/items/${item.id}/abgehakt`, {
                method: 'PATCH',
                body: JSON.stringify({ abgehakt: !item.abgehakt })
            });
            await load();
        } catch (e) {
            error = e.message;
        }
    }

    async function deleteItem(item) {
        error = null;
        try {
            await apiFetch(`/api/items/${item.id}`, { method: 'DELETE' });
            await load();
        } catch (e) {
            error = e.message;
        }
    }

    function startEditItem(item) {
        editingItemId = item.id;
        editBezeichnung = item.bezeichnung;
        editMenge = item.menge;
        editEinheit = item.einheit;
        editKategorie = item.kategorie ?? '';
    }

    function cancelEditItem() {
        editingItemId = null;
    }

    async function saveEditItem(itemId) {
        const bezeichnung = editBezeichnung.trim();
        const einheit = editEinheit.trim();
        if (!bezeichnung || !editMenge || !einheit) return;

        savingItem = true;
        error = null;
        try {
            await apiFetch(`/api/items/${itemId}`, {
                method: 'PUT',
                body: JSON.stringify({
                    bezeichnung,
                    menge: Number(editMenge),
                    einheit,
                    kategorie: editKategorie.trim() || null
                })
            });
            editingItemId = null;
            await load();
        } catch (e) {
            error = e.message;
        } finally {
            savingItem = false;
        }
    }

    async function saveHeader() {
        savingHeader = true;
        error = null;
        try {
            await apiFetch(`/api/lists/${listId}`, {
                method: 'PATCH',
                body: JSON.stringify({
                    name: nameInput.trim() || null,
                    einkaufsdatum: dateInput
                })
            });
            editingHeader = false;
            await load();
        } catch (e) {
            error = e.message;
        } finally {
            savingHeader = false;
        }
    }

    onMount(load);
</script>

<div class="flex h-full min-h-0 flex-col">
    <button class="mb-3 self-start text-sm text-primary" onclick={onBack}>← Zurück zur Übersicht</button>

    {#if error}
        <p class="mb-2 text-sm text-error">{error}</p>
    {/if}

    {#if loading}
        <p class="text-base-content/60">Lade...</p>
    {:else if list}
        {#if editingHeader}
            <div class="mb-4 flex flex-shrink-0 flex-wrap gap-2">
                <input
                    class="input input-bordered input-sm"
                    bind:value={nameInput}
                    placeholder="Name (optional)"
                    disabled={savingHeader}
                />
                <input
                    class="input input-bordered input-sm"
                    type="date"
                    bind:value={dateInput}
                    disabled={savingHeader}
                />
                <button class="btn btn-primary btn-sm" onclick={saveHeader} disabled={savingHeader}>
                    Speichern
                </button>
            </div>
        {:else}
            <button
                class="mb-4 flex flex-shrink-0 items-baseline gap-2 self-start text-left"
                onclick={() => (editingHeader = true)}
            >
                <h2 class="text-xl font-semibold">
                    {list.name?.trim() ? list.name : formatDate(list.einkaufsdatum)}
                </h2>
                {#if list.name?.trim()}
                    <span class="text-sm text-base-content/60">{formatDate(list.einkaufsdatum)}</span>
                {/if}
                <span class="text-sm opacity-50">✏️</span>
            </button>
        {/if}

        <div class="flex-shrink-0">
            <MembersPanel
                listId={listId}
                members={list.members}
                ownerName={list.ownerName}
                onChanged={load}
            />
            <ImportPanel listId={listId} onImported={load} />
        </div>

        <form class="mb-4 grid flex-shrink-0 grid-cols-[2fr_1fr_1fr_1.3fr_auto] gap-2" onsubmit={addItem}>
            <input
                class="input input-bordered input-sm min-w-0"
                bind:value={newBezeichnung}
                placeholder="Was?"
                disabled={adding}
            />
            <input
                class="input input-bordered input-sm min-w-0"
                bind:value={newMenge}
                type="number"
                step="any"
                min="0"
                placeholder="Menge"
                disabled={adding}
            />
            <input
                class="input input-bordered input-sm min-w-0"
                bind:value={newEinheit}
                placeholder="Einheit"
                disabled={adding}
            />
            <input
                class="input input-bordered input-sm min-w-0"
                bind:value={newKategorie}
                placeholder="Kategorie (optional)"
                disabled={adding}
            />
            <button class="btn btn-primary btn-sm" type="submit" disabled={adding}>+</button>
        </form>

        {#if list.items.length === 0}
            <p class="text-base-content/60">Noch keine Items – oben hinzufügen.</p>
        {:else}
            <div class="min-h-0 flex-1 overflow-y-auto [-webkit-overflow-scrolling:touch]">
                <ul class="flex flex-col">
                    {#each sortedItems() as item (item.id)}
                        <li class="flex items-center gap-2 border-b border-base-200 py-2">
                            {#if editingItemId === item.id}
                                <div class="grid w-full grid-cols-[2fr_1fr_1fr_1.3fr_auto_auto] items-center gap-1">
                                    <input
                                        class="input input-bordered input-sm min-w-0"
                                        bind:value={editBezeichnung}
                                        placeholder="Was?"
                                        disabled={savingItem}
                                    />
                                    <input
                                        class="input input-bordered input-sm min-w-0"
                                        bind:value={editMenge}
                                        type="number"
                                        step="any"
                                        min="0"
                                        placeholder="Menge"
                                        disabled={savingItem}
                                    />
                                    <input
                                        class="input input-bordered input-sm min-w-0"
                                        bind:value={editEinheit}
                                        placeholder="Einheit"
                                        disabled={savingItem}
                                    />
                                    <input
                                        class="input input-bordered input-sm min-w-0"
                                        bind:value={editKategorie}
                                        placeholder="Kategorie (optional)"
                                        disabled={savingItem}
                                    />
                                    <button
                                        class="btn btn-primary btn-sm"
                                        onclick={() => saveEditItem(item.id)}
                                        disabled={savingItem}
                                    >
                                        ✓
                                    </button>
                                    <button
                                        class="btn btn-outline btn-sm"
                                        onclick={cancelEditItem}
                                        disabled={savingItem}
                                    >
                                        ✕
                                    </button>
                                </div>
                            {:else}
                                <input
                                    class="checkbox checkbox-sm"
                                    type="checkbox"
                                    checked={item.abgehakt}
                                    onchange={() => toggleAbgehakt(item)}
                                />
                                <button
                                    class="min-w-0 flex-1 truncate text-left"
                                    class:line-through={item.abgehakt}
                                    class:text-base-content={!item.abgehakt}
                                    class:opacity-50={item.abgehakt}
                                    onclick={() => startEditItem(item)}
                                >
                                    {item.bezeichnung} — {item.menge} {item.einheit}
                                    {#if item.kategorie}
                                        <span class="text-sm text-base-content/60">({item.kategorie})</span>
                                    {/if}
                                </button>
                                <button
                                    class="shrink-0 px-1 opacity-50 hover:opacity-100"
                                    onclick={() => deleteItem(item)}
                                >
                                    🗑
                                </button>
                            {/if}
                        </li>
                    {/each}
                </ul>
            </div>
        {/if}
    {/if}
</div>
