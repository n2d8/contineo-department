package com.challenge.department_store.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challenge.department_store.dto.CreateInventoryRequest;
import com.challenge.department_store.dto.InventoryResponse;
import com.challenge.department_store.dto.UpdateInventoryQuantityRequest;
import com.challenge.department_store.dto.UpdateInventoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;
import com.challenge.department_store.entity.SubCategory;
import com.challenge.department_store.repository.CategoryRepository;
import com.challenge.department_store.repository.InventoryRepository;
import com.challenge.department_store.repository.SubCategoryRepository;

import jakarta.persistence.EntityNotFoundException;

@Service 
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            CategoryRepository categoryRepository,
                            SubCategoryRepository subCategoryRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findAll().stream().map(InventoryResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = getInventory(id);
        return InventoryResponse.from(inventory);
    }

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        Category category = getCategory(request.category());
        Set<SubCategory> subCategories = validateCategoryAndSubCategory(category, request.subCategories());

        inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(request.name(), request.shopName())
            .ifPresent(foundInventory -> {
                throw new DuplicateKeyException("Inventory name and shop name pair already exists.");
            });
        
        Inventory inventory = new Inventory(request.name(), 
                                            request.shopName(), 
                                            category, 
                                            request.quantity(), 
                                            request.description(),
                                            subCategories
                                        );

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse updateInventory(Long id, UpdateInventoryRequest request) {
        Inventory inventory = getInventory(id);
        inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(request.name(), request.shopName())
            .ifPresent(foundInventory -> {
                throw new DuplicateKeyException("Inventory name and shop name pair already exists.");
            });
        
        Category category = getCategory(request.category());
        Set<SubCategory> subCategories = validateCategoryAndSubCategory(category, request.subCategories());

        inventory.setCategory(category);
        inventory.setSubCategories(subCategories);
        inventory.setName(request.name());
        inventory.setShopName(request.shopName());
        inventory.setDescription(request.description());
        inventory.setQuantity(request.quantity());

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse updateInventoryQuantity(Long id, UpdateInventoryQuantityRequest request) {
        if (request.quantity() < 0) {
            //TODO: Replace IllegalArgumentException with dedicated business logic Exception instead
            throw new IllegalArgumentException("Quantity must not be negative.");
        }
        Inventory inventory = getInventory(id);
        inventory.setQuantity(request.quantity());
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    private SubCategory findSubCategoryOrElseThrow(String subCategoryName) {
        return subCategoryRepository.findByNameIgnoreCase(subCategoryName).orElseThrow(
            //TODO: Replace EntityNotFoundException with dedicated business logic Exception instead
            () -> new EntityNotFoundException("Sub category does not exist.")
        );
    }    
    private Set<SubCategory> validateCategoryAndSubCategory(Category category, Set<String> subCategories) {
        boolean isValid = true;
        Set<String> supportedSubCat = category.getSubCategories().stream().map(this::toCaseIncensitiveName).collect(Collectors.toSet());
        for (String subName: subCategories) {
            // Align case insensitive behavior as respository queries
            if (!supportedSubCat.contains(subName.toUpperCase(Locale.ROOT))) {
                isValid = false;
                break;
            }
        }
        if (!isValid) {
            throw new IllegalStateException("Sub categories is not compatible with category.");
        }

        return subCategories.stream().map(this::findSubCategoryOrElseThrow).collect(Collectors.toSet());
    }

    private String toCaseIncensitiveName(SubCategory sub) {
        return sub.getName().toUpperCase(Locale.ROOT);
    }

    private Inventory getInventory(Long id) {
        return inventoryRepository.findById(id).orElseThrow(
            //TODO: Replace EntityNotFoundException with dedicated business logic Exception instead
            () -> new EntityNotFoundException( "Inventory not found: " + id)
        );
    }

    private Category getCategory(String categoryName) {
        return categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(
            //TODO: Replace EntityNotFoundException with dedicated business logic Exception instead
            () -> new EntityNotFoundException("Category does not exist.")
        );
    }
}