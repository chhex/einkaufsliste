package ch.chris.einkaufsliste.web.dto;

import ch.chris.einkaufsliste.service.importer.ParsedItem;

import java.math.BigDecimal;

public record ParsedItemResponse(String bezeichnung, BigDecimal menge, String einheit, String kategorie) {
    public static ParsedItemResponse from(ParsedItem item) {
        return new ParsedItemResponse(item.bezeichnung(), item.menge(), item.einheit(), item.kategorie());
    }
}
