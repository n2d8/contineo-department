package com.challenge.department_store.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;
import com.challenge.department_store.entity.SubCategory;
import com.challenge.department_store.exception.DuplicateEntityException;
import com.challenge.department_store.exception.IllegalOperationException;
import com.challenge.department_store.exception.NotFoundException;
import com.challenge.department_store.repository.CategoryRepository;
import com.challenge.department_store.repository.InventoryRepository;
import com.challenge.department_store.repository.SubCategoryRepository;

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
            throw new DuplicateEntityException("Category already exists: " + foundCategory.getName());
        });

        Category newCategory = new Category(request.name());
        addSubCategoryFromNames(newCategory, request.subCategories());

        return CategoryResponse.from(categoryRepository.save(newCategory));
    }

    @Transactional
    public CategoryResponse updateCategory(UpdateCategoryRequest request, String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(
            () -> new NotFoundException( "Category not found: " + categoryName)
        );
        if (!isSubCategoryUpdateValid(category, request.getSubCategories())) {
            throw new IllegalOperationException("Cannot replace sub-categories in use.");
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
            () -> new NotFoundException( "Category not found: " + categoryName)
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
        Set<String> normalizedNewSubCategoryNames = normalizeNames(newSubCategorieNames);
        Set<String> currentSubCategories = getNormalizedSubCategoryNames(currentCategory.getSubCategories());
        if (normalizedNewSubCategoryNames.containsAll(currentSubCategories)) {
            return true;
        }
        
        List<Inventory> inventories = inventoryRepository.findByCategory(currentCategory);

        for (Inventory inv: inventories) {
            if (!normalizedNewSubCategoryNames.containsAll(
                    getNormalizedSubCategoryNames(inv.getSubCategories()))) {
                return false;
            }
        }

        return true;
    }

    private Set<String> getNormalizedSubCategoryNames(Set<SubCategory> subCategories) {
        return subCategories.stream()
                .map(SubCategory::getName)
                .map(name -> name.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<String> normalizeNames(Set<String> names) {
        return names.stream().map(name -> name.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    }
}
