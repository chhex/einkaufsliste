package ch.chris.einkaufsliste.domain.enums;

/**
 * Nach welchem Freitext-Feld eines Items sortiert wird.
 * DB-seitig als Kleinbuchstaben gespeichert (siehe CHECK-Constraint in
 * V2__create_domain_schema.sql) - Konvertierung uebernimmt SortFieldConverter.
 */
public enum SortField {
    KATEGORIE,
    BEZEICHNUNG,
    EINHEIT
}
