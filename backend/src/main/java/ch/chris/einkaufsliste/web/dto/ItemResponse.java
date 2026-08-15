package ch.chris.einkaufsliste.web.dto;

import ch.chris.einkaufsliste.domain.entity.Item;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ItemResponse(
        Long id,
        String bezeichnung,
        BigDecimal menge,
        String einheit,
        String kategorie,
        boolean abgehakt,
        OffsetDateTime abgehaktAm
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getBezeichnung(),
                item.getMenge(),
                item.getEinheit(),
                item.getKategorie(),
                item.isAbgehakt(),
                item.getAbgehaktAm()
        );
    }
}
