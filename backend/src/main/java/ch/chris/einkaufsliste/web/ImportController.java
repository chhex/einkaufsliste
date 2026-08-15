package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.service.importer.ImportService;
import ch.chris.einkaufsliste.service.importer.ImportSource;
import ch.chris.einkaufsliste.web.dto.ImportRequest;
import ch.chris.einkaufsliste.web.dto.ParsedItemResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Liefert nur die Vorschau (ParsedItem) - die eigentliche Uebernahme in eine
 * Liste passiert erst ueber POST /api/lists/{listId}/items (ItemController),
 * nachdem der Nutzer die Vorschau bestaetigt/korrigiert hat (Anforderung 6).
 */
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/{source}")
    public List<ParsedItemResponse> parse(@PathVariable ImportSource source, @Valid @RequestBody ImportRequest request) {
        return importService.parse(source, request.rawText()).stream()
                .map(ParsedItemResponse::from)
                .toList();
    }

}
