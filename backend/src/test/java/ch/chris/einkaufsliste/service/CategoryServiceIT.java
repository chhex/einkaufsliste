package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.Category;
import ch.chris.einkaufsliste.domain.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CategoryServiceIT extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void createLegtNeueKategorieAn() {
        Category category = categoryService.create("Baby & Kind");

        assertThat(category.getId()).isNotNull();
        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }

    @Test
    void createLehntDuplikatNamenAb() {
        categoryService.create("Snacks");

        assertThatThrownBy(() -> categoryService.create("Snacks"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAendertName() {
        Category category = categoryService.create("Getraenke");

        categoryService.update(category.getId(), "Getränke");

        Category reloaded = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Getränke");
    }

}
