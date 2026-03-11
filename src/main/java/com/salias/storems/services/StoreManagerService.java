package com.salias.storems.services;

import com.salias.storems.dto.storemanager.CreateStoreManagerRequest;
import com.salias.storems.dto.storemanager.StoreManagerDto;
import com.salias.storems.exceptions.ResourceNotFoundException;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.model.storemanager.StoreManager;
import com.salias.storems.model.storemanager.StoreManagerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StoreManagerService {

    private final StoreManagerRepository storeManagerRepository;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;

    public StoreManagerService(StoreManagerRepository storeManagerRepository,
                               StoreRepository storeRepository,
                               EmployeeRepository employeeRepository) {
        this.storeManagerRepository = storeManagerRepository;
        this.storeRepository = storeRepository;
        this.employeeRepository = employeeRepository;
    }

    public StoreManagerDto create(CreateStoreManagerRequest request) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId()));
        Employee manager = employeeRepository.findById(request.managerEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.managerEmployeeId()));

        StoreManager mapping = new StoreManager(store, manager);
        StoreManager saved = storeManagerRepository.save(mapping);
        return toDto(saved);
    }

    public StoreManagerDto update(Long storeId, CreateStoreManagerRequest request) {
        StoreManager existing = storeManagerRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store manager mapping not found for store: " + storeId));

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId()));
        Employee manager = employeeRepository.findById(request.managerEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.managerEmployeeId()));

        existing.setStoreID(store);
        existing.setManagerID(manager);
        StoreManager saved = storeManagerRepository.save(existing);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public StoreManagerDto getByStoreId(Long storeId) {
        return storeManagerRepository.findById(storeId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Store manager mapping not found for store: " + storeId));
    }

    @Transactional(readOnly = true)
    public Page<StoreManagerDto> list(Pageable pageable) {
        return storeManagerRepository.findAll(pageable).map(this::toDto);
    }

    public void delete(Long storeId) {
        if (!storeManagerRepository.existsById(storeId)) {
            throw new ResourceNotFoundException("Store manager mapping not found for store: " + storeId);
        }
        storeManagerRepository.deleteById(storeId);
    }

    private StoreManagerDto toDto(StoreManager storeManager) {
        Long sid = storeManager.getStoreID() == null ? null : storeManager.getStoreID().getStoreID();
        Long mid = storeManager.getManagerID() == null ? null : storeManager.getManagerID().getId();
        return new StoreManagerDto(sid, mid);
    }
}
