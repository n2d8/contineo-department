package com.challenge.department_store.entity;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryTest {

    private Category category;
    private Inventory inventory;

    @BeforeEach 
    void setUp() {
        category = new Category("Footwear");
        inventory = new Inventory(
            "Air Jordan 1",
            "Jordan",
            category,
            10,
            "High-top basketball shoe",
            new HashSet<SubCategory>()
        );
    }

    @Test
    void testConstructorSetsAllFields() {
        assertThat(inventory.getName()).isEqualTo("Air Jordan 1");
        assertThat(inventory.getShopName()).isEqualTo("Jordan");
        assertThat(inventory.getCategory().getName()).isEqualTo(category.getName());
        assertThat(inventory.getQuantity()).isEqualTo(10);
        assertThat(inventory.getDescription()).isEqualTo("High-top basketball shoe");
        assertThat(inventory.getSubCategories()).hasSize(0);
    }

    @Test
    void testSettersUpdateFields() {
        String newInventoryName = "Nike Dunk";
        String newShopName = "Nike";
        int newQuantity = 20;
        String newDescription = "Low-top basketball shoe";

        inventory.setName(newInventoryName);
        inventory.setShopName(newShopName);
        inventory.setQuantity(newQuantity);
        inventory.setDescription(newDescription);

        assertThat(inventory.getName()).isEqualTo(newInventoryName);
        assertThat(inventory.getShopName()).isEqualTo(newShopName);
        assertThat(inventory.getQuantity()).isEqualTo(newQuantity);
        assertThat(inventory.getDescription()).isEqualTo(newDescription);
    }

    @Test 
    void testCategoryCanBeReplaced() {
        Category newCategory = new Category();
        assertThat(inventory.getCategory()).isNotEqualTo(newCategory);
        inventory.setCategory(newCategory);
        assertThat(inventory.getCategory()).isEqualTo(newCategory);
    }

    @Test
    void testSubCategoriesCanBeReplaced() {
        Set<SubCategory> newSubCategories = new HashSet<>();
        newSubCategories.add(new SubCategory());
        assertThat(inventory.getSubCategories()).isNotEqualTo(newSubCategories);
        inventory.setSubCategories(newSubCategories);
        assertThat(inventory.getSubCategories()).isEqualTo(newSubCategories);
    }

    @Test
    void testIdIsNull() {
        assertThat(inventory.getId()).isNull();
    }

}
