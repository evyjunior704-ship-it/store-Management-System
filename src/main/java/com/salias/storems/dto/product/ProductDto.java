package com.salias.storems.dto.product;

public record ProductDto(
        Long id,
        String name,
        String manufacturer,
        String description
) {}
