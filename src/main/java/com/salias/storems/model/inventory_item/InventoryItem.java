package com.salias.storems.model.inventory_item;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Inventory_Item")
public class InventoryItem {

    @EmbeddedId
    private InventoryItemId id;

    @Column(
            nullable = false,
            columnDefinition = "TEXT")
    private String inStoreProductName;

    @Column(nullable = false,
            columnDefinition = "DECIMAL(10,2)")
    private BigDecimal unitPrice;

    @Column(nullable = false,
            columnDefinition = "INT")
    private Integer quantity;

    public InventoryItem(){}

    public InventoryItem(InventoryItemId id,
                         String productNameIn,
                         BigDecimal unitPrice,
                         Integer quantity){
        this.id = id;
        this.inStoreProductName = productNameIn;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
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

    public InventoryItemId getId() {
        return id;
    }

    public void setId(InventoryItemId id) {
        this.id = id;
    }
}
