package ch.chris.einkaufsliste.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Konvertiert SortField (Java-Konvention: GROSSBUCHSTABEN) zu/von den
 * Kleinbuchstaben-Werten, die der DB-CHECK-Constraint erwartet
 * ('kategorie', 'bezeichnung', 'einheit').
 */
@Converter(autoApply = true)
public class SortFieldConverter implements AttributeConverter<SortField, String> {

    @Override
    public String convertToDatabaseColumn(SortField attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public SortField convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SortField.valueOf(dbData.toUpperCase());
    }

}
