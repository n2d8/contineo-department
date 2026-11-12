package com.challenge.department_store.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;   

class SubCategoryTest {
    
    private SubCategory cleats;
    private SubCategory trainers;

    @BeforeEach 
    void setUp() {
        trainers = new SubCategory("trainers");
        cleats = new SubCategory("cleats");
    }

    @Test 
    void testCreateSubCategoryWithName() {
        assertThat(trainers.getName()).isEqualTo("trainers");
        assertThat(cleats.getName()).isEqualTo("cleats");
    }

    @Test
    void testCreatedSubCategoryHasNullIdBeforePersistence() {
        assertThat(trainers.getId()).isNull();
        assertThat(cleats.getId()).isNull();
    }

    @Test 
    void testCreatedSubCategoryHasEmptyCategories() {
        assertThat(trainers.getCategories()).isEmpty();
        assertThat(cleats.getCategories()).isEmpty();
    }

    @Test 
    void testAddSubCategoryLinksBothCategoryAndSubCategory() {
        Category footwear = new Category("Footwear");
        footwear.addSubCategory(cleats);
        footwear.addSubCategory(trainers);
        assertThat(cleats.getCategories()).hasSize(1);
        assertThat(trainers.getCategories()).hasSize(1);
        assertThat(cleats.getCategories()).contains(footwear);
        assertThat(trainers.getCategories()).contains(footwear);
    }
}
