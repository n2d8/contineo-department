// CategoryServiceTest.java
package com.challenge.department_store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.SubCategory;
import com.challenge.department_store.repository.CategoryRepository;
import com.challenge.department_store.repository.SubCategoryRepository;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void testCreatesCategoryWithSubCategories() {
        when(categoryRepository.findByNameIgnoreCase("Footwear")).thenReturn(Optional.empty());
        when(subCategoryRepository.findByNameIgnoreCase("Sneakers"))
            .thenReturn(Optional.of(new SubCategory("Sneakers")));
        when(subCategoryRepository.findByNameIgnoreCase("Cleats"))
            .thenReturn(Optional.of(new SubCategory("Cleats")));
        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateCategoryRequest request = new CreateCategoryRequest(
            "Footwear", Set.of("Sneakers","Cleats"));

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.getName()).isEqualTo("Footwear");
        assertThat(response.getSubCategories()).containsExactlyInAnyOrder("Sneakers","Cleats");
    }


    @Test
    void testCreatesCategoryWithoutSubCategories() {
        when(categoryRepository.findByNameIgnoreCase("Footwear")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateCategoryRequest request = new CreateCategoryRequest(
            "Footwear", Set.of());

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.getName()).isEqualTo("Footwear");
        assertThat(response.getSubCategories()).isEmpty();
    }

    @Test
    void testRejectsDuplicateCategoryNameCaseInsensitive() {
        when(categoryRepository.findByNameIgnoreCase("FOOTWEAR"))
            .thenReturn(Optional.of(new Category("Footwear")));

        assertThatThrownBy(() ->
            categoryService.createCategory(new CreateCategoryRequest("FOOTWEAR", Set.of())))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test 
    void testUpdateWithSubCategories() {
        Category footwear = new Category("Footwear");
        footwear.addSubCategory(new SubCategory("Sneakers"));
        footwear.addSubCategory(new SubCategory("Cleats"));
        when(categoryRepository.findByNameIgnoreCase("Footwear"))
            .thenReturn(Optional.of(footwear));
        when(subCategoryRepository.findByNameIgnoreCase("Cleats"))
            .thenReturn(Optional.of(new SubCategory("Cleats")));
        when(subCategoryRepository.findByNameIgnoreCase("Sneakers"))
            .thenReturn(Optional.of(new SubCategory("Sneakers")));
        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCategoryRequest request = new UpdateCategoryRequest(
            Set.of("Cleats"));
        CategoryResponse response = categoryService.updateCategory(request, "Footwear");
        assertThat(response.getName()).isEqualTo("Footwear");
        assertThat(response.getSubCategories()).containsExactly("Cleats");
        request = new UpdateCategoryRequest(
            Set.of("Sneakers"));
        response = categoryService.updateCategory(request, "Footwear");
        assertThat(response.getSubCategories()).containsExactly("Sneakers");
        request = new UpdateCategoryRequest(
            Set.of("Sneakers", "Cleats"));
        response = categoryService.updateCategory(request, "Footwear");
        assertThat(response.getSubCategories()).containsExactlyInAnyOrder("Sneakers", "Cleats");
    }

    @Test 
    void testUpdateWithNonExistentCategoryName() {
        when(categoryRepository.findByNameIgnoreCase("WrongCatName"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            categoryService.updateCategory(new UpdateCategoryRequest(Set.of("Cleats")), "WrongCatName"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test 
    void testGetAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(categoryWithSubCategories()));

        List<CategoryResponse> categoriesResponse = categoryService.getAllCategories();
        assertThat(categoriesResponse).hasSize(1);
        assertThat(categoriesResponse.get(0).getName()).isEqualTo("Footwear");
    }

    @Test 
    void testGetCategoryByNameWithCategoryName() {
        when(categoryRepository.findByNameIgnoreCase("Footwear"))
            .thenReturn(Optional.of(categoryWithSubCategories()));

        CategoryResponse response = categoryService.getCategoryByName("Footwear");
        assertThat(response.getName()).isEqualTo("Footwear");
        assertThat(response.getSubCategories()).containsExactlyInAnyOrder("Sneakers", "Cleats");
    }

    @Test 
    void testGetCategoryByNameWithNonExistentName() {
        when(categoryRepository.findByNameIgnoreCase("WrongCatName"))
            .thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            categoryService.getCategoryByName("WrongCatName"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    private Category categoryWithSubCategories() {
        Category footwear = new Category("Footwear");
        footwear.addSubCategory(new SubCategory("Sneakers"));
        footwear.addSubCategory(new SubCategory("Cleats"));
        return footwear;
    }
}