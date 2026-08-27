package com.salias.storems.service;

import com.salias.storems.model.emergencyinfo.EmergencyInfo;
import com.salias.storems.model.emergencyinfo.EmergencyInfoRepository;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Employee records and their emergency contacts.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmergencyInfoRepository emergencyInfoRepository) {
        this.employeeRepository = employeeRepository;
        this.emergencyInfoRepository = emergencyInfoRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + employeeId));
    }

    public List<EmergencyInfo> findEmergencyContacts(Long employeeId) {
        return emergencyInfoRepository.findByEmployee_Id(employeeId);
    }

    @Transactional
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Transactional
    public EmergencyInfo addEmergencyContact(Long employeeId,
                                             String contactName,
                                             String phoneNumber,
                                             String relationship) {
        Employee employee = findById(employeeId);
        return emergencyInfoRepository.save(
                new EmergencyInfo(contactName, phoneNumber, relationship, employee));
    }
}
