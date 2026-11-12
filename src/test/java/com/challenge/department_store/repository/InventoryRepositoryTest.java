package com.challenge.department_store.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;

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
    void testFindInventoryByNameAndShop() {
        inventoryRepository.saveAndFlush(inventory);
        Inventory retieveInventory = inventoryRepository.findInventoryByNameAndShopName(inventory.getName(), inventory.getShopName()).orElseThrow();
        validateInventories(inventory, retieveInventory);
    }

    @Test 
    void testReturnEmptyForNonExistentInventoryFoundByNameAndShop() {
        assertThat(inventoryRepository.findInventoryByNameAndShopName("inventory name", "shop name")).isEmpty();
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

    private void validateInventories(Inventory inventory1, Inventory inventory2) {
        assertThat(inventory1.getCategory().getId()).isEqualTo(inventory2.getCategory().getId());
        assertThat(inventory1.getName()).isEqualTo(inventory2.getName());
        assertThat(inventory1.getQuantity()).isEqualTo(inventory2.getQuantity());
        assertThat(inventory1.getDescription()).isEqualTo(inventory2.getDescription());
        assertThat(inventory1.getShopName()).isEqualTo(inventory2.getShopName());
        assertThat(inventory1.getSubCategories()).isEqualTo(inventory2.getSubCategories());
    }
    
}
