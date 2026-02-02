package com.salias.storems.model.storemanager;


import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.store.Store;
import jakarta.persistence.*;

@Entity
public class StoreManager {

    @Id
    @OneToOne
    @JoinColumn(name = "store",
            nullable = false)
    private Store storeID;

    @OneToOne
    @JoinColumn(name = "Manager_ID",
            unique = true,
            nullable = false)
    private Employee managerID;

    public Employee getManagerID() {
        return managerID;
    }

    public void setManagerID(Employee managerID) {
        this.managerID = managerID;
    }

    public Store getStoreID() {
        return storeID;
    }

    public void setStoreID(Store storeID) {
        this.storeID = storeID;
    }

    public StoreManager(){}

    public StoreManager(Store storeID,Employee managerID){
        this.storeID = storeID;
        this.managerID = managerID;
    }



}
