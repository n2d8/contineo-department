package com.challenge.department_store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByNameIgnoreCaseAndShopNameIgnoreCase(String inventoryName, String shopName);

    List<Inventory> findByCategory(Category category);
}
