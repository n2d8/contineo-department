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

# Requirements, Decisions and Assumptions

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
12. Unknown API fields in the JSON bodies are rejected with 400 bad request error.
13. Category and sub-category have many to many relationship. "Footwear" and "shoe" can both be a category while a sub-category named "trainers" can be categorized as both footwear and shoe.
14. Inventory name is not unique, so different users can create an inventory for their specific product even if they have the same product name, however the combination of the inventory name and the shop name must be unique ignoring case.
15. Secure the API calls with JWK and JWT pair as bonus.
16. Category names are unique and the uniquness is case insensitive, so footwear and FOOTWEAR results the same category.
17. Instead of using pre-defined enums for categories and sub-categories, we allow free input of these values so we don't limit on the categorization of the inventory and can extend to future potential inventory products.
18. As this is a department store, their are no consideration of "locking a stock and decrease only on checkout". When a product is sold, the sales representative of the shop updates the inventory afterwards.

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
- PATCH api/inventories/{id}/quantity
    - Update an existing inventory record's quantity.
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

# Database Schema

The current database design is used for H2 database and outline the database table schema for the service. Further considerations will be needed if the service is deployed across multiple database instances where a globally unique identifier, such as UUID, may be preferred over auto-increment ID.

```sql
CREATE TABLE category (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE sub_category (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sub_category_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE category_sub_category (
    category_id     BIGINT NOT NULL,
    sub_category_id BIGINT NOT NULL,
    PRIMARY KEY (category_id, sub_category_id),
    FOREIGN KEY (category_id) REFERENCES category(id),
    FOREIGN KEY (sub_category_id) REFERENCES sub_category(id)
);

CREATE TABLE inventory (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    shop_name   VARCHAR(100) NOT NULL,
    quantity    INT NOT NULL,
    description VARCHAR(500),
    FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT unique_inventory_name_shop_name UNIQUE (inventory_name, shop_name)
);

CREATE TABLE inventory_sub_category (
    inventory_id    BIGINT NOT NULL,
    sub_category_id BIGINT NOT NULL,
    PRIMARY KEY (inventory_id, sub_category_id),
    FOREIGN KEY (inventory_id) REFERENCES inventory(id),
    FOREIGN KEY (sub_category_id) REFERENCES sub_category(id)
);

CREATE UNIQUE INDEX uq_inventory_name_shop_upper
    ON inventory (UPPER(inventory_name), UPPER(shop_name));
```
# Exception Handling

Custom Exception classes were created for this application to disguish different type of errors and isolate Exceptions from common libraries and the application service Exception.

The following are the Eceptions that were created for the service:
- DuplicateEntityException: When the requests require creation of a resource but that requested resource already exists.
- IllegalOperationException: When the requests itself contains issues leading to the service cannot be fulfilled.
- NotFounException: When the requests require action on resource that expects to exist but can no longer be found at the time of the request was received.

The following shows the mapping of Exceptions and the HTTP error status codes:
|Exception|HTTP Status|Response Error|Example|
|DuplicateEntityException|409 Conflict|Conflict|Category already exists: SomeExistingCategory|
|IllegalOperationException|400 Bad Request|Bad Request|Cannot replace sub-categories in use.|
|NotFoundException|404 Not Found|NotFound|Category not found: SomeUnknownCategory|
|MethodArgumentNotValidException|400 Bad Request|Bad Request|name: must not be blank|
|HttpMessageNotReadableException|400 Bad Request|Bad Request|Malformed or invalid request body|
|MethodArgumentTypeMismatchException|400 Bad Request|Bad Request|Invalid value for path parameter: id|
|DataIntegrityViolationException|409 Conflict|Conflict|The request conflicts with existing data|
|Exception|500 Internal Server Error|Internal Server Error|An unexpected error occurred|
