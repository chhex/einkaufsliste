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

    // $effect statt manueller applyTheme()-Funktion: laeuft reaktiv einmal
    // beim Mount UND bei jeder Aenderung von `current` automatisch erneut.
    // Behebt Svelte 5s "state_referenced_locally"-Warnung endgueltig - die
    // vorherige Variante (current in normalem Top-Level-Script-Code lesen,
    // auch ausserhalb eines Funktionsaufrufs) war IMMER noch kein reaktiver
    // Kontext und loeste die Warnung weiterhin aus.
    $effect(() => {
        document.documentElement.setAttribute('data-theme', current);
        localStorage.setItem('theme', current);
    });
</script>

<select
    class="select select-bordered select-sm w-auto"
    value={current}
    onchange={(e) => (current = e.target.value)}
    aria-label="Theme wählen"
>
    {#each THEMES as theme}
        <option value={theme.value}>{theme.label}</option>
    {/each}
</select>
