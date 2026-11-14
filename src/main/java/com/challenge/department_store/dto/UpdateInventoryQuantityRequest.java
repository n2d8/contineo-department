package com.challenge.department_store.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateInventoryQuantityRequest(
    @NotNull 
    @PositiveOrZero 
    Integer quantity
) {}
