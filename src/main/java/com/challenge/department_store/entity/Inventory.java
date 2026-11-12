package com.challenge.department_store.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity 
@Table(name = "inventory", 
        uniqueConstraints = @UniqueConstraint(
            name = "unique_inventory_name_shop_name", 
            columnNames = {"inventory_name", "shop_name"}))
public class Inventory {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_name", length = 100, nullable = false)
    private String name;

    @Column(name = "shop_name", length = 100, nullable = false)
    private String shopName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "inventory_sub_category",
        joinColumns = @JoinColumn(name = "inventory_id"),
        inverseJoinColumns = @JoinColumn(name = "sub_category_id")
    )
    private Set<SubCategory> subCategories = new HashSet<>(); 
    
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "description", length = 500)
    private String description;

    protected Inventory() {}

    public Inventory(String inventoryName, 
                        String shopName, 
                        Category category, 
                        int quantity, 
                        String description, 
                        Set<SubCategory> subCategories) {
        this.name = inventoryName;
        this.shopName = shopName;
        this.category = category;
        this.quantity = quantity;
        this.description = description;
        this.subCategories = subCategories;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<SubCategory> getSubCategories() {
        return subCategories;
    }

    public void setSubCategories(Set<SubCategory> subCategories) {
        this.subCategories = subCategories;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
