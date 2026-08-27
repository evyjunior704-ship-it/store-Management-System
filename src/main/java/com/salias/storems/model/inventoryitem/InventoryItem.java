package com.salias.storems.model.inventoryitem;

import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One store's stock of one product: its local name, price and quantity on hand.
 */
@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @EmbeddedId
    private InventoryItemId id;

    @MapsId("storeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** The name this store shelves the product under, which may differ from the catalogue name. */
    @NotBlank
    @Column(name = "in_store_product_name", nullable = false, length = 200)
    private String inStoreProductName;

    @NotNull
    @PositiveOrZero
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @PositiveOrZero
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    protected InventoryItem() {
        // required by JPA
    }

    public InventoryItem(Store store,
                         Product product,
                         String inStoreProductName,
                         BigDecimal unitPrice,
                         Integer quantity) {
        this.store = store;
        this.product = product;
        this.id = new InventoryItemId(store.getStoreId(), product.getProductId());
        this.inStoreProductName = inStoreProductName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public InventoryItemId getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public Product getProduct() {
        return product;
    }

    public String getInStoreProductName() {
        return inStoreProductName;
    }

    public void setInStoreProductName(String inStoreProductName) {
        this.inStoreProductName = inStoreProductName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InventoryItem that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "InventoryItem{id=%s, name='%s', unitPrice=%s, quantity=%d}"
                .formatted(id, inStoreProductName, unitPrice, quantity);
    }
}
