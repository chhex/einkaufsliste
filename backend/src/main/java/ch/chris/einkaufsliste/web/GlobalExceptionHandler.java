package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Uebersetzt Service-Exceptions in einheitliche HTTP-Fehler-Responses,
 * statt dass jeder Controller das einzeln try/catchen muss.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Alle unsere Services werfen IllegalArgumentException fuer
    // "nicht gefunden" / "ungueltige Anfrage" (bewusst einheitlich, siehe
    // ListService/ItemService/UnitService/CategoryService) - hier zentral
    // auf 400 gemappt statt in jedem Controller einzeln.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Ungueltige Anfrage");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

}
