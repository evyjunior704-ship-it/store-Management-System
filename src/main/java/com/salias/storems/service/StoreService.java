package com.salias.storems.service;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.model.storemanager.StoreManager;
import com.salias.storems.model.storemanager.StoreManagerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Store records and the manager assigned to each store.
 */
@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreManagerRepository storeManagerRepository;

    public StoreService(StoreRepository storeRepository,
                        EmployeeRepository employeeRepository,
                        StoreManagerRepository storeManagerRepository) {
        this.storeRepository = storeRepository;
        this.employeeRepository = employeeRepository;
        this.storeManagerRepository = storeManagerRepository;
    }

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    public Store findById(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("No store with id " + storeId));
    }

    public List<Employee> findStaff(Long storeId) {
        return employeeRepository.findByStoreAssociatedTo_StoreId(storeId);
    }

    @Transactional
    public Store create(String storeName, String storeAddress) {
        return storeRepository.save(new Store(storeName, storeAddress));
    }

    /**
     * Makes {@code employeeId} the manager of {@code storeId}, replacing the
     * current manager if there is one. The employee must already work at that
     * store.
     */
    @Transactional
    public StoreManager assignManager(Long storeId, Long employeeId) {
        Store store = findById(storeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + employeeId));

        if (!store.equals(employee.getStoreAssociatedTo())) {
            throw new IllegalArgumentException(
                    "Employee %d does not work at store %d".formatted(employeeId, storeId));
        }

        StoreManager assignment = storeManagerRepository.findById(storeId)
                .orElseGet(() -> new StoreManager(store, employee));
        assignment.setManager(employee);
        return storeManagerRepository.save(assignment);
    }
}
