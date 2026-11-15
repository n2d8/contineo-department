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
16. Category names are unique and uniqueness is case-insensitive, so `footwear` and `FOOTWEAR` refer to the same category.
17. Instead of using pre-defined enums for categories and sub-categories, we allow free input of these values so we don't limit on the categorization of the inventory and can extend to future potential inventory products.
18. As this is a department store, there is no consideration of locking stock and decreasing it only at checkout. When a product is sold, the sales representative updates the inventory afterwards.
19. Category creation and category updates create sub-categories that do not already exist.
20. Updating a category replaces its complete sub-category list. A sub-category that is still referenced by an existing inventory record cannot be removed.

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

CREATE UNIQUE INDEX uq_category_name_upper ON category (UPPER(category_name));

CREATE UNIQUE INDEX uq_sub_category_name_upper ON sub_category (UPPER(sub_category_name));

CREATE UNIQUE INDEX uq_inventory_name_shop_upper ON inventory (UPPER(inventory_name), UPPER(shop_name));

CREATE INDEX idx_inventory_category_id ON inventory (category_id);
```

# Exception Handling

Custom exception classes were created to distinguish different types of errors and isolate application exceptions from exceptions thrown by common libraries.

The following Exceptions were created for the service:
- DuplicateEntityException: When the requests require creation of a resource but that requested resource already exists.
- IllegalOperationException: When a request contains an issue that prevents the service from fulfilling it.
- NotFoundException: When a request requires a resource that is expected to exist but cannot be found when the request is received.

The following shows the mapping of Exceptions and the HTTP error status codes:
| Exception |HTTP Status | Response Error | Example |
|---|---|---|---|
| DuplicateEntityException | 409 Conflict | Conflict | Category already exists: SomeExistingCategory |
| IllegalOperationException | 400 Bad Request | Bad Request | Cannot replace sub-categories in use. |
| NotFoundException | 404 Not Found | Not Found | Category not found: SomeUnknownCategory |
| MethodArgumentNotValidException | 400 Bad Request | Bad Request | name: must not be blank |
| HttpMessageNotReadableException | 400 Bad Request | Bad Request | Malformed or invalid request body. |
| MethodArgumentTypeMismatchException | 400 Bad Request | Bad Reques | Invalid value for path parameter: id |
| DataIntegrityViolationException | 409 Conflict | Conflict | The request conflicts with existing data. |
| Exception | 500 Internal Server Error | Internal Server Error | An unexpected error occurred. |

# API documentation

When the application is running, OpenAPI documentation is available at:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

# JWT Authentication

JWT authentication is an optional bonus feature. It is disabled by default and is enabled only when the `JWT_AUTH_ENABLED` environment variable is set to `true`. Use the environment variable to set a JWKS file from the local file system, set the expected audience and issuer, then enable authentication. Once enabled, the security filter chain requires authentication for inventory and category API routes.

Configure these environment variables to enable authentication:

| Environment variable | Purpose |
|---|---|
| `JWT_AUTH_ENABLED` | Set to `true` to require authentication for inventory and category API routes. |
| `JWT_JWK_SET_FILE` | Path to a JWKS JSON file containing public RSA signing keys. |
| `JWT_ISSUER` | Expected `iss` claim for incoming tokens. |
| `JWT_AUDIENCE` | Expected audience value in the token's `aud` claim. |

When enabled, authentication applies to all inventory and category API routes. Swagger UI and OpenAPI documentation remain publicly accessible. Send tokens using `Authorization: Bearer <token>`.

Tokens must be signed with RS256 and JWKs files must contain the matching public RSA key.

```powershell
# Set environment variables

$env:JWT_AUTH_ENABLED = "true"
$env:JWT_JWK_SET_FILE = "C:\path\to\jwks.json"
$env:JWT_ISSUER = "your-issuer"
$env:JWT_AUDIENCE = "your-api-audience"
```
