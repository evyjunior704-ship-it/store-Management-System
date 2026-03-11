package com.salias.storems.model.employee;

import com.salias.storems.model.store.Store;
import jakarta.persistence.*;

import java.time.LocalDate;



@Entity(name = "Employee")
@Table(name = "employee")
public class Employee {


    @Id
    @SequenceGenerator(
            name = "emp_sequence",
            sequenceName = "emp_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "emp_sequence"
    )
    @Column(
            name = "id",
            updatable = false
    )
    private Long id;

    @Column(
            name = "emp_first_name",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String empFirstName;

    @Column(
            name = "emp_last_name",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String empLastName;

    @Column(
            name = "empdob",
            nullable = false
    )
    private LocalDate empDOB;

    @Column(
            name = "emp_address",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String empAddress;

    @Column(
            name = "emp_role",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String empRole;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "store_associated_to",
            nullable = false)
    private Store storeAssociatedTo;


    public Employee(){}

    public Employee(
                    String empFirstName,
                    String empLastName,
                    LocalDate empDOB,
                    String empAddress,
                    String empRole,
                    Store storeAssociatedTo){
        this.empFirstName = empFirstName;
        this.empLastName = empLastName;
        this.empDOB = empDOB;
        this.empAddress = empAddress;
        this.storeAssociatedTo = storeAssociatedTo;
        this.empRole = empRole;
    }



    public Long getId() {
        return id;
    }


    public String getEmpAddress() {
        return empAddress;
    }

    public void setEmpAddress(String empAddress) {
        this.empAddress = empAddress;
    }

    public String getEmpRole() {
        return empRole;
    }

    public void setEmpRole(String empRole) {
        this.empRole = empRole;
    }

    public LocalDate getEmpDOB() {
        return empDOB;
    }

    public void setEmpDOB(LocalDate empDOB) {
        this.empDOB = empDOB;
    }
    public String toString(){
        return null;
    }



    public String getEmpLastName() {
        return empLastName;
    }

    public void setEmpLastName(String empLastName) {
        this.empLastName = empLastName;
    }

    public String getEmpFirstName() {
        return empFirstName;
    }

    public void setEmpFirstName(String empFirstName) {
        this.empFirstName = empFirstName;
    }

    // ADD: needed for DTO/service mapping
    public Store getStoreAssociatedTo() {
        return storeAssociatedTo;
    }

    public void setStoreAssociatedTo(Store storeAssociatedTo) {
        this.storeAssociatedTo = storeAssociatedTo;
    }
}
