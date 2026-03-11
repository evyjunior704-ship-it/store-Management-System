package com.salias.storems.services;

import com.salias.storems.dto.emergencyinfo.CreateEmergencyInfoRequest;
import com.salias.storems.dto.emergencyinfo.EmergencyInfoDto;
import com.salias.storems.exceptions.ResourceNotFoundException;
import com.salias.storems.model.emergencyinfo.EmergencyInfo;
import com.salias.storems.model.emergencyinfo.EmergencyInfoRepository;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmergencyInfoService {

    private final EmergencyInfoRepository emergencyInfoRepository;
    private final EmployeeRepository employeeRepository;

    public EmergencyInfoService(EmergencyInfoRepository emergencyInfoRepository, EmployeeRepository employeeRepository) {
        this.emergencyInfoRepository = emergencyInfoRepository;
        this.employeeRepository = employeeRepository;
    }

    public EmergencyInfoDto create(CreateEmergencyInfoRequest req) {
        Employee employee = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + req.employeeId()));

        EmergencyInfo info = new EmergencyInfo(req.contactName(), req.phoneNumber(), req.relationship());
        info.setEmployee(employee);

        EmergencyInfo saved = emergencyInfoRepository.save(info);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<EmergencyInfoDto> list(Pageable pageable) {
        return emergencyInfoRepository.findAll(pageable).map(this::toDto);
    }

    private EmergencyInfoDto toDto(EmergencyInfo e) {
        Long employeeId = (e.getEmployee() == null) ? null : e.getEmployee().getId();
        return new EmergencyInfoDto(
                e.getEmergID(),
                e.getContactName(),
                e.getPhoneNumber(),
                e.getRelationship(),
                employeeId
        );
    }
}
