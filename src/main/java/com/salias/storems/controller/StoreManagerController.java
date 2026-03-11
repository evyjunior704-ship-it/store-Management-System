package com.salias.storems.controller;

import com.salias.storems.dto.storemanager.CreateStoreManagerRequest;
import com.salias.storems.dto.storemanager.StoreManagerDto;
import com.salias.storems.services.StoreManagerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store-managers")
public class StoreManagerController {

    private final StoreManagerService storeManagerService;

    public StoreManagerController(StoreManagerService storeManagerService) {
        this.storeManagerService = storeManagerService;
    }

    @PostMapping
    public ResponseEntity<StoreManagerDto> create(@Valid @RequestBody CreateStoreManagerRequest request) {
        StoreManagerDto created = storeManagerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{storeId}")
    public StoreManagerDto update(@PathVariable Long storeId, @Valid @RequestBody CreateStoreManagerRequest request) {
        return storeManagerService.update(storeId, request);
    }

    @GetMapping("/{storeId}")
    public StoreManagerDto getByStoreId(@PathVariable Long storeId) {
        return storeManagerService.getByStoreId(storeId);
    }

    @GetMapping
    public Page<StoreManagerDto> list(Pageable pageable) {
        return storeManagerService.list(pageable);
    }

    @DeleteMapping("/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long storeId) {
        storeManagerService.delete(storeId);
    }
}
