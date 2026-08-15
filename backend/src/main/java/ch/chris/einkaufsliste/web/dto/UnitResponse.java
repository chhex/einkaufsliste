package ch.chris.einkaufsliste.web.dto;

import ch.chris.einkaufsliste.domain.entity.Unit;

public record UnitResponse(Long id, String name, String abbreviation) {
    public static UnitResponse from(Unit unit) {
        return new UnitResponse(unit.getId(), unit.getName(), unit.getAbbreviation());
    }
}
