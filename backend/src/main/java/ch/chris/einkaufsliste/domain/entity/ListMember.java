package ch.chris.einkaufsliste.domain.entity;

import ch.chris.einkaufsliste.domain.enums.SortField;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Teil des ShoppingList-Aggregats - wird ausschliesslich ueber
 * ShoppingList.addMember() erzeugt. @MapsId sorgt dafuer, dass die
 * zusammengesetzte ID (list_id + user_id) automatisch aus den beiden
 * Objektreferenzen abgeleitet wird, statt sie manuell zu pflegen.
 * AppUser wird trotzdem als Objekt referenziert (nicht nur ID) - das
 * ist hier legitim, weil das schon vorhandene AppUser-Objekt beim
 * Hinzufuegen ohnehin vorliegt (addMember(AppUser user)), nicht separat
 * nachgeladen wird.
 */
@Entity
@Table(name = "list_member")
public class ListMember {

    @EmbeddedId
    private ListMemberId id = new ListMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("listId")
    @JoinColumn(name = "list_id")
    private ShoppingList list;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUser user;

    /**
     * NULL = kein eigenes Override, es gilt list.sortierung.
     * Gesetzt = individuelle Praeferenz dieses Members fuer diese Liste.
     */
    @Column
    private SortField sortierung;

    @Column(name = "added_at", insertable = false, updatable = false)
    private OffsetDateTime addedAt;

    protected ListMember() {
        // fuer JPA
    }

    ListMember(ShoppingList list, AppUser user) {
        this.list = list;
        this.user = user;
    }

    public ListMemberId getId() {
        return id;
    }

    public ShoppingList getList() {
        return list;
    }

    public AppUser getUser() {
        return user;
    }

    public SortField getSortierung() {
        return sortierung;
    }

    public void setSortierung(SortField sortierung) {
        this.sortierung = sortierung;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

}
