package com.salias.storems.controller;

import com.salias.storems.dto.store.CreateStoreRequest;
import com.salias.storems.dto.store.StoreDto;
import com.salias.storems.dto.store.UpdateStoreRequest;
import com.salias.storems.services.StoreService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    public ResponseEntity<StoreDto> create(@Valid @RequestBody CreateStoreRequest request) {
        StoreDto created = storeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public StoreDto getById(@PathVariable Long id) {
        return storeService.getById(id);
    }

    @GetMapping
    public Page<StoreDto> list(Pageable pageable) {
        return storeService.list(pageable);
    }

    @PutMapping("/{id}")
    public StoreDto update(@PathVariable Long id, @Valid @RequestBody UpdateStoreRequest request) {
        return storeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        storeService.delete(id);
    }
}
