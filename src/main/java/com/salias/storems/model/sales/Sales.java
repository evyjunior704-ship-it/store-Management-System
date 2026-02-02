package com.salias.storems.model.sales;

import com.salias.storems.model.product.Product;
import com.salias.storems.model.store.Store;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Sales {
    @Id
    @SequenceGenerator(
            name = "saleNumber",
            sequenceName = "saleNumber",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "saleNumber")
    private Long saleNumber;

    @ManyToOne
    @JoinColumn(name = "bought_item_id_")
    private Product itemID;
    private Integer quantityBought;
    private BigDecimal price;
    @ManyToOne
    private Store store;
    private LocalDateTime madeAt;

    public Sales(Integer quantityBought, BigDecimal price, Store store) {
        this.quantityBought = quantityBought;
        this.price = price;
        this.store = store;
        this.madeAt = LocalDateTime.now();
    }

    public Sales() {

    }

    public Product getItemID() {
        return itemID;
    }

    public void setItemID(Product itemID) {
        this.itemID = itemID;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantityBought() {
        return quantityBought;
    }

    public void setQuantityBought(Integer quantityBought) {
        this.quantityBought = quantityBought;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public LocalDateTime getMadeAt() {
        return madeAt;
    }

    public void setMadeAt(LocalDateTime madeAt) {
        this.madeAt = madeAt;
    }
}
