package com.challenge.department_store.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;
import com.challenge.department_store.entity.SubCategory;
import com.challenge.department_store.repository.CategoryRepository;
import com.challenge.department_store.repository.InventoryRepository;
import com.challenge.department_store.repository.SubCategoryRepository;

import jakarta.persistence.EntityNotFoundException;

@Service 
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final InventoryRepository inventoryRepository;

    public CategoryService(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository, InventoryRepository inventoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.name()).ifPresent(foundCategory -> {
            //TODO: Replace DuplicateKeyException as its database level and use a dedicated business level Exception instead
            throw new DuplicateKeyException("Duplicate case-insensitive category name: " + request.name() + " and " + foundCategory.getName());
        });

        Category newCategory = new Category(request.name());
        addSubCategoryFromNames(newCategory, request.subCategories());

        return CategoryResponse.from(categoryRepository.save(newCategory));
    }

    @Transactional
    public CategoryResponse updateCategory(UpdateCategoryRequest request, String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(
            //TODO: Replace EntityNotFoundException with dedicated business logic Exception instead
            () -> new EntityNotFoundException( "Category not found: " + categoryName)
        );
        if (!isSubCategoryUpdateValid(category, request.getSubCategories())) {
            //TODO: Replace IllegalStateException with dedicated business logic Exception instead
            throw new IllegalStateException("Cannot replace sub categories in use.");
        }
        category.getSubCategories().clear();
        addSubCategoryFromNames(category, request.getSubCategories());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
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

    private boolean isSubCategoryUpdateValid(Category currentCategory, Set<String> newSubCategorieNames) {
        Set<String> currentSubCategories = getSubCategoriesNamesFromSet(currentCategory.getSubCategories());
        if (newSubCategorieNames.containsAll(currentSubCategories)) {
            return true;
        }
        
        List<Inventory> inventories = inventoryRepository.findByCategory(currentCategory);

        for (Inventory inv: inventories) {
            if (!newSubCategorieNames.containsAll(getSubCategoriesNamesFromSet(inv.getSubCategories()))) {
                return false;
            }
        }

        return true;
    }

    private Set<String> getSubCategoriesNamesFromSet (Set<SubCategory> subCategories) {
        return subCategories.stream().map(SubCategory::getName).collect(Collectors.toSet());
    }
}
