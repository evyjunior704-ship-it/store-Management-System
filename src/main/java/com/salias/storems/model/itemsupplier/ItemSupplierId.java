package com.salias.storems.model.itemsupplier;

import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

import java.io.Serializable;

@Embeddable

public class ItemSupplierId implements Serializable {
    private Long supplierID;

    @ManyToOne
    private Store storeID;
    @ManyToOne
    private Product itemID;

    public ItemSupplierId(){}

    public ItemSupplierId(Long supplierID, Store storeID, Product itemID){
        this.itemID = itemID;
        this.supplierID = supplierID;
        this.storeID = storeID;
    }
    public Long getSupplierID() {
        return supplierID;
    }

    public Product getItemID() {
        return itemID;
    }

    public Store getStoreID() {
        return storeID;
    }

    public void setItemID(Product itemID) {
        this.itemID = itemID;
    }

    public void setStoreID(Store storeID) {
        this.storeID = storeID;
    }

    public void setSupplierID(Long supplierID) {
        this.supplierID = supplierID;
    }
}
