package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.Unit;
import ch.chris.einkaufsliste.domain.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UnitServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UnitService unitService;

    @Autowired
    private UnitRepository unitRepository;

    @Test
    void createLegtNeueEinheitAn() {
        Unit unit = unitService.create("Prise", "Pr");

        assertThat(unit.getId()).isNotNull();
        assertThat(unitRepository.findById(unit.getId())).isPresent();
    }

    @Test
    void createLehntDuplikatNamenAb() {
        unitService.create("Cup", "Cup");

        assertThatThrownBy(() -> unitService.create("Cup", "Cup2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAendertNameUndAbkuerzung() {
        Unit unit = unitService.create("Karton", "Ktn"); // bewusst NICHT "Bund" - das ist bereits Seed-Daten aus V3

        unitService.update(unit.getId(), "Kartons", "Ktns");

        Unit reloaded = unitRepository.findById(unit.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Kartons");
        assertThat(reloaded.getAbbreviation()).isEqualTo("Ktns");
    }

}
