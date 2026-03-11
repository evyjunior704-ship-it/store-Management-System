package com.salias.storems.dto.store;

import jakarta.validation.constraints.NotBlank;

public record UpdateStoreRequest(
        @NotBlank String name,
        @NotBlank String address
) {
}
