package com.challenge.department_store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.challenge.department_store.dto.CreateInventoryRequest;
import com.challenge.department_store.dto.InventoryResponse;
import com.challenge.department_store.dto.UpdateInventoryQuantityRequest;
import com.challenge.department_store.dto.UpdateInventoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.service.InventoryService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(
    value = InventoryController.class,
    properties = "spring.jackson.deserialization.fail-on-unknown-properties=true"
)
class InventoryControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void testGetAllInventoriesReturns200WithInventoryList() {
        when(inventoryService.getAllInventories()).thenReturn(List.of(
            inventory(1L, "Air Jordan 1", "Jordan", "Footwear", Set.of("Shoe"), 10),
            inventory(2L, "Pegasus", "Nike", "Footwear", Set.of("Shoe"), 5)
        ));

        assertThat(mvc.get().uri("/api/inventories"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.length()").isEqualTo(2);
    }

    @Test
    void testGetAllInventoriesReturnsEmptyList() {
        when(inventoryService.getAllInventories()).thenReturn(List.of());

        assertThat(mvc.get().uri("/api/inventories"))
            .hasStatusOk()
            .bodyJson().isStrictlyEqualTo("[]");
    }

    @Test
    void testGetInventoryByIdReturns200WithInventory() {
        when(inventoryService.getInventoryById(1L))
            .thenReturn(inventory(1L, "Air Jordan 1", "Jordan", "Footwear", Set.of("Shoe"), 10));

        assertThat(mvc.get().uri("/api/inventories/1"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.name").isEqualTo("Air Jordan 1");

        verify(inventoryService).getInventoryById(1L);
    }

    @Test
    void testGetInventoryByIdReturns404WhenNotFound() {
        when(inventoryService.getInventoryById(999L))
            .thenThrow(new EntityNotFoundException("Inventory not found: 999"));

        assertThat(mvc.get().uri("/api/inventories/999"))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetInventoryByIdReturns400WhenIdIsNotNumeric() {
        assertThat(mvc.get().uri("/api/inventories/not-a-number"))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testCreateInventoryReturns201AndDelegatesRequest() {
        CreateInventoryRequest request = new CreateInventoryRequest(
            "Air Jordan 1",
            "Jordan",
            "Footwear",
            Set.of("Shoe"),
            10,
            "High-top basketball shoe"
        );
        when(inventoryService.createInventory(request))
            .thenReturn(inventory(1L, "Air Jordan 1", "Jordan", "Footwear", Set.of("Shoe"), 10));

        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Air Jordan 1",
                      "shopName": "Jordan",
                      "category": "Footwear",
                      "subCategories": ["Shoe"],
                      "quantity": 10,
                      "description": "High-top basketball shoe"
                    }
                    """))
            .hasStatus(HttpStatus.CREATED)
            .bodyJson().extractingPath("$.name").isEqualTo("Air Jordan 1");

        verify(inventoryService).createInventory(eq(request));
    }

    @Test
    void testCreateInventoryReturns409WhenInventoryAlreadyExists() {
        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
            .thenThrow(new DuplicateKeyException("Inventory name and shop name pair already exists."));

        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInventoryRequest()))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void testCreateInventoryReturns404WhenCategoryDoesNotExist() {
        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
            .thenThrow(new EntityNotFoundException("Category does not exist."));

        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInventoryRequest()))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCreateInventoryReturns404WhenSubCategoryDoesNotExist() {
        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
            .thenThrow(new EntityNotFoundException("Sub category does not exist."));

        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInventoryRequest()))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCreateInventoryReturns409WhenSubCategoryIsNotCompatibleWithCategory() {
        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
            .thenThrow(new IllegalStateException(
                "Sub categories is not compatible with category."
            ));

        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInventoryRequest()))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void testCreateInventoryReturns400WhenRequiredFieldsAreMissing() {
        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testCreateInventoryReturns400WhenSubCategoryNameExceeds100Characters() {
        String longName = "a".repeat(101);

        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Air Jordan 1",
                      "shopName": "Jordan",
                      "category": "Footwear",
                      "subCategories": ["%s"],
                      "quantity": 10,
                      "description": "Shoe"
                    }
                    """.formatted(longName)))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testCreateInventoryReturns400WhenUnknownJsonFieldIsPresent() {
        assertThat(mvc.post().uri("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Air Jordan 1",
                      "shopName": "Jordan",
                      "category": "Footwear",
                      "subCategories": ["Shoe"],
                      "quantity": 10,
                      "description": "Shoe",
                      "unexpected": true
                    }
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testUpdateInventoryReturns200AndDelegatesIdAndRequest() {
        UpdateInventoryRequest request = new UpdateInventoryRequest(
            "Air Jordan 1",
            "Jordan",
            "Footwear",
            Set.of("Shoe"),
            12,
            "Updated description"
        );
        when(inventoryService.updateInventory(1L, request))
            .thenReturn(inventory(1L, "Air Jordan 1", "Jordan", "Footwear", Set.of("Shoe"), 12));

        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Air Jordan 1",
                      "shopName": "Jordan",
                      "category": "Footwear",
                      "subCategories": ["Shoe"],
                      "quantity": 12,
                      "description": "Updated description"
                    }
                    """))
            .hasStatusOk()
            .bodyJson().extractingPath("$.quantity").isEqualTo(12);

        verify(inventoryService).updateInventory(1L, request);
    }

    @Test
    void testUpdateInventoryReturns404WhenInventoryDoesNotExist() {
        when(inventoryService.updateInventory(
                eq(999L),
                any(UpdateInventoryRequest.class)))
            .thenThrow(new EntityNotFoundException("Inventory not found: 999"));

        assertThat(mvc.put().uri("/api/inventories/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequest()))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateInventoryReturns404WhenCategoryDoesNotExist() {
        when(inventoryService.updateInventory(
                eq(1L),
                any(UpdateInventoryRequest.class)))
            .thenThrow(new EntityNotFoundException("Category does not exist."));

        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequest()))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateInventoryReturns404WhenSubCategoryDoesNotExist() {
        when(inventoryService.updateInventory(
                eq(1L),
                any(UpdateInventoryRequest.class)))
            .thenThrow(new EntityNotFoundException("Sub category does not exist."));

        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequest()))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateInventoryReturns409WhenSubCategoryIsNotCompatibleWithCategory() {
        when(inventoryService.updateInventory(
                eq(1L),
                any(UpdateInventoryRequest.class)))
            .thenThrow(new IllegalStateException(
                "Sub categories is not compatible with category."
            ));

        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequest()))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void testUpdateInventoryReturns409WhenInventoryNameAndShopNameAlreadyExist() {
        when(inventoryService.updateInventory(
                eq(1L),
                any(UpdateInventoryRequest.class)))
            .thenThrow(new DuplicateKeyException(
                "Inventory name and shop name pair already exists."
            ));

        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequest()))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void testUpdateInventoryReturns400WhenRequiredFieldsAreMissing() {
        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testUpdateInventoryReturns400WhenQuantityIsNegative() {
        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Air Jordan 1",
                      "shopName": "Jordan",
                      "category": "Footwear",
                      "subCategories": ["Shoe"],
                      "quantity": -1
                    }
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testUpdateInventoryReturns400WhenUnknownJsonFieldIsPresent() {
        assertThat(mvc.put().uri("/api/inventories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Air Jordan 1",
                      "shopName": "Jordan",
                      "category": "Footwear",
                      "subCategories": ["Shoe"],
                      "quantity": 10,
                      "unexpected": true
                    }
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testUpdateInventoryQuantityReturns200AndDelegatesIdAndRequest() {
        UpdateInventoryQuantityRequest request = new UpdateInventoryQuantityRequest(0);
        when(inventoryService.updateInventoryQuantity(1L, request))
            .thenReturn(inventory(1L, "Air Jordan 1", "Jordan", "Footwear", Set.of("Shoe"), 0));

        assertThat(mvc.patch().uri("/api/inventories/1/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity": 0}
                    """))
            .hasStatusOk()
            .bodyJson().extractingPath("$.quantity").isEqualTo(0);

        verify(inventoryService).updateInventoryQuantity(1L, request);
    }

    @Test
    void testUpdateInventoryQuantityReturns404WhenInventoryDoesNotExist() {
        when(inventoryService.updateInventoryQuantity(
                eq(999L),
                any(UpdateInventoryQuantityRequest.class)))
            .thenThrow(new EntityNotFoundException("Inventory not found: 999"));

        assertThat(mvc.patch().uri("/api/inventories/999/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity": 10}
                    """))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateInventoryQuantityReturns400WhenQuantityIsNegative() {
        assertThat(mvc.patch().uri("/api/inventories/1/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity": -1}
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    @Test
    void testUpdateInventoryQuantityReturns400WhenQuantityIsMissing() {
        assertThat(mvc.patch().uri("/api/inventories/1/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(inventoryService);
    }

    private static InventoryResponse inventory(
            Long id,
            String name,
            String shopName,
            String categoryName,
            Set<String> subCategories,
            int quantity) {
        return new InventoryResponse(
            id,
            name,
            shopName,
            new Category(categoryName),
            subCategories,
            quantity,
            "Inventory description"
        );
    }

    private static String validInventoryRequest() {
        return """
            {
              "name": "Air Jordan 1",
              "shopName": "Jordan",
              "category": "Footwear",
              "subCategories": ["Shoe"],
              "quantity": 10,
              "description": "Basketball shoe"
            }
            """;
    }

    private static String validUpdateRequest() {
        return """
            {
              "name": "Air Jordan 1",
              "shopName": "Jordan",
              "category": "Footwear",
              "subCategories": ["Shoe"],
              "quantity": 10,
              "description": "Basketball shoe"
            }
            """;
    }
}
