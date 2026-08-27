package com.salias.storems.model.storemanager;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Assigns one employee as the manager of one store.
 *
 * <p>The primary key is derived from the store, so a store can have at most one
 * manager. The {@code manager_id} unique constraint stops one employee from
 * managing two stores.
 */
@Entity
@Table(name = "store_manager")
public class StoreManager {

    @Id
    @Column(name = "store_id", updatable = false)
    private Long storeId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false, unique = true)
    private Employee manager;

    protected StoreManager() {
        // required by JPA
    }

    public StoreManager(Store store, Employee manager) {
        this.store = store;
        this.manager = manager;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Store getStore() {
        return store;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StoreManager storeManager)) {
            return false;
        }
        return storeId != null && storeId.equals(storeManager.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(storeId);
    }

    @Override
    public String toString() {
        return "StoreManager{storeId=%d, manager=%s}".formatted(storeId, manager);
    }
}
