package com.salias.storems.model.sales;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A single sale of one product at one store.
 */
@Entity
@Table(name = "sales")
public class Sales {

    @Id
    @SequenceGenerator(
            name = "sale_sequence",
            sequenceName = "sale_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sale_sequence"
    )
    @Column(name = "sale_number", updatable = false)
    private Long saleNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * The employee who rang the sale through. Nullable because bulk-loaded
     * historical sales have no operator, but every sale made through the till
     * records one.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by_employee_id")
    private Employee soldBy;

    @NotNull
    @Positive
    @Column(name = "quantity_bought", nullable = false, updatable = false)
    private Integer quantityBought;

    /** Total price for this sale, not the unit price. */
    @NotNull
    @PositiveOrZero
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * When the sale happened. This is business data, not an audit column: it is
     * assigned explicitly so historical sales can be loaded (and so sales-history
     * models can train on real dates). Always set via the constructor.
     */
    @NotNull
    @Column(name = "made_at", nullable = false, updatable = false)
    private LocalDateTime madeAt;

    protected Sales() {
        // required by JPA
    }

    /** Records a sale happening now, with no operator recorded. */
    public Sales(Product product, Store store, Integer quantityBought, BigDecimal price) {
        this(product, store, quantityBought, price, LocalDateTime.now());
    }

    /** Records a till sale rung through by a named employee, happening now. */
    public Sales(Product product,
                 Store store,
                 Employee soldBy,
                 Integer quantityBought,
                 BigDecimal price) {
        this(product, store, soldBy, quantityBought, price, LocalDateTime.now());
    }

    /** Records a sale by a named employee at an explicit time, for backfilling. */
    public Sales(Product product,
                 Store store,
                 Employee soldBy,
                 Integer quantityBought,
                 BigDecimal price,
                 LocalDateTime madeAt) {
        this(product, store, quantityBought, price, madeAt);
        this.soldBy = soldBy;
    }

    /** Records a sale at an explicit time, for backfilling history. */
    public Sales(Product product,
                 Store store,
                 Integer quantityBought,
                 BigDecimal price,
                 LocalDateTime madeAt) {
        this.product = product;
        this.store = store;
        this.quantityBought = quantityBought;
        this.price = price;
        this.madeAt = madeAt;
    }

    public Long getSaleNumber() {
        return saleNumber;
    }

    public Product getProduct() {
        return product;
    }

    public Store getStore() {
        return store;
    }

    public Employee getSoldBy() {
        return soldBy;
    }

    public Integer getQuantityBought() {
        return quantityBought;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getMadeAt() {
        return madeAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Sales that)) {
            return false;
        }
        return saleNumber != null && saleNumber.equals(that.saleNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(saleNumber);
    }

    @Override
    public String toString() {
        return "Sales{saleNumber=%d, quantityBought=%d, price=%s, madeAt=%s}"
                .formatted(saleNumber, quantityBought, price, madeAt);
    }
}
