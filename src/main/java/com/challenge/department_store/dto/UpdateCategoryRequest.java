package com.challenge.department_store.dto;

import java.util.Set;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(Set<@Size(max = 100) String> subCategories) {

    public Set<String> getSubCategories() {
        return subCategories;
    }}
