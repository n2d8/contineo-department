package com.challenge.department_store.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;
import com.challenge.department_store.entity.SubCategory;

@DataJpaTest 
class InventoryRepositoryTest {

    @Autowired 
    private InventoryRepository inventoryRepository;

    @Autowired 
    private CategoryRepository categoryRepository;

    @Autowired 
    private SubCategoryRepository subCategoryRepository;

    private Inventory inventory;

    @BeforeEach 
    void setUp() {
        Category cat = new Category("basketball shoes");
        categoryRepository.save(cat);
            inventory = new Inventory(
                "Air Jordan 1",
                "Jordan",
                cat,
                10,
                "High-top basketball shoe",
                new HashSet<>()
        );
    }

    @Test 
    void testSaveAndRetrieveInventoryById() {
        assertThat(inventory.getId()).isNull();
        SubCategory sc = new SubCategory("Sub Category Name");
        subCategoryRepository.saveAndFlush(sc);
        inventory.getSubCategories().add(sc);
        inventory = inventoryRepository.save(inventory);
        
        Inventory retrieveInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        validateInventories(inventory, retrieveInventory);
    }

    @Test 
    void testReturnEmptyForNonExistentInventoryId() {
        assertThat(inventoryRepository.findById(8632L)).isEmpty();
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCase() {
        inventoryRepository.saveAndFlush(inventory);
        Inventory retieveInventory = inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(inventory.getName(), inventory.getShopName()).orElseThrow();
        validateInventories(inventory, retieveInventory);
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCaseReturnsInventoryWhenNameCaseDiffers() {
        inventoryRepository.saveAndFlush(inventory);

        Optional<Inventory> foundInventory = inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        "AIR JORDAN 1",
                        inventory.getShopName()
                );

        assertThat(inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        "AIR JORDAN 1",
                        inventory.getShopName()
                )).isPresent();
        validateInventories(inventory, foundInventory.get());
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCaseReturnsInventoryWhenShopNameCaseDiffers() {
        inventoryRepository.saveAndFlush(inventory);

        Optional<Inventory> foundInventory = inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        inventory.getName(),
                        "JORDAN"
                );

        assertThat(foundInventory).isPresent();
        validateInventories(inventory, foundInventory.get());
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCaseReturnsInventoryWhenBothCasesDiffer() {
        inventoryRepository.saveAndFlush(inventory);

        Optional<Inventory> foundInventory = inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        "AIR JORDAN 1",
                        "JORDAN"
                );

        assertThat(foundInventory).isPresent();
        validateInventories(inventory, foundInventory.get());
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCaseReturnsEmptyForUnknownInventory() {
        assertThat(inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        "Unknown Inventory",
                        "Unknown Shop"
                )).isEmpty();
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCaseReturnsEmptyWhenShopNameDoesNotMatch() {
        inventoryRepository.saveAndFlush(inventory);

        Optional<Inventory> foundInventory = inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        inventory.getName(),
                        "Unknown Shop"
                );

        assertThat(foundInventory).isEmpty();
    }

    @Test
    void testFindByNameIgnoreCaseAndShopNameIgnoreCaseReturnsEmptyWhenNameDoesNotMatch() {
        inventoryRepository.saveAndFlush(inventory);

        Optional<Inventory> foundInventory = inventoryRepository
                .findByNameIgnoreCaseAndShopNameIgnoreCase(
                        "Unknown Inventory",
                        inventory.getShopName()
                );

        assertThat(foundInventory).isEmpty();
    }

    @Test 
    void testReturnEmptyForNonExistentInventoryFoundByNameIgnoreCaseAndShopIgnoreCase() {
        assertThat(inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase("inventory name", "shop name")).isEmpty();
    }

    @Test
    void testRejectsDuplicateInventoryNameAndShopName() {
        inventoryRepository.saveAndFlush(inventory);
        Category cat = new Category("basketball shoes 2");
        categoryRepository.saveAndFlush(cat);

        Inventory duplicateInventory = new Inventory(
            "Air Jordan 1",
            "Jordan",
            cat,
            5,
            "Duplicate record",
            new HashSet<>()
        );

        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(duplicateInventory))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testFindByCategoryReturnsInventoriesForCategory() {
        inventoryRepository.saveAndFlush(inventory);

        Inventory secondInventory = new Inventory(
                "Air Jordan 2",
                "Jordan",
                categoryRepository.findAll().get(0),
                5,
                "Basketball shoe",
                new HashSet<>()
        );

        Inventory savedSecondInventory = inventoryRepository.saveAndFlush(secondInventory);
        Category category = inventory.getCategory();
        List<Inventory> foundInventories = inventoryRepository.findByCategory(category);
        assertThat(foundInventories)
                .hasSize(2)
                .extracting(Inventory::getId)
                .containsExactlyInAnyOrder(
                        inventory.getId(),
                        savedSecondInventory.getId()
                );
    }

    @Test
    void testFindByCategoryReturnsEmptyListWhenCategoryHasNoInventories() {
        Category emptyCategory = categoryRepository.save(new Category("empty category"));
        List<Inventory> foundInventories = inventoryRepository.findByCategory(emptyCategory);
        assertThat(foundInventories).isEmpty();
    }

    @Test
    void testFindByCategoryReturnsInventoryWithSubCategories() {
        SubCategory subCategory = new SubCategory("basketball shoes");
        subCategoryRepository.saveAndFlush(subCategory);
        inventory.getSubCategories().add(subCategory);
        inventoryRepository.saveAndFlush(inventory);
        List<Inventory> foundInventories = inventoryRepository.findByCategory(inventory.getCategory());
        assertThat(foundInventories).hasSize(1);

        Inventory foundInventory = foundInventories.get(0);
        assertThat(foundInventory.getSubCategories()).extracting(SubCategory::getName).containsExactly("basketball shoes");
    }

    @Test
    void testFindByCategoryReturnsMultipleInventoriesForSameCategory() {
        inventoryRepository.saveAndFlush(inventory);

        Inventory secondInventory = new Inventory(
                "Air Jordan 2",
                "Jordan",
                inventory.getCategory(),
                5,
                "Basketball shoe",
                new HashSet<>()
        );

        inventoryRepository.saveAndFlush(secondInventory);
        List<Inventory> foundInventories = inventoryRepository.findByCategory(inventory.getCategory());
        assertThat(foundInventories)
                .hasSize(2)
                .extracting(Inventory::getName)
                .containsExactlyInAnyOrder("Air Jordan 1", "Air Jordan 2");
    }

    @Test
    void testFindByCategoryReturnsInventoryWithCorrectCategory() {
        inventoryRepository.saveAndFlush(inventory);
        List<Inventory> foundInventories = inventoryRepository.findByCategory(inventory.getCategory());
        assertThat(foundInventories).hasSize(1);
        Inventory foundInventory = foundInventories.get(0);
        assertThat(foundInventory.getCategory().getId()).isEqualTo(inventory.getCategory().getId());
    }

    private void validateInventories(Inventory expectedInventory, Inventory actualInventory) {
        assertThat(actualInventory.getId()).isEqualTo(expectedInventory.getId());
        assertThat(actualInventory.getName()).isEqualTo(expectedInventory.getName());
        assertThat(actualInventory.getShopName()).isEqualTo(expectedInventory.getShopName());
        assertThat(actualInventory.getCategory().getId())
                .isEqualTo(expectedInventory.getCategory().getId());
        assertThat(actualInventory.getQuantity()).isEqualTo(expectedInventory.getQuantity());
        assertThat(actualInventory.getDescription()).isEqualTo(expectedInventory.getDescription());
        assertThat(actualInventory.getSubCategories())
                .extracting(SubCategory::getName)
                .containsExactlyInAnyOrderElementsOf(
                        expectedInventory.getSubCategories()
                                .stream()
                                .map(SubCategory::getName)
                                .toList()
                );
    }
    
}
