package ch.chris.einkaufsliste.domain.repository;

import ch.chris.einkaufsliste.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
