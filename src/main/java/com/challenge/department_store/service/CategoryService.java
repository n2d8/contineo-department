package com.challenge.department_store.service;

import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.SubCategory;
import com.challenge.department_store.repository.CategoryRepository;
import com.challenge.department_store.repository.SubCategoryRepository;

import jakarta.persistence.EntityNotFoundException;

@Service 
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public CategoryService(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(foundCategory -> {
            //TODO: Replace DuplicateKeyException as its database level and use a dedicated business level Exception instead
            throw new DuplicateKeyException("Duplicate case-insensitive category name: " + request.getName() + " and " + foundCategory.getName());
        });

        Category newCategory = new Category(request.getName());
        addSubCategoryFromNames(newCategory, request.getSubCategories());

        return CategoryResponse.from(categoryRepository.save(newCategory));
    }

    public CategoryResponse updateCategory(UpdateCategoryRequest request, String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(
            //TODO: Replace NameNotFoundException with dedicated business logic Exception instead
            () -> new EntityNotFoundException( "Category not found: " + categoryName)
        );
        category.getSubCategories().clear();
        addSubCategoryFromNames(category, request.getSubCategories());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse getCategoryByName(String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(
            //TODO: Replace NameNotFoundException with dedicated business logic Exception instead
            () -> new EntityNotFoundException( "Category not found: " + categoryName)
        );
        return CategoryResponse.from(category);
    }

    private void addSubCategoryFromNames(Category categoryToAdd, Set<String> subCategoryNames) {
        for (String subCategoryName: subCategoryNames) {
            SubCategory sub = subCategoryRepository.findByNameIgnoreCase(subCategoryName)
                                            .orElseGet(() -> subCategoryRepository.save(new SubCategory(subCategoryName)));
            categoryToAdd.addSubCategory(sub);
        }
    }
}
