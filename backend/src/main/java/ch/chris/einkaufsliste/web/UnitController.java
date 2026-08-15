package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.Unit;
import ch.chris.einkaufsliste.service.UnitService;
import ch.chris.einkaufsliste.web.dto.UnitRequest;
import ch.chris.einkaufsliste.web.dto.UnitResponse;
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
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public List<UnitResponse> list() {
        return unitService.list().stream().map(UnitResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<UnitResponse> create(@Valid @RequestBody UnitRequest request) {
        Unit unit = unitService.create(request.name(), request.abbreviation());
        return ResponseEntity.created(URI.create("/api/units/" + unit.getId()))
                .body(UnitResponse.from(unit));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody UnitRequest request) {
        unitService.update(id, request.name(), request.abbreviation());
        return ResponseEntity.noContent().build();
    }

}
