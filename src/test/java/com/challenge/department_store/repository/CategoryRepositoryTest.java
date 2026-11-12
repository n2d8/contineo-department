package com.challenge.department_store.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.SubCategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest 
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;


    @Test
    void testSavesAndRetrievesCategoryById() {
        Category footwear = new Category("Footwear");
        footwear = categoryRepository.save(footwear);

        Category retrieveFootwear = categoryRepository.findById(footwear.getId()).orElseThrow();
        assertThat(retrieveFootwear.getName()).isEqualTo(footwear.getName());
        assertThat(retrieveFootwear.getId()).isEqualTo(footwear.getId());
    }

    @Test 
    void testReturnsEmptyForNonExistentCategoryId() {
        assertThat(categoryRepository.findById(60900L)).isEmpty();
    }

    @Test
    void testFindsCategoryByNameIgnoreCase() {
        categoryRepository.save(new Category("Footwear"));

        assertThat(categoryRepository.findByNameIgnoreCase("footwear")).isPresent();
        assertThat(categoryRepository.findByNameIgnoreCase("FOOTWEAR")).isPresent();
        assertThat(categoryRepository.findByNameIgnoreCase("FoOTwEaR")).isPresent();
    }

    @Test
    void testReturnsEmptyForUnknownCategoryName() {
        assertThat(categoryRepository.findByNameIgnoreCase("NonExistent")).isEmpty();
    }

    @Test
    void testRejectsDuplicateCategoryName() {
        categoryRepository.saveAndFlush(new Category("Footwear"));

        assertThatThrownBy(() -> 
            categoryRepository.saveAndFlush(new Category("Footwear")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testSavesCategoryWithSubCategoriesAndReadsBack() {
        Category footwear = new Category("Footwear");
        SubCategory sneakers = new SubCategory("sneakers");
        subCategoryRepository.saveAndFlush(sneakers);
        footwear.addSubCategory(sneakers);
        categoryRepository.saveAndFlush(footwear);

        Category retrieveFootwear = categoryRepository.findById(footwear.getId()).orElseThrow();
        assertThat(retrieveFootwear.getSubCategories())
            .extracting(SubCategory::getName)
            .containsExactly("sneakers");
    }
}
