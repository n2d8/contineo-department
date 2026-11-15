package com.challenge.department_store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.Inventory;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from Inventory inventory where inventory.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);

    Optional<Inventory> findByNameIgnoreCaseAndShopNameIgnoreCase(String inventoryName, String shopName);

    List<Inventory> findByCategory(Category category);
}
