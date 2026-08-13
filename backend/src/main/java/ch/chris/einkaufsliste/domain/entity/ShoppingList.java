package ch.chris.einkaufsliste.domain.entity;

import ch.chris.einkaufsliste.domain.enums.ListStatus;
import ch.chris.einkaufsliste.domain.enums.SortField;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregat-Wurzel (DDD-Sinne): Item und ListMember gehoeren fachlich zur
 * Liste und existieren nicht eigenstaendig - deshalb hier als echte
 * Objekt-Collections (@OneToMany, cascade ALL, orphanRemoval), im
 * Gegensatz zu ownerId, welches eine Referenz auf ein ANDERES Aggregat
 * (AppUser) ist und deshalb bewusst nur als ID gehalten wird (kein
 * ungewolltes Mitladen fremder Aggregate, kein N+1 falls ein User viele
 * Listen besitzt).
 * <p>
 * Item- und ListMember-Instanzen werden ausschliesslich ueber addItem()/
 * addMember() erzeugt (deren Konstruktoren sind package-private) - das
 * stellt sicher, dass niemals ein Item/ListMember ohne zugehoerige Liste
 * existieren kann.
 */
@Entity
@Table(name = "list")
public class ShoppingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Unidirektionale Objektreferenz auf den Owner - bewusst semantisch
     * (nicht nur ID), weil "wem gehoert diese Liste" fachlich zentral ist
     * und in dieser kleinen App kein Skalierungsproblem darstellt.
     * Bewusst KEINE Rueckseite auf AppUser (kein List<ShoppingList> dort) -
     * ein User koennte potenziell viele Listen besitzen, das wollen wir nie
     * "automatisch" mitladen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListStatus status = ListStatus.AKTIV;

    @Column(nullable = false)
    private SortField sortierung = SortField.KATEGORIE;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListMember> members = new ArrayList<>();

    protected ShoppingList() {
        // fuer JPA
    }

    public ShoppingList(String name, AppUser owner) {
        this.name = name;
        this.owner = owner;
    }

    public Item addItem(String bezeichnung, BigDecimal menge, String einheit) {
        Item item = new Item(this, bezeichnung, menge, einheit);
        items.add(item);
        return item;
    }

    public void removeItem(Item item) {
        items.remove(item);
        item.detachFromList();
    }

    public ListMember addMember(AppUser user) {
        ListMember member = new ListMember(this, user);
        members.add(member);
        return member;
    }

    public void removeMember(ListMember member) {
        members.remove(member);
        member.detachFromList();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AppUser getOwner() {
        return owner;
    }

    public ListStatus getStatus() {
        return status;
    }

    public void setStatus(ListStatus status) {
        this.status = status;
    }

    public SortField getSortierung() {
        return sortierung;
    }

    public void setSortierung(SortField sortierung) {
        this.sortierung = sortierung;
    }

    public OffsetDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(OffsetDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public List<ListMember> getMembers() {
        return List.copyOf(members);
    }

}
