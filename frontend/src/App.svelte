<script>
  // Provisorischer Smoke-Test fuer Schritt 1 (Projektgeruest).
  // Wird in Schritt 6 durch die echte UI mit Mock-Backend ersetzt.
  let status = $state('nicht geprueft');

  async function checkBackend() {
    status = 'prüfe...';
    try {
      const res = await fetch('/api/ping');
      const data = await res.json();
      status = `Backend antwortet: ${data.status}`;
    } catch (e) {
      status = 'Backend nicht erreichbar';
    }
  }
</script>

<main>
  <h1>🛒 Einkaufsliste</h1>
  <p>Projektgerüst steht. Status: {status}</p>
  <button onclick={checkBackend}>Backend-Verbindung testen</button>
</main>

<style>
  main {
    font-family: system-ui, sans-serif;
    max-width: 480px;
    margin: 4rem auto;
    text-align: center;
  }
  button {
    padding: 0.5rem 1rem;
    margin-top: 1rem;
    cursor: pointer;
  }
</style>
