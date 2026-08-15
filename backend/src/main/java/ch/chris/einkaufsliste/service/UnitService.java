package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.entity.Unit;
import ch.chris.einkaufsliste.domain.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD fuer die Einheiten-Vorschlagsliste (kein FK von Item aus - siehe
 * Kommentar in Unit.java). Bewusst simpel, kein Aggregat.
 */
@Service
public class UnitService {

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Transactional
    public Unit create(String name, String abbreviation) {
        if (unitRepository.existsByName(name)) {
            throw new IllegalArgumentException("Einheit '" + name + "' existiert bereits");
        }
        return unitRepository.save(new Unit(name, abbreviation));
    }

    @Transactional
    public void update(Long id, String name, String abbreviation) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Einheit nicht gefunden: " + id));
        unit.setName(name);
        unit.setAbbreviation(abbreviation);
    }

}
