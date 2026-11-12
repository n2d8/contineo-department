package com.challenge.department_store.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    private Category footwear;
    private Category shoe;
    private SubCategory cleats;

    @BeforeEach 
    void setUp() {
        footwear = new Category("Footwear");
        shoe = new Category("Shoe");
        cleats = new SubCategory("cleats");
    }

    @Test 
    void testCreateCategoryWithName() {
        assertThat(footwear.getName()).isEqualTo("Footwear");
        assertThat(shoe.getName()).isEqualTo("Shoe");
    }

    @Test
    void testCreatedCategoryHasNullIdBeforePersistence() {
        assertThat(footwear.getId()).isNull();
        assertThat(shoe.getId()).isNull();
    }

    @Test 
    void testCreatedCategoryHasEmptySubCategories() {
        assertThat(footwear.getSubCategories()).isEmpty();
        assertThat(shoe.getSubCategories()).isEmpty();
    }

    @Test 
    void testAddSubCategoryLinksBothCategoryAndSubCategory() {
        footwear.addSubCategory(cleats);
        assertThat(footwear.getSubCategories()).contains(cleats);
        shoe.addSubCategory(cleats);
        assertThat(shoe.getSubCategories()).contains(cleats);
        assertThat(cleats.getCategories()).contains(footwear);
        assertThat(cleats.getCategories()).contains(shoe);
        assertThat(cleats.getCategories()).hasSize(2);  
    }

    @Test 
    void testAddSubCategoryIsIdempotent() {
        footwear.addSubCategory(cleats);
        footwear.addSubCategory(cleats);
        assertThat(footwear.getSubCategories()).hasSize(1);
        assertThat(cleats.getCategories()).hasSize(1);
    }
}
