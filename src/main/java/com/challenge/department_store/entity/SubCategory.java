package com.challenge.department_store.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity 
@Table(name = "sub_category")
public class SubCategory {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_category_name", nullable = false, length = 100, unique = true)
    private String subCategoryName;

    @ManyToMany(mappedBy = "subCategories")
    private Set<Category> categories = new HashSet<>();

    protected SubCategory() {}

    public SubCategory(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return subCategoryName;
    }

    public Set<Category> getCategories() {
        return categories;
    }
}
