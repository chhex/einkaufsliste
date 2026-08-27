<script>
    const THEMES = [
        { value: 'light', label: '☀️ Light' },
        { value: 'dark', label: '🌙 Dark' },
        { value: 'cupcake', label: '🧁 Cupcake' },
        { value: 'forest', label: '🌲 Forest' },
        { value: 'dracula', label: '🧛 Dracula' },
        { value: 'autumn', label: '🍂 Autumn' },
        { value: 'corporate', label: '💼 Corporate' },
        { value: 'synthwave', label: ' neon Synthwave' },
        { value: 'cyberpunk', label: '🤖 Cyberpunk' },
        { value: 'valentine', label: '💕 Valentine' },
        { value: 'halloween', label: '🎃 Halloween' },
        { value: 'garden', label: '🌷 Garden' },
        { value: 'bumblebee', label: '🐝 Bumblebee' },
        { value: 'emerald', label: '💚 Emerald' },
        { value: 'retro', label: '🕹️ Retro' },
        { value: 'lo-fi', label: ' chill Lo-Fi' },
        { value: 'pastel', label: '🎨 Pastel' },
        { value: 'fantasy', label: '🧙 Fantasy' },
        { value: 'wireframe', label: '✏️ Wireframe' },
        { value: 'black', label: '🖤 Black' },
        { value: 'luxury', label: '💎 Luxury' }
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
