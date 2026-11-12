package com.challenge.department_store.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity 
@Table(name = "category")
public class Category {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, length = 100, unique = true)
    private String name;

    @ManyToMany 
    @JoinTable(name = "category_sub_category", 
        joinColumns = @JoinColumn(name = "category_id"), 
        inverseJoinColumns = @JoinColumn(name = "sub_category_id"))
    private Set<SubCategory> subCategories = new HashSet<>();

    protected Category() {}

    public Category(String categoryName) {
        this.name = categoryName;   
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<SubCategory> getSubCategories() {
        return subCategories;
    }

    public void addSubCategory(SubCategory subCategory) {
        subCategories.add(subCategory);
        subCategory.getCategories().add(this);
    }
}
