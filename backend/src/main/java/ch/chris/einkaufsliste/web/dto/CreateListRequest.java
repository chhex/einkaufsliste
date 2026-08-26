package ch.chris.einkaufsliste.web.dto;

import java.time.LocalDate;

/**
 * Beide Felder optional - Anlegen einer Liste soll reibungslos gehen (kein
 * Pflicht-Name), einkaufsdatum faellt ohne Angabe auf "heute" zurueck
 * (siehe ShoppingList-Entity-Default).
 * <p>
 * ownerId kommt NICHT vom Client (Sicherheitsluecke: sonst koennte sich ein
 * eingeloggter User als beliebiger anderer User ausgeben) - siehe
 * ListController: der Owner ist immer der authentifizierte User aus dem JWT.
 */
public record CreateListRequest(String name, LocalDate einkaufsdatum) {
}
