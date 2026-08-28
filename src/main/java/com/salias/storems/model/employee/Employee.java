package com.salias.storems.model.employee;

import com.salias.storems.model.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A person employed at one store.
 */
@Entity
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
    @Column(name = "id", updatable = false)
    private Long id;

    @NotBlank
    @Column(name = "emp_first_name", nullable = false, length = 120)
    private String empFirstName;

    @NotBlank
    @Column(name = "emp_last_name", nullable = false, length = 120)
    private String empLastName;

    @Past
    @NotNull
    @Column(name = "emp_dob", nullable = false)
    private LocalDate empDob;

    @NotBlank
    @Column(name = "emp_address", nullable = false, length = 255)
    private String empAddress;

    @NotBlank
    @Column(name = "emp_role", nullable = false, length = 80)
    private String empRole;

    /** Many employees work at one store. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_associated_to", nullable = false)
    private Store storeAssociatedTo;

    /**
     * Login name, or null for staff with no system access. Unique where present,
     * so most employees can exist without credentials.
     */
    @Column(name = "username", unique = true, length = 60)
    private String username;

    /** BCrypt hash. Never holds a plaintext password. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected Employee() {
        // required by JPA
    }

    public Employee(String empFirstName,
                    String empLastName,
                    LocalDate empDob,
                    String empAddress,
                    String empRole,
                    Store storeAssociatedTo) {
        this.empFirstName = empFirstName;
        this.empLastName = empLastName;
        this.empDob = empDob;
        this.empAddress = empAddress;
        this.empRole = empRole;
        this.storeAssociatedTo = storeAssociatedTo;
    }

    public Long getId() {
        return id;
    }

    public String getEmpFirstName() {
        return empFirstName;
    }

    public void setEmpFirstName(String empFirstName) {
        this.empFirstName = empFirstName;
    }

    public String getEmpLastName() {
        return empLastName;
    }

    public void setEmpLastName(String empLastName) {
        this.empLastName = empLastName;
    }

    public LocalDate getEmpDob() {
        return empDob;
    }

    public void setEmpDob(LocalDate empDob) {
        this.empDob = empDob;
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

    public Store getStoreAssociatedTo() {
        return storeAssociatedTo;
    }

    public void setStoreAssociatedTo(Store storeAssociatedTo) {
        this.storeAssociatedTo = storeAssociatedTo;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Grants or updates system access.
     *
     * @param passwordHash an already-encoded hash; callers must never pass plaintext
     */
    public void setCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /** Revokes system access without deleting the employee record. */
    public void revokeCredentials() {
        this.username = null;
        this.passwordHash = null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Employee employee)) {
            return false;
        }
        return id != null && id.equals(employee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Employee{id=%d, name='%s %s', role='%s'}"
                .formatted(id, empFirstName, empLastName, empRole);
    }
}
