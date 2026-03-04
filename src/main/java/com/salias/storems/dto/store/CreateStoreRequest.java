package com.salias.storems.dto.store;

import jakarta.validation.constraints.NotBlank;

public record CreateStoreRequest(
        @NotBlank String name,
        @NotBlank String address
) {}
