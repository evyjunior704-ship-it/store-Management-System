package com.salias.storems.model.storemanager;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_manager")
public class StoreManager {

    @Id
    @Column(name = "store_id", nullable = false)
    private Long id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "store_id", nullable = false)
    private Store storeID;

    @OneToOne(optional = false)
    @JoinColumn(name = "manager_id", unique = true, nullable = false)
    private Employee managerID;

    public StoreManager() {
    }

    public StoreManager(Store storeID, Employee managerID) {
        this.storeID = storeID;
        this.managerID = managerID;
        this.id = (storeID == null) ? null : storeID.getStoreID();
    }

    public Long getId() {
        return id;
    }

    public Store getStoreID() {
        return storeID;
    }

    public void setStoreID(Store storeID) {
        this.storeID = storeID;
        this.id = (storeID == null) ? null : storeID.getStoreID();
    }

    public Employee getManagerID() {
        return managerID;
    }

    public void setManagerID(Employee managerID) {
        this.managerID = managerID;
    }
}
