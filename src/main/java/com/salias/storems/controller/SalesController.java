package com.salias.storems.controller;

import com.salias.storems.dto.sales.CreateSaleRequest;
import com.salias.storems.dto.sales.SalesDto;
import com.salias.storems.services.SalesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    public ResponseEntity<SalesDto> create(@Valid @RequestBody CreateSaleRequest request) {
        SalesDto created = salesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<SalesDto> list(Pageable pageable) {
        return salesService.list(pageable);
    }
}
