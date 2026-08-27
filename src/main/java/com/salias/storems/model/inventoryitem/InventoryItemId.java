package com.salias.storems.model.inventoryitem;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link InventoryItem}: one row per product per store.
 *
 * <p>Holds the raw foreign-key values rather than the associated entities; the
 * entity references live on {@code InventoryItem} and are tied to these fields
 * with {@code @MapsId}. JPA requires this class to implement
 * {@code equals}/{@code hashCode}.
 */
@Embeddable
public class InventoryItemId implements Serializable {

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "product_id")
    private Long productId;

    protected InventoryItemId() {
        // required by JPA
    }

    public InventoryItemId(Long storeId, Long productId) {
        this.storeId = storeId;
        this.productId = productId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InventoryItemId that)) {
            return false;
        }
        return Objects.equals(storeId, that.storeId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId, productId);
    }

    @Override
    public String toString() {
        return "InventoryItemId{storeId=%d, productId=%d}".formatted(storeId, productId);
    }
}
