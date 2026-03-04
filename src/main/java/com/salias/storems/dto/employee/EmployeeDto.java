 package com.salias.storems.dto.employee;

import java.time.LocalDate;

public record EmployeeDto(
        Long id,
        String firstName,
        String lastName,
        LocalDate dob,
        String address,
        String role,
        Long storeId
) {}
