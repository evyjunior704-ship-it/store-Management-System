package com.salias.storems.model.itemsupplier;

import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * A supply arrangement: supplier X provides product Y to store Z.
 *
 * <p>Note: {@code supplierId} is currently a bare number with no {@code Supplier}
 * entity behind it, so supplier names and contact details have nowhere to live.
 * See README.md for this known modelling gap.
 */
@Entity
@Table(name = "item_supplier")
public class ItemSupplier {

    @EmbeddedId
    private ItemSupplierId id;

    @MapsId("storeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected ItemSupplier() {
        // required by JPA
    }

    public ItemSupplier(Long supplierId, Store store, Product product) {
        this.id = new ItemSupplierId(supplierId, store.getStoreId(), product.getProductId());
        this.store = store;
        this.product = product;
    }

    public ItemSupplierId getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemSupplier that)) {
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
        return "ItemSupplier{id=%s}".formatted(id);
    }
}
