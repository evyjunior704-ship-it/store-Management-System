package com.salias.storems.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull LocalDate dob,
        @NotBlank String address,
        @NotBlank String role,
        @NotNull Long storeId
) {}
