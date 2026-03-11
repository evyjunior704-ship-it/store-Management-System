package com.salias.storems.services;

import com.salias.storems.dto.employee.CreateEmployeeRequest;
import com.salias.storems.dto.employee.EmployeeDto;
import com.salias.storems.dto.employee.UpdateEmployeeRequest;
import com.salias.storems.exceptions.ResourceNotFoundException;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;

    public EmployeeService(EmployeeRepository employeeRepository, StoreRepository storeRepository) {
        this.employeeRepository = employeeRepository;
        this.storeRepository = storeRepository;
    }

    public EmployeeDto create(CreateEmployeeRequest req) {
        Store store = storeRepository.findById(req.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + req.storeId()));

        Employee employee = new Employee(
                req.firstName(),
                req.lastName(),
                req.dob(),
                req.address(),
                req.role(),
                store
        );

        Employee saved = employeeRepository.save(employee);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeDto getById(Long id) {
        return employeeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDto> list(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(this::toDto);
    }

    public EmployeeDto update(Long id, UpdateEmployeeRequest req) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));

        Store store = storeRepository.findById(req.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + req.storeId()));

        existing.setEmpFirstName(req.firstName());
        existing.setEmpLastName(req.lastName());
        existing.setEmpDOB(req.dob());
        existing.setEmpAddress(req.address());
        existing.setEmpRole(req.role());
        existing.setStoreAssociatedTo(store);

        Employee saved = employeeRepository.save(existing);
        return toDto(saved);
    }

    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found: " + id);
        }
        employeeRepository.deleteById(id);
    }

    private EmployeeDto toDto(Employee e) {
        Long storeId = (e.getStoreAssociatedTo() == null) ? null : e.getStoreAssociatedTo().getStoreID();
        return new EmployeeDto(
                e.getId(),
                e.getEmpFirstName(),
                e.getEmpLastName(),
                e.getEmpDOB(),
                e.getEmpAddress(),
                e.getEmpRole(),
                storeId
        );
    }
}
