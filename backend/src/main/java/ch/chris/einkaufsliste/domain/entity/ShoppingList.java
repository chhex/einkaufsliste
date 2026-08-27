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
import java.time.LocalDate;
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

    /**
     * Optional - Anlegen einer Liste soll moeglichst reibungslos sein
     * (Anforderung: "man will einfach eine neue Einkaufsliste machen"),
     * ohne dass ein Name Pflicht ist. Ohne Name zeigt die UI stattdessen
     * das Einkaufsdatum als Bezeichner.
     */
    @Column
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

    /**
     * Default = Erfassungsdatum, aber jederzeit aenderbar (z.B. wenn man
     * die Liste im Voraus fuer einen spaeteren Einkauf anlegt, oder
     * rueckwirkend korrigiert).
     */
    @Column(nullable = false)
    private LocalDate einkaufsdatum = LocalDate.now();

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

    /**
     * Variante mit explizitem Einkaufsdatum (z.B. Liste im Voraus fuer ein
     * bestimmtes Datum anlegen). Ohne Angabe gilt der Field-Default (heute).
     */
    public ShoppingList(String name, AppUser owner, LocalDate einkaufsdatum) {
        this(name, owner);
        if (einkaufsdatum != null) {
            this.einkaufsdatum = einkaufsdatum;
        }
    }

    public Item addItem(String bezeichnung, BigDecimal menge, String einheit) {
        Item item = new Item(this, bezeichnung, menge, einheit);
        items.add(item);
        return item;
    }

    /**
     * Nur Bezeichnung, ohne Menge/Einheit - beides kann spaeter nachgetragen
     * werden (Anforderung: reibungsloses Hinzufuegen, nur Artikelname Pflicht).
     */
    public Item addItem(String bezeichnung) {
        Item item = new Item(this, bezeichnung);
        items.add(item);
        return item;
    }

    public void removeItem(Item item) {
        // Bewusst KEIN item.detachFromList() hier: orphanRemoval=true kuemmert
        // sich beim Flush selbststaendig um das DELETE. Wuerden wir die
        // Objektreferenz zusaetzlich manuell nullen, wuerde Hibernate vorher
        // ein UPDATE list_id=NULL versuchen und an der NOT-NULL-Constraint
        // scheitern.
        items.remove(item);
    }

    public ListMember addMember(AppUser user) {
        ListMember member = new ListMember(this, user);
        members.add(member);
        return member;
    }

    public void removeMember(ListMember member) {
        // Analog zu removeItem(): kein manuelles detachFromList() noetig,
        // orphanRemoval erledigt das DELETE selbststaendig.
        members.remove(member);
    }

    /**
     * Archiviert die Liste (Status -> ARCHIVIERT, archivedAt gesetzt).
     */
    public void archive() {
        this.status = ListStatus.ARCHIVIERT;
        this.archivedAt = OffsetDateTime.now();
    }

    /**
     * Reaktiviert eine archivierte Liste: Status -> AKTIV, archivedAt
     * zurueckgesetzt, UND alle Haken werden zurueckgesetzt (Anforderung:
     * "Haken gehen bei Reaktivierung alle weg").
     */
    public void reactivate() {
        this.status = ListStatus.AKTIV;
        this.archivedAt = null;
        items.forEach(item -> item.setAbgehakt(false));
    }

    /**
     * Wird aufgerufen, wenn ein einzelnes Item auf einer ARCHIVIERTEN Liste
     * wieder aufgehakt wird - loest den sonst widerspruechlichen Zustand
     * "Liste archiviert, aber ein Item offen" auf. Bewusst ANDERS als
     * reactivate(): setzt NICHT alle Haken zurueck (nur das eine Item wurde
     * ja vom Aufrufer schon geaendert), sondern nur Status/archivedAt.
     * Einkaufsdatum wird auf heute gesetzt, weil das faktisch ein neuer
     * Einkaufsgang ist, der an die alte Liste anknuepft. No-op, falls die
     * Liste ohnehin schon AKTIV ist.
     */
    public void unarchiveDueToItemUncheck() {
        if (status == ListStatus.ARCHIVIERT) {
            this.status = ListStatus.AKTIV;
            this.archivedAt = null;
            this.einkaufsdatum = LocalDate.now();
        }
    }

    /**
     * True, wenn die Liste mindestens ein Item hat und ALLE abgehakt sind -
     * die Bedingung, unter der automatisch archiviert wird (siehe
     * ListService.archiveIfAllItemsChecked, aufgerufen nach jedem Abhaken).
     * Eine leere Liste gilt bewusst NICHT als "alle abgehakt".
     */
    public boolean areAllItemsChecked() {
        return !items.isEmpty() && items.stream().allMatch(Item::isAbgehakt);
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

    public LocalDate getEinkaufsdatum() {
        return einkaufsdatum;
    }

    public void setEinkaufsdatum(LocalDate einkaufsdatum) {
        this.einkaufsdatum = einkaufsdatum;
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
