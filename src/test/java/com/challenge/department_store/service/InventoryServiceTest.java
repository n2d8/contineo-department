package com.challenge.department_store.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @InjectMocks 
    private InventoryService inventoryService;

    private static final Long INVENTORY_ID = 1L;
    private static final String INVENTORY_NAME = "Air Jordan 1";
    private static final String SHOP_NAME = "Jordan";
    private static final String CATEGORY_NAME = "Footwear";
    private static final String SHOE_SUBCATEGORY_NAME = "Shoe";
    private static final String TRAINERS_SUBCATEGORY_NAME = "Trainers";

    private Category footwearCategory;
    private SubCategory shoeSubCategory;
    private Inventory existingInventory;

    private Category createCategory(String name, String... subCategoryNames) {
        Category category = new Category(name);

        for (String subCategoryName : subCategoryNames) {
            category.addSubCategory(new SubCategory(subCategoryName));
        }

        return category;
    }

    private Inventory createInventory(String name,
                                    String shopName,
                                    Category category,
                                    int quantity,
                                    String description,
                                    String... subCategoryNames) {
        Set<SubCategory> subCategories = new java.util.HashSet<>();

        for (String subCategoryName : subCategoryNames) {
            subCategories.add(new SubCategory(subCategoryName));
        }

        return new Inventory(
                name,
                shopName,
                category,
                quantity,
                description,
                subCategories
        );
    }

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                inventoryRepository,
                categoryRepository,
                subCategoryRepository
        );

        footwearCategory = createCategory(
                CATEGORY_NAME,
                SHOE_SUBCATEGORY_NAME,
                TRAINERS_SUBCATEGORY_NAME
        );

        shoeSubCategory = new SubCategory(SHOE_SUBCATEGORY_NAME);

        existingInventory = createInventory(
                INVENTORY_NAME,
                SHOP_NAME,
                footwearCategory,
                10,
                "High-top basketball shoe",
                SHOE_SUBCATEGORY_NAME
        );
    }

    private void stubExistingCategory() {
        when(categoryRepository.findByNameIgnoreCase(CATEGORY_NAME))
                .thenReturn(Optional.of(footwearCategory));
    }

    private void stubExistingSubCategories(String... subCategoryNames) {
        for (String subCategoryName : subCategoryNames) {
            when(subCategoryRepository.findByNameIgnoreCase(subCategoryName))
                    .thenReturn(Optional.of(new SubCategory(subCategoryName)));
        }
    }

    private void stubInventorySave() {
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCreateInventoryCreatesInventory() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        stubExistingSubCategories(SHOE_SUBCATEGORY_NAME);
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.name()).isEqualTo(INVENTORY_NAME);
        assertThat(response.shopName()).isEqualTo(SHOP_NAME);
        assertThat(response.categoryName()).isEqualTo(CATEGORY_NAME);
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.description()).isEqualTo("High-top basketball shoe");
        assertThat(response.subCategories()).containsExactly(SHOE_SUBCATEGORY_NAME);

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryCreatesInventoryWithoutSubCategories() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.subCategories()).isEmpty();

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryCreatesInventoryWithMultipleSubCategories() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(
                        SHOE_SUBCATEGORY_NAME,
                        TRAINERS_SUBCATEGORY_NAME
                ),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        stubExistingSubCategories(
                SHOE_SUBCATEGORY_NAME,
                TRAINERS_SUBCATEGORY_NAME
        );
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.subCategories())
                .containsExactlyInAnyOrder(
                        SHOE_SUBCATEGORY_NAME,
                        TRAINERS_SUBCATEGORY_NAME
                );

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryReusesExistingCategoryIgnoringCase() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                "FOOTWEAR",
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "High-top basketball shoe"
        );

        when(categoryRepository.findByNameIgnoreCase("FOOTWEAR"))
                .thenReturn(Optional.of(footwearCategory));
        stubExistingSubCategories(SHOE_SUBCATEGORY_NAME);
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.name()).isEqualTo(INVENTORY_NAME);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateInventoryThrowsWhenCategoryDoesNotExist() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                "Unknown Category",
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "High-top basketball shoe"
        );

        when(categoryRepository.findByNameIgnoreCase("Unknown Category"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inventoryService.createInventory(request)
        )
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Category does not exist.");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryReusesExistingSubCategoryIgnoringCase() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of("SHOE"),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        when(subCategoryRepository.findByNameIgnoreCase("SHOE"))
                .thenReturn(Optional.of(shoeSubCategory));
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.subCategories())
                .containsExactly(SHOE_SUBCATEGORY_NAME);

        verify(subCategoryRepository, never()).save(any(SubCategory.class));
    }

    @Test
    void testCreateInventoryRejectsSubCategoryNotAssignedToCategory() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(
                        SHOE_SUBCATEGORY_NAME,
                        "Boots"
                ),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        stubExistingSubCategories("Boots");

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sub categories is not compatible with category.");

        verify(subCategoryRepository, never()).save(any(SubCategory.class));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryThrowsEntityNotFoundWhenSubCategoryDoesNotExist() {
        String missingSubCategory = "Unknown";
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(missingSubCategory),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        when(subCategoryRepository.findByNameIgnoreCase(missingSubCategory))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Some of the sub categories does not exist.");

        verify(subCategoryRepository).findByNameIgnoreCase(missingSubCategory);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryThrowsWhenInventoryNameAndShopNameAlreadyExist() {
        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        stubExistingSubCategories(SHOE_SUBCATEGORY_NAME);
        when(inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(
                INVENTORY_NAME,
                SHOP_NAME
        ))
                .thenReturn(Optional.of(existingInventory));

        assertThatThrownBy(() ->
                inventoryService.createInventory(request)
        )
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("Inventory name and shop name pair already exists.");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryAllowsSameInventoryNameWithDifferentShopName() {
        String differentShopName = "Nike Store";

        CreateInventoryRequest request = new CreateInventoryRequest(
                INVENTORY_NAME,
                differentShopName,
                CATEGORY_NAME,
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "High-top basketball shoe"
        );

        stubExistingCategory();
        stubExistingSubCategories(SHOE_SUBCATEGORY_NAME);
        when(inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(
                INVENTORY_NAME,
                differentShopName
        ))
                .thenReturn(Optional.empty());
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.shopName()).isEqualTo(differentShopName);

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testCreateInventoryAllowsSameShopNameWithDifferentInventoryName() {
        String differentInventoryName = "Nike Pegasus";

        CreateInventoryRequest request = new CreateInventoryRequest(
                differentInventoryName,
                SHOP_NAME,
                CATEGORY_NAME,
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "Running shoe"
        );

        stubExistingCategory();
        stubExistingSubCategories(SHOE_SUBCATEGORY_NAME);
        when(inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(
                differentInventoryName,
                SHOP_NAME
        ))
                .thenReturn(Optional.empty());
        stubInventorySave();

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.name()).isEqualTo(differentInventoryName);

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testUpdateInventoryThrowsDuplicateKeyWhenNameAndShopPairBelongsToAnotherInventory() {
        String conflictingName = "Nike Pegasus";
        String conflictingShopName = "Nike";
        UpdateInventoryRequest request = new UpdateInventoryRequest(
                conflictingName,
                conflictingShopName,
                CATEGORY_NAME,
                Set.of(SHOE_SUBCATEGORY_NAME),
                10,
                "Running shoe"
        );
        Inventory otherInventory = createInventory(
                conflictingName,
                conflictingShopName,
                footwearCategory,
                5,
                "Existing running shoe",
                SHOE_SUBCATEGORY_NAME
        );

        when(inventoryRepository.findByIdForUpdate(INVENTORY_ID))
                .thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.findByNameIgnoreCaseAndShopNameIgnoreCase(
                conflictingName,
                conflictingShopName
        ))
                .thenReturn(Optional.of(otherInventory));

        assertThatThrownBy(() -> inventoryService.updateInventory(INVENTORY_ID, request))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("Inventory name and shop name pair already exists.");

        verify(inventoryRepository).findByNameIgnoreCaseAndShopNameIgnoreCase(
                conflictingName,
                conflictingShopName
        );
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testUpdateInventoryQuantityUpdatesExistingInventory() {
        UpdateInventoryQuantityRequest request = new UpdateInventoryQuantityRequest(25);

        when(inventoryRepository.findByIdForUpdate(INVENTORY_ID))
                .thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(existingInventory))
                .thenReturn(existingInventory);

        InventoryResponse response =
                inventoryService.updateInventoryQuantity(
                        INVENTORY_ID,
                        request
                );

        assertThat(response.quantity()).isEqualTo(25);

        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void testUpdateInventoryQuantityAllowsZeroQuantity() {
        UpdateInventoryQuantityRequest request = new UpdateInventoryQuantityRequest(0);

        when(inventoryRepository.findByIdForUpdate(INVENTORY_ID))
                .thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(existingInventory))
                .thenReturn(existingInventory);

        InventoryResponse response =
                inventoryService.updateInventoryQuantity(
                        INVENTORY_ID,
                        request
                );

        assertThat(response.quantity()).isZero();

        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void testUpdateInventoryQuantityThrowsForUnknownInventory() {
        UpdateInventoryQuantityRequest request = new UpdateInventoryQuantityRequest(25);

        when(inventoryRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inventoryService.updateInventoryQuantity(999L, request)
        )
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Inventory not found");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testUpdateInventoryQuantityRejectsNegativeQuantity() {
        UpdateInventoryQuantityRequest request = new UpdateInventoryQuantityRequest(-1);

        assertThatThrownBy(() ->
                inventoryService.updateInventoryQuantity(
                        INVENTORY_ID,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must not be negative.");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testGetAllInventoriesReturnsAllInventories() {
        Category electronicsCategory = createCategory(
                "Electronics",
                "Phone"
        );

        Inventory electronicsInventory = createInventory(
                "iPhone 16",
                "Apple",
                electronicsCategory,
                5,
                "Smartphone",
                "Phone"
        );

        when(inventoryRepository.findAll())
                .thenReturn(List.of(existingInventory, electronicsInventory));

        List<InventoryResponse> responses = inventoryService.getAllInventories();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(InventoryResponse::name)
                .containsExactlyInAnyOrder(
                        INVENTORY_NAME,
                        "iPhone 16"
                );
    }

    @Test
    void testGetInventoryByIdReturnsInventory() {
        when(inventoryRepository.findById(INVENTORY_ID))
                .thenReturn(Optional.of(existingInventory));

        InventoryResponse response = inventoryService.getInventoryById(INVENTORY_ID);

        assertThat(response.name()).isEqualTo(INVENTORY_NAME);
        assertThat(response.shopName()).isEqualTo(SHOP_NAME);
        assertThat(response.categoryName()).isEqualTo(CATEGORY_NAME);
    }

    @Test
    void testGetInventoryByIdThrowsForUnknownInventory() {
        when(inventoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inventoryService.getInventoryById(999L)
        )
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Inventory not found");
    }
}
