package com.salias.storems.model.emergencyinfo;

import com.salias.storems.model.employee.Employee;
import jakarta.persistence.*;

@Entity
public class  EmergencyInfo {

    @Id
    @SequenceGenerator(
            name = "emerg_sequence",
            sequenceName = "emerg_sequence",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "emerg_sequence")
    private Long emergID;
    private String contactName;
    private String phoneNumber;
    private String relationship;

    @ManyToOne
    private Employee employee;

    public EmergencyInfo(String contactName, String phoneNumber, String relationship) {
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
    }

    public EmergencyInfo() {

    }

    public Long getEmergID() {
        return emergID;
    }

    public void setEmergID(Long emergID) {
        this.emergID = emergID;
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
}
