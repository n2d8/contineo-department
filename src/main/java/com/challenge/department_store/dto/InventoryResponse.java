package com.challenge.department_store.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;
import com.challenge.department_store.entity.SubCategory;

public record InventoryResponse(Long id, 
                                String name,
                                String shopName, 
                                Category category, 
                                Set<String> subCategories, 
                                Integer quantity, 
                                String description) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getId(),
                                     inventory.getName(), 
                                     inventory.getShopName(), 
                                     inventory.getCategory(), 
                                     inventory.getSubCategories()
                                              .stream()
                                              .map(SubCategory::getName)
                                              .collect(Collectors.toSet()), 
                                     inventory.getQuantity(), 
                                     inventory.getDescription());
    }
}
