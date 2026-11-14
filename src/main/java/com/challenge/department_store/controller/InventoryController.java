package com.challenge.department_store.controller;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.challenge.department_store.dto.CreateInventoryRequest;
import com.challenge.department_store.dto.InventoryResponse;
import com.challenge.department_store.dto.UpdateInventoryQuantityRequest;
import com.challenge.department_store.dto.UpdateInventoryRequest;
import com.challenge.department_store.service.InventoryService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> getAllInventories() {
        return inventoryService.getAllInventories();
    }

    @GetMapping("/{id}")
    public InventoryResponse getInventoryById(@PathVariable Long id) {
        try {
            return inventoryService.getInventoryById(id);
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
    public InventoryResponse createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        try {
            return inventoryService.createInventory(request);
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                exception
            );
        } catch (DuplicateKeyException | IllegalStateException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception
            );
        }
    }

    @PutMapping("/{id}")
    public InventoryResponse updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        try {
            return inventoryService.updateInventory(id, request);
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                exception
            );
        } catch (DuplicateKeyException | IllegalStateException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception
            );
        }
    }

    @PatchMapping("/{id}/quantity")
    public InventoryResponse updateInventoryQuantity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryQuantityRequest request) {
        try {
            return inventoryService.updateInventoryQuantity(id, request);
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                exception
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                exception
            );
        }
    }
}
