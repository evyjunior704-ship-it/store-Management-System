package com.salias.storems.model.itemsupplier;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class ItemSupplier {

    @EmbeddedId
    private ItemSupplierId supplierId;

    public ItemSupplier(ItemSupplierId supplierId){
        this.supplierId = supplierId;
    }


    public ItemSupplier() {

    }

    public ItemSupplierId getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(ItemSupplierId supplierId) {
        this.supplierId = supplierId;
    }
}
