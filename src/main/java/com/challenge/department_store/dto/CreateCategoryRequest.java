package com.challenge.department_store.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank 
    @Size(max = 100)
    String name, 
    
    @Size(max = 100)
    Set<@Size(max = 100) String> subCategories) {}
