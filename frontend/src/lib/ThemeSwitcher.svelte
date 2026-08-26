<script>
    const THEMES = [
        { value: 'light', label: '☀️ Light' },
        { value: 'dark', label: '🌙 Dark' },
        { value: 'cupcake', label: '🧁 Cupcake' },
        { value: 'forest', label: '🌲 Forest' },
        { value: 'dracula', label: '🧛 Dracula' },
        { value: 'autumn', label: '🍂 Autumn' },
        { value: 'corporate', label: '💼 Corporate' }
    ];

    let current = $state(localStorage.getItem('theme') ?? 'light');

    function applyTheme(theme) {
        current = theme;
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
    }

    // Initiales Theme direkt anwenden (ohne ueber applyTheme(current) zu
    // gehen - Svelte 5 warnt sonst, weil eine $state-Variable als
    // Funktionsargument zur Modul-Initialisierungszeit nur ihren aktuellen
    // Wert einmalig einfriert statt reaktiv zu bleiben; hier aber ohnehin
    // beabsichtigt, da es nur einmal beim Laden passieren soll).
    document.documentElement.setAttribute('data-theme', current);
</script>

<select
    class="select select-bordered select-sm w-auto"
    value={current}
    onchange={(e) => applyTheme(e.target.value)}
    aria-label="Theme wählen"
>
    {#each THEMES as theme}
        <option value={theme.value}>{theme.label}</option>
    {/each}
</select>
