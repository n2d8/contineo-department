package com.challenge.department_store.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challenge.department_store.dto.CreateInventoryRequest;
import com.challenge.department_store.dto.InventoryResponse;
import com.challenge.department_store.dto.UpdateInventoryQuantityRequest;
import com.challenge.department_store.dto.UpdateInventoryRequest;
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
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    private final Logger logger = LoggerFactory.getLogger(InventoryService.class);

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

        ifInventoryExists(request.name(), request.shopName());
        
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
        Inventory inventory = getInventoryForUpdate(id);
        
        if (!(inventory.getName().equalsIgnoreCase(request.name()) && 
                inventory.getShopName().equalsIgnoreCase(request.shopName()))) {
            
            ifInventoryExists(request.name(), request.shopName());
        }

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
            throw new IllegalOperationException("Quantity must not be negative.");
        }
        Inventory inventory = getInventoryForUpdate(id);
        inventory.setQuantity(request.quantity());
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Transactional 
    public void deleteInventory(Long id) {
        logger.debug("Deleting inventory with id: {}", id);
        
        Inventory inventory = getInventory(id);
        inventoryRepository.delete(inventory);

        logger.debug("Succssfully deleted inventory - id: {}, name: {}", inventory.getId(), inventory.getName());
    }

    private SubCategory findSubCategoryOrElseThrow(String subCategoryName) {
        return subCategoryRepository.findByNameIgnoreCase(subCategoryName).orElseThrow(
            () -> new NotFoundException("Sub-category does not exist.")
        );
    }    
    private Set<SubCategory> validateCategoryAndSubCategory(Category category, Set<String> subCategories) {
        boolean isValid = true;
        Set<String> supportedSubCat = category.getSubCategories().stream().map(this::toCaseIncensitiveName).collect(Collectors.toSet());
        String invalidSubCategory = null;
        for (String subName: subCategories) {
            // Align case insensitive behavior as respository queries
            if (!supportedSubCat.contains(subName.toUpperCase(Locale.ROOT))) {
                isValid = false;
                invalidSubCategory = subName;
                break;
            }
        }
        if (!isValid) {
            if (invalidSubCategory != null) {
                subCategoryRepository.findByNameIgnoreCase(invalidSubCategory).ifPresent(
                    foundCategory -> {
                        throw new IllegalOperationException("Sub-categories are not compatible with category.");
                    }
                );
            }
            throw new NotFoundException("Some of the sub-categories does not exist.");
        }

        return subCategories.stream().map(this::findSubCategoryOrElseThrow).collect(Collectors.toSet());
    }

    private String toCaseIncensitiveName(SubCategory sub) {
        return sub.getName().toUpperCase(Locale.ROOT);
    }

    private Inventory getInventory(Long id) {
        return inventoryRepository.findById(id).orElseThrow(
            () -> new NotFoundException( "Inventory not found: " + id)
        );
    }

    private Inventory getInventoryForUpdate(Long id) {
        return inventoryRepository.findByIdForUpdate(id).orElseThrow(
            () -> new NotFoundException("Inventory not found: " + id)
        );
    }

    private void ifInventoryExists(String categoryName, String shopName) {
        inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(categoryName, shopName)
            .ifPresent(foundInventory -> {
                throw new DuplicateEntityException(
                    "Inventory already exists: " + foundInventory.getName() + " - " + foundInventory.getShopName()
                );
            });
    }

    private Category getCategory(String categoryName) {
        return categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(
            () -> new NotFoundException("Category does not exist: " + categoryName)
        );
    }
}