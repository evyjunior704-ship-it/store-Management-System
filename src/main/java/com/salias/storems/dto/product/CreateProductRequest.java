package com.salias.storems.dto.product;

import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(
        @NotBlank String name,
        String manufacturer,
        @NotBlank String description
) {}
