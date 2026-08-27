package com.salias.storems.model.emergencyinfo;

import com.salias.storems.model.employee.Employee;
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

import java.util.Objects;

/**
 * An emergency contact for an employee, including contact details and
 * the contact's relationship to that employee.
 */
@Entity
@Table(name = "emergency_info")
public class EmergencyInfo {

    @Id
    @SequenceGenerator(
            name = "emerg_sequence",
            sequenceName = "emerg_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "emerg_sequence"
    )
    @Column(name = "emergency_info_id", updatable = false)
    private Long id;

    @NotBlank
    @Column(name = "contact_name", nullable = false, length = 200)
    private String contactName;

    @NotBlank
    @Column(name = "phone_number", nullable = false, length = 40)
    private String phoneNumber;

    @NotBlank
    @Column(name = "relationship", nullable = false, length = 80)
    private String relationship;

    /** An employee may list several emergency contacts. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    protected EmergencyInfo() {
        // required by JPA
    }

    public EmergencyInfo(String contactName, String phoneNumber, String relationship, Employee employee) {
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.employee = employee;
    }

    public Long getId() {
        return id;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EmergencyInfo emergencyInfo)) {
            return false;
        }
        return id != null && id.equals(emergencyInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "EmergencyInfo{id=%d, contactName='%s', relationship='%s'}"
                .formatted(id, contactName, relationship);
    }
}
