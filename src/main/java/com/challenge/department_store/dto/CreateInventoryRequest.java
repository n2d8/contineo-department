package com.challenge.department_store.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateInventoryRequest(

    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank 
    @Size(max = 100)
    String shopName,

    @NotBlank 
    @Size(max = 100)
    String category,

    @NotEmpty 
    Set<@Size(max = 100) String> subCategories,
    
    @NotNull 
    @PositiveOrZero 
    Integer quantity,

    @Size(max = 500)
    String description
) {}
