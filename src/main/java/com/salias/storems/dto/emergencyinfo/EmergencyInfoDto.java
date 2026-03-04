package com.salias.storems.dto.emergencyinfo;

public record EmergencyInfoDto(
        Long id,
        String contactName,
        String phoneNumber,
        String relationship,
        Long employeeId
) {}
