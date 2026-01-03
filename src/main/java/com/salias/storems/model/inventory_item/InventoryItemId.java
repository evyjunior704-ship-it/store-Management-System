package com.salias.storems.model.inventory_item;

import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Embeddable;

import jakarta.persistence.ManyToOne;

import java.io.Serializable;

@Embeddable
public class InventoryItemId implements Serializable {

    @ManyToOne
    private Store storeID;
    @ManyToOne
    private Product productID;

    public InventoryItemId(Store storeID,Product productID){
        this.storeID = storeID;
        this.productID = productID;
    }

    public InventoryItemId() {

    }

    public Store getStoreID() {
        return storeID;
    }

    public void setStoreID(Store storeID) {
        this.storeID = storeID;
    }

    public Product getProductID() {
        return productID;
    }

    public void setProductID(Product productID) {
        this.productID = productID;
    }
}
