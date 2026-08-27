package com.salias.storems.model.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * A physical store location. Addresses are unique: two stores cannot share one.
 */
@Entity
@Table(name = "store")
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
    @Column(name = "store_id", updatable = false)
    private Long storeId;

    @NotBlank
    @Column(name = "store_name", nullable = false, length = 120)
    private String storeName;

    @NotBlank
    @Column(name = "store_address", nullable = false, unique = true, length = 255)
    private String storeAddress;

    protected Store() {
        // required by JPA
    }

    public Store(String storeName, String storeAddress) {
        this.storeName = storeName;
        this.storeAddress = storeAddress;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Store store)) {
            return false;
        }
        return storeId != null && storeId.equals(store.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(storeId);
    }

    @Override
    public String toString() {
        return "Store{storeId=%d, storeName='%s', storeAddress='%s'}"
                .formatted(storeId, storeName, storeAddress);
    }
}
