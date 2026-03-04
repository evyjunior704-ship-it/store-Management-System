package com.salias.storems.dto.emergencyinfo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmergencyInfoRequest(
        @NotBlank String contactName,
        @NotBlank String phoneNumber,
        @NotBlank String relationship,
        @NotNull Long employeeId
) {}
