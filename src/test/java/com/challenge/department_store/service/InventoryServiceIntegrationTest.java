package com.challenge.department_store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

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

@DataJpaTest
@Import(InventoryService.class)
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testCreateInventoryPersistsInventoryAndSubCategoryAssociations() {
        CategoryFixture fixture = createCategoryFixture();
        CreateInventoryRequest request = createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of(fixture.shoeSubCategory().getName()),
                8
        );

        InventoryResponse response = inventoryService.createInventory(request);
        entityManager.flush();
        entityManager.clear();

        Inventory persistedInventory = inventoryRepository.findById(response.id()).orElseThrow();

        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.shopName()).isEqualTo(request.shopName());
        assertThat(response.subCategories()).containsExactly(fixture.shoeSubCategory().getName());
        assertThat(persistedInventory.getQuantity()).isEqualTo(8);
        assertThat(persistedInventory.getSubCategories())
                .extracting(SubCategory::getName)
                .containsExactly(fixture.shoeSubCategory().getName());
    }

    @Test
    void testCreateInventoryRejectsCaseInsensitiveDuplicateNameAndShopPair() {
        CategoryFixture fixture = createCategoryFixture();
        inventoryService.createInventory(createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of(fixture.shoeSubCategory().getName()),
                8
        ));
        CreateInventoryRequest duplicateRequest = createRequest(
                fixture,
                "AIR JORDAN",
                "JORDAN STORE",
                Set.of(fixture.shoeSubCategory().getName()),
                10
        );

        assertThatThrownBy(() -> inventoryService.createInventory(duplicateRequest))
                .isInstanceOf(DuplicateEntityException.class);
        assertThat(inventoryRepository.count()).isEqualTo(1);
    }

    @Test
    void testCreateInventoryRejectsMissingCategoryOrSubCategory() {
        CategoryFixture fixture = createCategoryFixture();
        CreateInventoryRequest missingCategoryRequest = new CreateInventoryRequest(
                "Air Jordan",
                "Jordan Store",
                "Unknown category",
                Set.of(fixture.shoeSubCategory().getName()),
                8,
                "Test inventory"
        );
        CreateInventoryRequest missingSubCategoryRequest = createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of("Unknown subcategory " + fixture.suffix()),
                8
        );

        assertThatThrownBy(() -> inventoryService.createInventory(missingCategoryRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category does not exist");
        assertThatThrownBy(() -> inventoryService.createInventory(missingSubCategoryRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("sub-categories does not exist");
        assertThat(inventoryRepository.count()).isZero();
    }

    @Test
    void testCreateInventoryRejectsExistingSubCategoryNotAssignedToCategory() {
        CategoryFixture fixture = createCategoryFixture();
        SubCategory unrelatedSubCategory =
                entityManager.persistAndFlush(new SubCategory("Unrelated " + fixture.suffix()));
        CreateInventoryRequest request = createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of(unrelatedSubCategory.getName()),
                8
        );

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("not compatible");
        assertThat(inventoryRepository.count()).isZero();
    }

    @Test
    void testUpdateInventoryPersistsChangedDetailsAndUsesLockedLookup() {
        CategoryFixture fixture = createCategoryFixture();
        InventoryResponse created = inventoryService.createInventory(createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of(fixture.shoeSubCategory().getName()),
                8
        ));
        UpdateInventoryRequest request = new UpdateInventoryRequest(
                "Air Jordan Updated",
                "Jordan Store",
                fixture.category().getName(),
                Set.of(fixture.bootSubCategory().getName()),
                12,
                "Updated description"
        );

        InventoryResponse response = inventoryService.updateInventory(created.id(), request);
        entityManager.flush();
        entityManager.clear();
        Inventory persistedInventory = inventoryRepository.findById(created.id()).orElseThrow();

        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.quantity()).isEqualTo(12);
        assertThat(response.subCategories()).containsExactly(fixture.bootSubCategory().getName());
        assertThat(persistedInventory.getName()).isEqualTo(request.name());
        assertThat(persistedInventory.getDescription()).isEqualTo(request.description());
        assertThat(persistedInventory.getSubCategories())
                .extracting(SubCategory::getName)
                .containsExactly(fixture.bootSubCategory().getName());
    }

    @Test
    void testUpdateInventoryRejectsDuplicateNameAndShopPair() {
        CategoryFixture fixture = createCategoryFixture();
        String conflictingName = "Pegasus " + fixture.suffix();
        String conflictingShopName = "Nike Store " + fixture.suffix();
        InventoryResponse original = inventoryService.createInventory(createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of(fixture.shoeSubCategory().getName()),
                8
        ));
        inventoryService.createInventory(new CreateInventoryRequest(
                conflictingName,
                conflictingShopName,
                fixture.category().getName(),
                Set.of(fixture.shoeSubCategory().getName()),
                5,
                "Test inventory"
        ));
        UpdateInventoryRequest duplicateRequest = new UpdateInventoryRequest(
                conflictingName.toUpperCase(),
                conflictingShopName.toUpperCase(),
                fixture.category().getName(),
                Set.of(fixture.shoeSubCategory().getName()),
                8,
                "Should not be saved"
        );

        assertThatThrownBy(() -> inventoryService.updateInventory(original.id(), duplicateRequest))
                .isInstanceOf(DuplicateEntityException.class);

        Inventory unchanged = inventoryRepository.findById(original.id()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Air Jordan " + fixture.suffix());
        assertThat(unchanged.getShopName()).isEqualTo("Jordan Store " + fixture.suffix());
    }

    @Test
    void testUpdateInventoryQuantityPersistsValueUsingLockedLookup() {
        CategoryFixture fixture = createCategoryFixture();
        InventoryResponse created = inventoryService.createInventory(createRequest(
                fixture,
                "Air Jordan",
                "Jordan Store",
                Set.of(fixture.shoeSubCategory().getName()),
                8
        ));

        InventoryResponse response = inventoryService.updateInventoryQuantity(
                created.id(),
                new UpdateInventoryQuantityRequest(3)
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(response.quantity()).isEqualTo(3);
        assertThat(inventoryRepository.findById(created.id()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    private CategoryFixture createCategoryFixture() {
        String suffix = UUID.randomUUID().toString();
        SubCategory shoe = entityManager.persistAndFlush(new SubCategory("Shoe " + suffix));
        SubCategory boot = entityManager.persistAndFlush(new SubCategory("Boot " + suffix));
        Category category = new Category("Footwear " + suffix);
        category.addSubCategory(shoe);
        category.addSubCategory(boot);
        entityManager.persistAndFlush(category);
        entityManager.clear();

        Category persistedCategory = categoryRepository.findByNameIgnoreCase(category.getName()).orElseThrow();
        SubCategory persistedShoe = subCategoryRepository.findByNameIgnoreCase(shoe.getName()).orElseThrow();
        SubCategory persistedBoot = subCategoryRepository.findByNameIgnoreCase(boot.getName()).orElseThrow();

        return new CategoryFixture(suffix, persistedCategory, persistedShoe, persistedBoot);
    }

    private CreateInventoryRequest createRequest(
            CategoryFixture fixture,
            String name,
            String shopName,
            Set<String> subCategories,
            int quantity) {
        return new CreateInventoryRequest(
                name + " " + fixture.suffix(),
                shopName + " " + fixture.suffix(),
                fixture.category().getName(),
                subCategories,
                quantity,
                "Test inventory"
        );
    }

    private record CategoryFixture(
            String suffix,
            Category category,
            SubCategory shoeSubCategory,
            SubCategory bootSubCategory) {
    }
}
