package com.salias.storems.dto.storemanager;

import jakarta.validation.constraints.NotNull;

public record CreateStoreManagerRequest(
        @NotNull Long storeId,
        @NotNull Long managerEmployeeId
) {}
