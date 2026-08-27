package com.salias.storems.model.sales;

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
import org.hibernate.annotations.CreationTimestamp;

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

    @NotNull
    @Positive
    @Column(name = "quantity_bought", nullable = false, updatable = false)
    private Integer quantityBought;

    /** Total price for this sale, not the unit price. */
    @NotNull
    @PositiveOrZero
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Set by Hibernate on insert; never updated afterwards. */
    @CreationTimestamp
    @Column(name = "made_at", nullable = false, updatable = false)
    private LocalDateTime madeAt;

    protected Sales() {
        // required by JPA
    }

    public Sales(Product product, Store store, Integer quantityBought, BigDecimal price) {
        this.product = product;
        this.store = store;
        this.quantityBought = quantityBought;
        this.price = price;
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
