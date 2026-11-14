package com.challenge.department_store.controller;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.service.CategoryService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController 
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping 
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{name}")
    public CategoryResponse getCategoryByName(@PathVariable @Size(max = 100) String name) {
        try {
            return categoryService.getCategoryByName(name);
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                exception
            );
        }
    }

    @PostMapping 
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        try {
            return categoryService.createCategory(request);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception
            );
        }
    }

    @PutMapping("/{name}")
    public CategoryResponse updateCategory(@Valid @RequestBody UpdateCategoryRequest request, 
                                        @PathVariable @Size(max = 100) String name) {
        //TODO: Create a global exception handler and specific service exceptions.
        try {
            return categoryService.updateCategory(request, name);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception
            );
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                exception
            );
        }
    }
}
