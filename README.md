# Department Store

Spring Boot Inventory API for Department Store

This application provides a REST API for managing product inventory in a single department store. Each inventory record represents one product sold by the store and contains the product's inventory name, category, sub-category, and available quantity.

The service supports creating inventory records, replacing stock quantities, and browsing inventory. Deleting an inventory record is an additional feature representing the end of the store's arrangement to sell a product; an out-of-stock product remains registered with a quantity of zero.


# Scope

This application implements an inventory service for the department store, the main features of this service:
- Inventory Management (Create, Update and browse Inventory records)
    - Inventory Name
    - Inventory category
    - Inventory sub-category
    - Quantity
    - Description
- Validation Rules
    - Validation rules to determine category and sub-category compatibility.

# Requirements and Assumptions

The service tracks the product inventory of the Department Store.

The following contains the assumptions and requirements to implement the service:
1. Create an inventory for a specific product to be sold in the department store.
2. Track the inventory record for that specific department store.
3. Update an inventory for a specified product inventory.
4. Ensure the created inventory category and sub-category aligns.
5. For simplicity, each inventory only tracks 1 product.
6. A product is not maintained as a separate entity by the service; it is represented by its inventory record.
7. Deleting an inventory is a bonus feature.
8. An inventory record may be deleted regardless of its remaining quantity.
9. Deleting an inventory record represents the retirement of a product, typically because its supply contract has expired.
10. Any remaining quantity is assumed to be returned to the provider and is not transferred to another inventory record and is no longer managed by the service.
11. Browsing of inventory records provides a snapshot of all existing inventory at the point in time of the query.

# API Design

Outline the endpoints needed in the API to fulfil the above requirements and assumptions.
- GET api/inventories
    - Browse existing inventory records.
- GET api/inventories/{id}
    - Retrieve a specified inventory record.
- POST api/inventories
    - Create a new inventory record for a product.
- PUT api/inventories/{id}
    - Update an existing inventory record.
- DELETE api/inventories/{id}
    - Delete an existing inventory record and retire the associated product.
- GET api/categories
    - Retrieve a list of existing categories and their sub-categories.
- GET api/categories/{name}
    - Retrieve a list of sub-categories of a category.
- POST api/categories
    - Add a new category and its sub-categories.
- PUT api/categories/{name}
    - Update the list of sub-categories of a category.
    - Validate the updated sub-category list against all existing inventory records that use the category.
    - Reject the update if it would remove a sub-category that is still used by an existing inventory record.
    - Return an error identifying the sub-category or sub-categories that must remain available.
