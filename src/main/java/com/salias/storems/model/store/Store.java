package com.salias.storems.model.store;

import jakarta.persistence.*;

@Entity
@Table(
        name = "Store",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"storeAddress"})
        }
)
public class Store {

    @Id
    @SequenceGenerator(
            name = "store_sequence",
            sequenceName = "store_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "store_sequence"
    )
    private Long storeID;

    private String storeName;

    @Column(

            unique = true
    )
    private String storeAddress;

    public Store(){}

    public Store(String storeName, String storeAddress){
        this.storeName = storeName;
        this.storeAddress = storeAddress;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getStoreID() {
        return storeID;
    }

    public void setStoreID(Long storeID) {
        this.storeID = storeID;
    }
}
