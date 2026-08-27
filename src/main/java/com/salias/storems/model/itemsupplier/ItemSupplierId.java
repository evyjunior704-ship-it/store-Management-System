package com.salias.storems.model.itemsupplier;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link ItemSupplier}: which supplier provides which product
 * to which store.
 *
 * <p>JPA requires this class to implement {@code equals}/{@code hashCode}.
 */
@Embeddable
public class ItemSupplierId implements Serializable {

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "product_id")
    private Long productId;

    protected ItemSupplierId() {
        // required by JPA
    }

    public ItemSupplierId(Long supplierId, Long storeId, Long productId) {
        this.supplierId = supplierId;
        this.storeId = storeId;
        this.productId = productId;
    }

    public Long getSupplierId() {
        return supplierId;
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
        if (!(other instanceof ItemSupplierId that)) {
            return false;
        }
        return Objects.equals(supplierId, that.supplierId)
                && Objects.equals(storeId, that.storeId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId, storeId, productId);
    }

    @Override
    public String toString() {
        return "ItemSupplierId{supplierId=%d, storeId=%d, productId=%d}"
                .formatted(supplierId, storeId, productId);
    }
}
