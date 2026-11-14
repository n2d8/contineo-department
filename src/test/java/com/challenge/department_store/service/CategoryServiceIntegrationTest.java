package com.challenge.department_store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.challenge.department_store.dto.CategoryResponse;
import com.challenge.department_store.dto.CreateCategoryRequest;
import com.challenge.department_store.dto.UpdateCategoryRequest;
import com.challenge.department_store.entity.Category;
import com.challenge.department_store.entity.SubCategory;
import com.challenge.department_store.repository.CategoryRepository;
import com.challenge.department_store.repository.SubCategoryRepository;
import jakarta.persistence.EntityNotFoundException;

@DataJpaTest
@Import(CategoryService.class)
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testGetCategoryByNameLoadsLazySubCategoriesWithoutLazyInitializationException() {
        String suffix = UUID.randomUUID().toString();
        String categoryName = "Footwear " + suffix;
        String subCategoryName = "Shoe " + suffix;
        Category category = new Category(categoryName);
        category.addSubCategory(new SubCategory(subCategoryName));

        persistCategory(category);

        CategoryResponse response =
                categoryService.getCategoryByName(categoryName);

        assertThat(response.name()).isEqualTo(categoryName);
        assertThat(response.subCategories()).containsExactly(subCategoryName);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testGetAllCategoriesLoadsLazySubCategoriesWithoutLazyInitializationException() {
        String suffix = UUID.randomUUID().toString();
        String footwearName = "Footwear " + suffix;
        String shoeName = "Shoe " + suffix;
        String clothingName = "Clothing " + suffix;
        String shirtName = "Shirt " + suffix;
        Category footwear = new Category(footwearName);
        footwear.addSubCategory(new SubCategory(shoeName));

        Category clothing = new Category(clothingName);
        clothing.addSubCategory(new SubCategory(shirtName));

        persistCategory(footwear);
        persistCategory(clothing);

        var responses = categoryService.getAllCategories();

        assertThat(responses)
                .extracting(CategoryResponse::name)
                .contains(footwearName, clothingName);

        assertThat(responses)
                .filteredOn(response -> response.name().equals(footwearName))
                .extracting(CategoryResponse::subCategories)
                .containsExactly(Set.of(shoeName));
        assertThat(responses)
                .filteredOn(response -> response.name().equals(clothingName))
                .extracting(CategoryResponse::subCategories)
                .containsExactly(Set.of(shirtName));
    }

    @Test
    void testCreateCategoryPersistsCategoryAndCreatesMissingSubCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Footwear",
                Set.of("Shoe")
        );

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Footwear");
        assertThat(response.subCategories()).containsExactly("Shoe");

        assertThat(categoryRepository.findByNameIgnoreCase("Footwear"))
                .isPresent();

        assertThat(subCategoryRepository.findByNameIgnoreCase("Shoe"))
                .isPresent();
    }

    @Test
    void testCreateCategoryReusesExistingSubCategoryInsteadOfCreatingDuplicate() {
        SubCategory existingSubCategory = new SubCategory("Shoe");
        entityManager.persistAndFlush(existingSubCategory);

        CreateCategoryRequest request = new CreateCategoryRequest(
                "Footwear",
                Set.of("Shoe")
        );

        categoryService.createCategory(request);

        assertThat(subCategoryRepository.findAll())
                .filteredOn(subCategory -> subCategory.getName().equals("Shoe"))
                .hasSize(1);
    }

    @Test
    void testCreateCategoryRejectsDuplicateCaseInsensitiveCategoryName() {
        categoryService.createCategory(new CreateCategoryRequest(
                "Footwear",
                Set.of("Shoe")
        ));

        assertThatThrownBy(() ->
                categoryService.createCategory(new CreateCategoryRequest(
                        "footwear",
                        Set.of("Sneaker")
                ))
        ).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testUpdateCategoryReplacesExistingSubCategoriesWithRequestSubCategories() {
        Category category = new Category("Footwear");
        category.addSubCategory(new SubCategory("Shoe"));

        persistCategory(category);

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                Set.of("Sneaker", "Boot")
        );

        CategoryResponse response =
                categoryService.updateCategory(request, "Footwear");

        assertThat(response.name()).isEqualTo("Footwear");
        assertThat(response.subCategories())
                .containsExactlyInAnyOrder("Sneaker", "Boot");

        Category updatedCategory = categoryRepository
                .findByNameIgnoreCase("Footwear")
                .orElseThrow();

        assertThat(updatedCategory.getSubCategories())
                .extracting(SubCategory::getName)
                .containsExactlyInAnyOrder("Sneaker", "Boot");
    }

    @Test
    void testUpdateCategoryWithEmptySubCategoryListRemovesAllSubCategoryAssociations() {
        Category category = new Category("Footwear");
        category.addSubCategory(new SubCategory("Shoe"));

        persistCategory(category);

        UpdateCategoryRequest request = new UpdateCategoryRequest(Set.of());

        CategoryResponse response =
                categoryService.updateCategory(request, "Footwear");

        assertThat(response.subCategories()).isEmpty();

        Category updatedCategory = categoryRepository
                .findByNameIgnoreCase("Footwear")
                .orElseThrow();

        assertThat(updatedCategory.getSubCategories()).isEmpty();
    }

    @Test
    void testUpdateCategoryThrowsEntityNotFoundExceptionWhenCategoryDoesNotExist() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                Set.of("Shoe")
        );

        assertThatThrownBy(() ->
                categoryService.updateCategory(request, "Unknown")
        ).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testCreateCategoryRollsBackWhenSubCategoryPersistenceFails() {
        String categoryName = "Rollback " + UUID.randomUUID();
        String subCategoryName = "S".repeat(101);

        assertThatThrownBy(() ->
                categoryService.createCategory(new CreateCategoryRequest(
                        categoryName,
                        Set.of(subCategoryName)
                ))
        ).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(categoryRepository.findByNameIgnoreCase(categoryName))
                .isEmpty();
        assertThat(subCategoryRepository.findByNameIgnoreCase(subCategoryName))
                .isEmpty();
    }

    private void persistCategory(Category category) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            category.getSubCategories().forEach(entityManager::persist);
            entityManager.persistAndFlush(category);
            entityManager.clear();
        });
    }
}