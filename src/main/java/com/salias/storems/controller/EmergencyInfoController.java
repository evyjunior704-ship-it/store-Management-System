package com.salias.storems.controller;

import com.salias.storems.dto.emergencyinfo.CreateEmergencyInfoRequest;
import com.salias.storems.dto.emergencyinfo.EmergencyInfoDto;
import com.salias.storems.services.EmergencyInfoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emergency-info")
public class EmergencyInfoController {

    private final EmergencyInfoService emergencyInfoService;

    public EmergencyInfoController(EmergencyInfoService emergencyInfoService) {
        this.emergencyInfoService = emergencyInfoService;
    }

    @PostMapping
    public ResponseEntity<EmergencyInfoDto> create(@Valid @RequestBody CreateEmergencyInfoRequest request) {
        EmergencyInfoDto created = emergencyInfoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<EmergencyInfoDto> list(Pageable pageable) {
        return emergencyInfoService.list(pageable);
    }
}
