package com.challenge.department_store.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.SubCategory;

public record CategoryResponse(Long id, String name, Set<String> subCategories) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), 
                                    category.getName(), 
                                    category.getSubCategories()
                                            .stream()
                                            .map(SubCategory::getName)
                                            .collect(Collectors.toSet()));
    }
}
