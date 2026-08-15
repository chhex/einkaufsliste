package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.entity.Category;
import ch.chris.einkaufsliste.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD fuer die Kategorien-Vorschlagsliste (kein FK von Item aus - siehe
 * Kommentar in Category.java). Bewusst simpel, kein Aggregat.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Kategorie '" + name + "' existiert bereits");
        }
        return categoryRepository.save(new Category(name));
    }

    @Transactional
    public void update(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategorie nicht gefunden: " + id));
        category.setName(name);
    }

}
