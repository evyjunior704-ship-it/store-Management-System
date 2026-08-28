package com.salias.storems.service;

import com.salias.storems.model.emergencyinfo.EmergencyInfo;
import com.salias.storems.model.emergencyinfo.EmergencyInfoRepository;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Employee records, their emergency contacts, and their system access.
 *
 * <p>Every read and write is scoped to a store id supplied by the caller, which
 * comes from the authenticated principal rather than the request, so a manager
 * cannot reach another store's staff.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmergencyInfoRepository emergencyInfoRepository,
                           StoreRepository storeRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.emergencyInfoRepository = emergencyInfoRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> findStaff(Long storeId) {
        return employeeRepository.findByStoreAssociatedTo_StoreId(storeId);
    }

    /** Loads an employee, refusing if they work at a different store. */
    public Employee findAtStore(Long storeId, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + employeeId));
        if (!employee.getStoreAssociatedTo().getStoreId().equals(storeId)) {
            throw new ResourceNotFoundException(
                    "Employee %d does not work at store %d".formatted(employeeId, storeId));
        }
        return employee;
    }

    @Transactional
    public Employee create(Long storeId,
                           String firstName,
                           String lastName,
                           LocalDate dob,
                           String address,
                           String role,
                           String username,
                           String rawPassword) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("No store with id " + storeId));

        Employee employee = new Employee(firstName, lastName, dob, address, role, store);
        applyCredentials(employee, username, rawPassword);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long storeId,
                           Long employeeId,
                           String firstName,
                           String lastName,
                           LocalDate dob,
                           String address,
                           String role) {
        Employee employee = findAtStore(storeId, employeeId);
        employee.setEmpFirstName(firstName);
        employee.setEmpLastName(lastName);
        employee.setEmpDob(dob);
        employee.setEmpAddress(address);
        employee.setEmpRole(role);
        return employee;
    }

    /** Sets or replaces login credentials. A blank password leaves the existing one. */
    @Transactional
    public Employee setCredentials(Long storeId, Long employeeId, String username, String rawPassword) {
        Employee employee = findAtStore(storeId, employeeId);
        applyCredentials(employee, username, rawPassword);
        return employee;
    }

    @Transactional
    public Employee revokeAccess(Long storeId, Long employeeId) {
        Employee employee = findAtStore(storeId, employeeId);
        employee.revokeCredentials();
        return employee;
    }

    // ---------- emergency contacts ----------

    public List<EmergencyInfo> findEmergencyContacts(Long storeId, Long employeeId) {
        findAtStore(storeId, employeeId);
        return emergencyInfoRepository.findByEmployee_Id(employeeId);
    }

    @Transactional
    public EmergencyInfo addEmergencyContact(Long storeId,
                                             Long employeeId,
                                             String contactName,
                                             String phoneNumber,
                                             String relationship) {
        Employee employee = findAtStore(storeId, employeeId);
        return emergencyInfoRepository.save(
                new EmergencyInfo(contactName, phoneNumber, relationship, employee));
    }

    @Transactional
    public EmergencyInfo updateEmergencyContact(Long storeId,
                                                Long contactId,
                                                String contactName,
                                                String phoneNumber,
                                                String relationship) {
        EmergencyInfo contact = emergencyInfoRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("No emergency contact " + contactId));
        // Scope check: the contact's employee must work at the caller's store.
        findAtStore(storeId, contact.getEmployee().getId());

        contact.setContactName(contactName);
        contact.setPhoneNumber(phoneNumber);
        contact.setRelationship(relationship);
        return contact;
    }

    @Transactional
    public void deleteEmergencyContact(Long storeId, Long contactId) {
        EmergencyInfo contact = emergencyInfoRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("No emergency contact " + contactId));
        findAtStore(storeId, contact.getEmployee().getId());
        emergencyInfoRepository.delete(contact);
    }

    private void applyCredentials(Employee employee, String username, String rawPassword) {
        if (username == null || username.isBlank()) {
            return;
        }
        String trimmed = username.trim();

        boolean takenBySomeoneElse = !trimmed.equals(employee.getUsername())
                && employeeRepository.existsByUsername(trimmed);
        if (takenBySomeoneElse) {
            throw new IllegalArgumentException("Username '%s' is already taken".formatted(trimmed));
        }

        String hash = (rawPassword == null || rawPassword.isBlank())
                ? employee.getPasswordHash()
                : passwordEncoder.encode(rawPassword);

        if (hash == null) {
            throw new IllegalArgumentException("A password is required when granting system access");
        }
        employee.setCredentials(trimmed, hash);
    }
}
