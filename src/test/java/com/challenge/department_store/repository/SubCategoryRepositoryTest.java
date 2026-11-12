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
class SubCategoryRepositoryTest {

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testSavesAndRetrievesSubCategoryById() {
        SubCategory sneakers = new SubCategory("Sneakers");
        sneakers = subCategoryRepository.save(sneakers);

        SubCategory retrievedSneakers = subCategoryRepository.findById(sneakers.getId()).orElseThrow();
        assertThat(retrievedSneakers.getName()).isEqualTo(sneakers.getName());
        assertThat(retrievedSneakers.getId()).isEqualTo(sneakers.getId());
    }

    @Test 
    void testReturnsEmptyForNonExistentSubCategoryId() {
        assertThat(subCategoryRepository.findById(60900L)).isEmpty();
    }

    @Test
    void testFindsSubCategoryByNameIgnoreCase() {
        subCategoryRepository.save(new SubCategory("Sneakers"));

        assertThat(subCategoryRepository.findByNameIgnoreCase("sneakers")).isPresent();
        assertThat(subCategoryRepository.findByNameIgnoreCase("SNEAKERS")).isPresent();
        assertThat(subCategoryRepository.findByNameIgnoreCase("SnEaKErS")).isPresent();
    }

    @Test
    void testReturnsEmptyForUnknownSubCategoryName() {
        assertThat(subCategoryRepository.findByNameIgnoreCase("NonExistent")).isEmpty();
    }

    @Test
    void testRejectsDuplicateSubCategoryName() {
        subCategoryRepository.saveAndFlush(new SubCategory("Sneakers"));

        assertThatThrownBy(() -> 
            subCategoryRepository.saveAndFlush(new SubCategory("Sneakers")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testSavesSubCategoryLinkedToMultipleCategories() {
        Category footwear = new Category("Footwear");
        Category shoe = new Category("Shoe");
        SubCategory sneakers = new SubCategory("Sneakers");

        subCategoryRepository.saveAndFlush(sneakers);
        footwear.addSubCategory(sneakers);
        shoe.addSubCategory(sneakers);
        categoryRepository.saveAndFlush(footwear);
        categoryRepository.saveAndFlush(shoe);

        SubCategory retrievedSneakers = subCategoryRepository.findById(sneakers.getId()).orElseThrow();
        assertThat(retrievedSneakers.getCategories()).hasSize(2);
    }
}
