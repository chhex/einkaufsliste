package ch.chris.einkaufsliste.service.importer;

/**
 * Bekannte Import-Formate. Explizite Auswahl durch den Nutzer statt
 * Auto-Erkennung (supports()-Heuristik) - deterministischer, kein Rateflow
 * bei aehnlich aussehenden Formaten.
 */
public enum ImportSource {
    NYT_COOKING
    // spaeter z.B. MIGROS_APP, CSV, ...
}
