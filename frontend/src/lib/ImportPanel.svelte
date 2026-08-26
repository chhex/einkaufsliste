<script>
    import { apiFetch } from './api.js';

    let { listId, onImported } = $props();

    const SOURCES = [
        { value: 'NYT_COOKING', label: 'NYT Cooking' },
        { value: 'OBSIDIAN_MARKDOWN', label: 'Obsidian Markdown' }
    ];

    let open = $state(false);
    let source = $state('NYT_COOKING');
    let rawText = $state('');
    let parsing = $state(false);
    let importing = $state(false);
    let error = $state(null);
    let parsedItems = $state([]); // [{bezeichnung, menge, einheit, kategorie, include}]

    async function parse() {
        if (!rawText.trim()) return;
        parsing = true;
        error = null;
        try {
            const result = await apiFetch(`/api/import/${source}`, {
                method: 'POST',
                body: JSON.stringify({ rawText })
            });
            parsedItems = result.map((item) => ({ ...item, kategorie: item.kategorie ?? '', include: true }));
            if (parsedItems.length === 0) {
                error = 'Keine Einträge erkannt - Format passt evtl. nicht (siehe Anleitung unten).';
            }
        } catch (e) {
            error = e.message;
        } finally {
            parsing = false;
        }
    }

    async function importSelected() {
        const toImport = parsedItems.filter((i) => i.include);
        if (toImport.length === 0) return;

        importing = true;
        error = null;
        try {
            for (const item of toImport) {
                await apiFetch(`/api/lists/${listId}/items`, {
                    method: 'POST',
                    body: JSON.stringify({
                        bezeichnung: item.bezeichnung,
                        menge: Number(item.menge),
                        einheit: item.einheit,
                        kategorie: item.kategorie?.trim() || null
                    })
                });
            }
            close();
            onImported();
        } catch (e) {
            error = e.message;
        } finally {
            importing = false;
        }
    }

    function close() {
        open = false;
        rawText = '';
        parsedItems = [];
        error = null;
    }

    function placeholderFor(src) {
        return src === 'OBSIDIAN_MARKDOWN'
            ? '## Gemüse\n- [ ] Tomaten\n- [ ] 2 Zwiebeln\n\n## Milchprodukte\n- [ ] 500g Butter'
            : 'Your Grocery List\n1 Recipe\n\nRezeptname...\n-\n2 cups Mehl\n...';
    }

    function hintFor(src) {
        return src === 'OBSIDIAN_MARKDOWN'
            ? 'Checkliste aus Obsidian hier einfügen. Überschriften (## Gemüse) werden als Kategorie übernommen, bereits abgehakte Punkte werden trotzdem als offen importiert.'
            : 'Den "Your Grocery List"-Text von cooking.nytimes.com hier einfügen (inkl. Rezeptname/Portionen oben und Link unten – wird automatisch erkannt und ignoriert).';
    }
</script>

{#if !open}
    <div class="mb-4 flex justify-end">
        <button class="btn btn-outline btn-sm" onclick={() => (open = true)}>📋 Liste importieren</button>
    </div>
{:else}
    <div class="mb-4 rounded-lg border border-base-300 bg-base-200 p-3">
        <div class="mb-2 flex items-center justify-between">
            <strong>Import</strong>
            <button class="btn btn-ghost btn-xs" onclick={close}>✕</button>
        </div>

        {#if parsedItems.length === 0}
            <div class="mb-2 flex gap-2 text-sm">
                {#each SOURCES as s}
                    <label class="flex cursor-pointer items-center gap-1 rounded border border-base-300 px-2 py-1"
                           class:bg-primary={source === s.value}
                           class:text-primary-content={source === s.value}>
                        <input class="radio radio-xs" type="radio" bind:group={source} value={s.value} />
                        {s.label}
                    </label>
                {/each}
            </div>
        {/if}

        {#if error}
            <p class="mb-2 text-sm text-error">{error}</p>
        {/if}

        {#if parsedItems.length === 0}
            <p class="mb-2 text-xs text-base-content/60">{hintFor(source)}</p>
            <textarea
                class="textarea textarea-bordered mb-2 w-full font-mono text-xs"
                bind:value={rawText}
                rows="10"
                placeholder={placeholderFor(source)}
                disabled={parsing}
            ></textarea>
            <button class="btn btn-primary btn-sm" onclick={parse} disabled={parsing || !rawText.trim()}>
                {parsing ? 'Analysiere...' : 'Vorschau anzeigen'}
            </button>
        {:else}
            <p class="mb-2 text-xs text-base-content/60">
                {parsedItems.filter((i) => i.include).length} von {parsedItems.length} Items ausgewählt
                – Werte vor dem Übernehmen korrigierbar.
            </p>
            <ul class="mb-3 flex max-h-72 flex-col gap-1 overflow-y-auto">
                {#each parsedItems as item}
                    <li class="flex items-center gap-1">
                        <input class="checkbox checkbox-sm" type="checkbox" bind:checked={item.include} />
                        <input
                            class="input input-bordered input-sm min-w-0 flex-[2]"
                            bind:value={item.bezeichnung}
                            placeholder="Bezeichnung"
                        />
                        <input
                            class="input input-bordered input-sm min-w-0 flex-1"
                            type="number"
                            step="any"
                            bind:value={item.menge}
                        />
                        <input
                            class="input input-bordered input-sm min-w-0 flex-1"
                            bind:value={item.einheit}
                            placeholder="Einheit"
                        />
                        <input
                            class="input input-bordered input-sm min-w-0 flex-[1.3]"
                            bind:value={item.kategorie}
                            placeholder="Kategorie"
                        />
                    </li>
                {/each}
            </ul>
            <div class="flex justify-between gap-2">
                <button class="btn btn-outline btn-sm" onclick={() => (parsedItems = [])}>← Zurück</button>
                <button class="btn btn-primary btn-sm" onclick={importSelected} disabled={importing}>
                    {importing
                        ? 'Übernehme...'
                        : `${parsedItems.filter((i) => i.include).length} Items übernehmen`}
                </button>
            </div>
        {/if}
    </div>
{/if}
