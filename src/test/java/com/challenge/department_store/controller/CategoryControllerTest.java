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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.service.CategoryService;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(CategoryController.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvcTester mvc;   // ← instead of MockMvc

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void testGetAllCategories_returns200WithList() {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        assertThat(mvc.get().uri("/api/categories"))
            .hasStatusOk()
            .bodyJson().isStrictlyEqualTo("[]");;
    }

    @Test
    void testCreateCategory_returns201() {
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
    void testUpdateCategory_returns200() {
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
    void testCreateCategory_returns400_whenAnyCategoryNameExceeds100Characters() {
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
    void testCreateCategory_returns400_whenAnySubCategoryNameExceeds100Characters() {
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
    void testGetCategoryByName_returns400_whenNameExceeds100Characters() {
        String tooLongName = "a".repeat(101);

        assertThat(mvc.get().uri("/api/categories/" + tooLongName))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateCategory_returns400_whenCategoryNameExceeds100Characters() {
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
    void testUpdateCategory_returns400_whenSubCategoryNameExceeds100Characters() {
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
    void testCreateCategory_returns201_withMultipleSubCategories() {
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
    void updateCategory_returns200_withMultipleSubCategories() {
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
    void testCreateCategory_returns400_whenNameIsBlank() {
        assertThat(mvc.post().uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "  ", "subCategories": []}
                    """))
            .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(categoryService);
    }
}
