package com.salias.storems.model.supplies;

import com.salias.storems.model.itemsupplier.ItemSupplier;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Supplies {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "supply_sequence")
    @SequenceGenerator(
            name = "supply_sequence",
            sequenceName = "supply_sequence",
            allocationSize = 1)
    private Long supplyNumber;

    @ManyToOne
    //one store can be supplied many items
    // we do not look at how one item can be supplied to many stores. this is another row in the supplies table
    private Store storeSupplied;
    @ManyToOne
    // one item has one supplier
    private ItemSupplier supplier;

    @Column(
            updatable = false,
            nullable = false)
    private LocalDateTime suppliedAt;

    @ManyToOne
    // A supply row is one product
    private Product itemSupplied;

    @Column(
            name = "delivery_note",
            columnDefinition = "TEXT",
            updatable = false)
    private String deliveryNote;

    //Quantity supplied in boxes.
    @Column(
            columnDefinition =  "INT",
            nullable = false,
            updatable = false
    )
    private Integer quantitySupplied;

    public Supplies(Store storeSupplied, ItemSupplier supplier, Integer quantitySupplied){
        this.storeSupplied = storeSupplied;
        this.supplier = supplier;
        this.quantitySupplied = quantitySupplied;
    }

    public Supplies() {

    }


    public Long getSupplyNumber() {
        return supplyNumber;
    }

    public void setSupplyNumber(Long supplyNumber) {
        this.supplyNumber = supplyNumber;
    }

    public ItemSupplier getSupplier() {
        return supplier;
    }

    public void setSupplier(ItemSupplier supplier) {
        this.supplier = supplier;
    }

    public LocalDateTime getSuppliedAt() {
        return suppliedAt;
    }

    public void setSuppliedAt(LocalDateTime suppliedAt) {
        this.suppliedAt = suppliedAt;
    }

    public Store getStoreSupplied() {
        return storeSupplied;
    }

    public void setStoreSupplied(Store storeSupplied) {
        this.storeSupplied = storeSupplied;
    }

    public Integer getQuantitySupplied() {
        return quantitySupplied;
    }

    public void setQuantitySupplied(Integer quantitySupplied) {
        this.quantitySupplied = quantitySupplied;
    }
}
