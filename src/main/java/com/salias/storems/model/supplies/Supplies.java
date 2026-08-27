package com.salias.storems.model.supplies;

import com.salias.storems.model.itemsupplier.ItemSupplier;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One delivery of one product from one supplier to one store.
 *
 * <p>The store and product are not stored again here: the {@link ItemSupplier}
 * reference already identifies both, so they are exposed through
 * {@link #getStoreSupplied()} and {@link #getItemSupplied()}.
 */
@Entity
@Table(name = "supplies")
public class Supplies {

    @Id
    @SequenceGenerator(
            name = "supply_sequence",
            sequenceName = "supply_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "supply_sequence"
    )
    @Column(name = "supply_number", updatable = false)
    private Long supplyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id", nullable = false),
            @JoinColumn(name = "store_id", referencedColumnName = "store_id", nullable = false),
            @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    })
    private ItemSupplier supplier;

    /** Set by Hibernate on insert; never updated afterwards. */
    @CreationTimestamp
    @Column(name = "supplied_at", nullable = false, updatable = false)
    private LocalDateTime suppliedAt;

    @Column(name = "delivery_note", updatable = false, length = 2000)
    private String deliveryNote;

    /** Quantity supplied, in boxes. */
    @NotNull
    @Positive
    @Column(name = "quantity_supplied", nullable = false, updatable = false)
    private Integer quantitySupplied;

    protected Supplies() {
        // required by JPA
    }

    public Supplies(ItemSupplier supplier, Integer quantitySupplied, String deliveryNote) {
        this.supplier = supplier;
        this.quantitySupplied = quantitySupplied;
        this.deliveryNote = deliveryNote;
    }

    public Long getSupplyNumber() {
        return supplyNumber;
    }

    public ItemSupplier getSupplier() {
        return supplier;
    }

    public Store getStoreSupplied() {
        return supplier.getStore();
    }

    public Product getItemSupplied() {
        return supplier.getProduct();
    }

    public LocalDateTime getSuppliedAt() {
        return suppliedAt;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public Integer getQuantitySupplied() {
        return quantitySupplied;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Supplies that)) {
            return false;
        }
        return supplyNumber != null && supplyNumber.equals(that.supplyNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(supplyNumber);
    }

    @Override
    public String toString() {
        return "Supplies{supplyNumber=%d, quantitySupplied=%d, suppliedAt=%s}"
                .formatted(supplyNumber, quantitySupplied, suppliedAt);
    }
}
