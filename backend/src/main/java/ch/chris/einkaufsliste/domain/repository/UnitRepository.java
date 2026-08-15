package ch.chris.einkaufsliste.domain.repository;

import ch.chris.einkaufsliste.domain.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    boolean existsByName(String name);

}
