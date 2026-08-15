package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.Category;
import ch.chris.einkaufsliste.service.CategoryService;
import ch.chris.einkaufsliste.web.dto.CategoryRequest;
import ch.chris.einkaufsliste.web.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.list().stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.create(request.name());
        return ResponseEntity.created(URI.create("/api/categories/" + category.getId()))
                .body(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        categoryService.update(id, request.name());
        return ResponseEntity.noContent().build();
    }

}
