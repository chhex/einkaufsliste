package ch.chris.einkaufsliste.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Teil des ShoppingList-Aggregats - wird ausschliesslich ueber
 * ShoppingList.addItem() erzeugt (Konstruktor package-private), damit nie
 * ein Item ohne zugehoerige Liste existieren kann.
 */
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private ShoppingList list;

    @Column(nullable = false)
    private String bezeichnung;

    @Column(nullable = false)
    private BigDecimal menge;

    @Column(nullable = false)
    private String einheit;

    /**
     * Freitext, nullable - siehe Kommentar in V2__create_domain_schema.sql.
     * Bleibt beim Import zunaechst leer ("unkategorisiert").
     */
    @Column
    private String kategorie;

    @Column(nullable = false)
    private boolean abgehakt = false;

    @Column(name = "abgehakt_am")
    private OffsetDateTime abgehaktAm;

    @Column(nullable = false)
    private int reihenfolge = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Item() {
        // fuer JPA
    }

    Item(ShoppingList list, String bezeichnung, BigDecimal menge, String einheit) {
        this.list = list;
        this.bezeichnung = bezeichnung;
        this.menge = menge;
        this.einheit = einheit;
    }

    void detachFromList() {
        this.list = null;
    }

    public Long getId() {
        return id;
    }

    public ShoppingList getList() {
        return list;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public BigDecimal getMenge() {
        return menge;
    }

    public void setMenge(BigDecimal menge) {
        this.menge = menge;
    }

    public String getEinheit() {
        return einheit;
    }

    public void setEinheit(String einheit) {
        this.einheit = einheit;
    }

    public String getKategorie() {
        return kategorie;
    }

    public void setKategorie(String kategorie) {
        this.kategorie = kategorie;
    }

    public boolean isAbgehakt() {
        return abgehakt;
    }

    public void setAbgehakt(boolean abgehakt) {
        this.abgehakt = abgehakt;
        this.abgehaktAm = abgehakt ? OffsetDateTime.now() : null;
    }

    public OffsetDateTime getAbgehaktAm() {
        return abgehaktAm;
    }

    public int getReihenfolge() {
        return reihenfolge;
    }

    public void setReihenfolge(int reihenfolge) {
        this.reihenfolge = reihenfolge;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
