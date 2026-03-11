package com.salias.storems.services;

import com.salias.storems.dto.store.CreateStoreRequest;
import com.salias.storems.dto.store.StoreDto;
import com.salias.storems.dto.store.UpdateStoreRequest;
import com.salias.storems.exceptions.ResourceNotFoundException;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StoreService {
    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public StoreDto create(CreateStoreRequest req) {
        Store store = new Store(req.name(), req.address());
        Store saved = storeRepository.save(store);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public StoreDto getById(Long id) {
        return storeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<StoreDto> list(Pageable pageable) {
        return storeRepository.findAll(pageable).map(this::toDto);
    }

    public StoreDto update(Long id, UpdateStoreRequest req) {
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));

        existing.setStoreName(req.name());
        existing.setStoreAddress(req.address());

        Store saved = storeRepository.save(existing);
        return toDto(saved);
    }

    public void delete(Long id) {
        if (!storeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Store not found: " + id);
        }
        storeRepository.deleteById(id);
    }

    private StoreDto toDto(Store s) {
        return new StoreDto(s.getStoreID(), s.getStoreName(), s.getStoreAddress());
    }
}
