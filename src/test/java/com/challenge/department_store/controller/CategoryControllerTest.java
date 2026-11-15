package com.challenge.department_store.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.exception.DuplicateEntityException;
import com.challenge.department_store.exception.GlobalExceptionHandler;
import com.challenge.department_store.exception.IllegalOperationException;
import com.challenge.department_store.exception.NotFoundException;
import com.challenge.department_store.service.CategoryService;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(
    value = CategoryController.class,
    properties = "spring.jackson.deserialization.fail-on-unknown-properties=true"
)
@Import(GlobalExceptionHandler.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void testGetAllCategoriesReturns200WithList() {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        assertThat(mvc.get().uri("/api/categories"))
            .hasStatusOk()
            .bodyJson().isStrictlyEqualTo("[]");
    }

    @Test
    void testCreateCategoryReturns201() {
        CategoryResponse response = new CategoryResponse(1L, "Electronics", Set.of());
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
            .thenReturn(response);

        assertThat(mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Electronics", "subCategories": []}
                    """))
            .hasStatus(HttpStatus.CREATED)
            .bodyJson().extractingPath("$.name").isEqualTo("Electronics");
    }

    @Test
    void testCreateCategoryReturns409WhenNameAlreadyExistsIgnoringCase() {
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
            .thenThrow(new DuplicateEntityException("Category already exists: Electronics"));

        assertThat(mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "ELECTRONICS", "subCategories": []}
                    """))
            .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void testGetCategoryByNameReturns404WhenNotFound() {
        when(categoryService.getCategoryByName("unknown-category"))
            .thenThrow(new NotFoundException("Category not found: unknown-category"));

        assertThat(mvc.get().uri("/api/categories/unknown-category"))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateCategoryReturns200() {
        CategoryResponse response = new CategoryResponse(1L, "Electronics", Set.of());
        when(categoryService.updateCategory(any(UpdateCategoryRequest.class), eq("Electronics")))
            .thenReturn(response);

        assertThat(mvc.put().uri("/api/categories/Electronics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subCategories": []}
                    """))
            .hasStatusOk()
            .bodyJson().extractingPath("$.name").isEqualTo("Electronics");
    }

    @Test
    void testCreateCategoryReturns400WhenAnyCategoryNameExceeds100Characters() {
        String tooLongName = "a".repeat(101);

        assertThat(mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "%s", "subCategories": []}
                    """.formatted(tooLongName)))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testCreateCategoryReturns400WhenAnySubCategoryNameExceeds100Characters() {
        String tooLongSubName = "b".repeat(101);

        assertThat(mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Electronics", "subCategories": ["%s"]}
                    """.formatted(tooLongSubName)))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testGetCategoryByNameReturns400WhenNameExceeds100Characters() {
        String tooLongName = "a".repeat(101);

        assertThat(mvc.get().uri("/api/categories/" + tooLongName))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategoryReturns400WhenCategoryNameExceeds100Characters() {
        String tooLongName = "a".repeat(101);

        assertThat(mvc.put().uri("/api/categories/" + tooLongName)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subCategories": []}
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategoryReturns400WhenSubCategoryNameExceeds100Characters() {
        String tooLongSubName = "b".repeat(101);

        assertThat(mvc.put().uri("/api/categories/Electronics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"subCategories": ["%s"]}
                    """.formatted(tooLongSubName)))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testCreateCategoryReturns201WithMultipleSubCategories() {
        CategoryResponse response = new CategoryResponse(
            1L, "Electronics", Set.of("Phones", "Laptops", "Tablets"));
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
            .thenReturn(response);

        MockMvcRequestBuilder result = mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    "name": "Electronics",
                    "subCategories": ["Phones", "Laptops", "Tablets"]
                    }
                    """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.subCategories.length()").isEqualTo(3);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
            {"subCategories": ["Phones", "Laptops", "Tablets"]}
            """);
    }

    @Test
    void testUpdateCategoryReturns200WithMultipleSubCategories() {
        CategoryResponse response = new CategoryResponse(
            1L, "Electronics", Set.of("Phones", "Laptops"));
        when(categoryService.updateCategory(any(UpdateCategoryRequest.class), eq("Electronics")))
            .thenReturn(response);

        MockMvcRequestBuilder result = mvc.put().uri("/api/categories/Electronics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "subCategories": ["Phones", "Laptops"]
                    }
                    """);
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.subCategories.length()").isEqualTo(2);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
            {"subCategories": ["Phones", "Laptops"]}
            """);
    }

    @Test
    void testCreateCategoryReturns400WhenNameIsBlank() {
        assertThat(mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "  ", "subCategories": []}
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategoryReturns200WhenUpdatedCategory() {
        CategoryResponse response = new CategoryResponse(
            1L, "Footwear", Set.of("Shoe", "Trainers", "Boots"));
        when(categoryService.updateCategory(any(UpdateCategoryRequest.class), eq("footwear")))
            .thenReturn(response);

        MockMvcRequestBuilder result = mvc.put()
                .uri("/api/categories/footwear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "subCategories": ["Shoe", "Trainers", "Boots"]
                        }
                        """);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().isLenientlyEqualTo("""
            {"name": "Footwear", "subCategories": ["Shoe", "Trainers", "Boots"]}
            """);
    }

    @Test
    void testUpdateCategoryReturns400WhenNewSubCategorySetIsInUse() {
        when(categoryService.updateCategory(any(UpdateCategoryRequest.class), eq("footwear")))
            .thenThrow(new IllegalOperationException("Cannot replace sub categories in use."));

        MockMvcRequestBuilder result = mvc.put()
                .uri("/api/categories/footwear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "subCategories": ["Trainers"]
                        }
                        """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateCategoryReturns404WhenNotFoundForUnknownCategory() {
        when(categoryService.updateCategory(any(UpdateCategoryRequest.class), eq("unknown-category")))
            .thenThrow(new NotFoundException("Category not found: unknown-category"));

        MockMvcRequestBuilder result = mvc.put()
                .uri("/api/categories/unknown-category")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "subCategories": ["Shoe"]
                        }
                        """);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateCategoryReturns400WhenRejectsMissingSubCategories() {
        MockMvcRequestBuilder result = mvc.put()
                .uri("/api/categories/footwear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategoryReturns400WhenRejectsSubCategoryNameOver100Characters() {
        String longSubCategoryName = "a".repeat(101);

        MockMvcRequestBuilder result = mvc.put()
                .uri("/api/categories/footwear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "subCategories": ["%s"]
                        }
                        """.formatted(longSubCategoryName));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategoryReturns400WhenRejectsUnknownJsonField() {
        MockMvcRequestBuilder result = mvc.put()
                .uri("/api/categories/footwear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "subCategories": ["Shoe"],
                        "unknownField": "value"
                        }
                        """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategoryReturns400WhenRemovingSubCategoryUsedByInventory() {
        when(categoryService.updateCategory(
                any(UpdateCategoryRequest.class),
                eq("Footwear")))
            .thenThrow(new IllegalOperationException(
                "Cannot replace sub-categories in use."
            ));

        assertThat(mvc.put().uri("/api/categories/Footwear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    "subCategories": ["Boots"]
                    }
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
