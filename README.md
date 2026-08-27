# storems — Store Management System

A Spring Boot / JPA backend for managing stores, staff, product catalogue,
per-store inventory, suppliers and sales.

- Java 21
- Spring Boot 4.0.1 (Spring Data JPA, Bean Validation)
- PostgreSQL in development and production, H2 for tests

## Requirements

- JDK 21+
- PostgreSQL 14+ running locally
- No Maven install needed — use the bundled wrapper (`./mvnw`, or `mvnw.cmd` on Windows)

## Configuration

Database credentials are read from the environment; nothing secret is committed.

| Variable               | Required | Default                                        |
| ---------------------- | -------- | ---------------------------------------------- |
| `STOREMS_DB_URL`       | no       | `jdbc:postgresql://localhost:5432/storems`      |
| `STOREMS_DB_USER`      | no       | `postgres`                                     |
| `STOREMS_DB_PASSWORD`  | **yes**  | —                                              |

Create the database once:

```sql
CREATE DATABASE storems;
```

Then set the password for your shell session.

PowerShell:

```powershell
$env:STOREMS_DB_PASSWORD = "your-password"
```

bash / zsh:

```bash
export STOREMS_DB_PASSWORD="your-password"
```

## Running

```bash
./mvnw spring-boot:run
```

To start with sample stores, employees, products and stock loaded:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` seeder skips itself if any stores already exist.

## Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database, so they need neither a local PostgreSQL
instance nor any credentials.

## Project layout

```
com.salias.storems
├── StoremsApplication      entry point
├── config                  DevDataSeeder (dev profile only)
├── model                   entities + Spring Data repositories, one package per aggregate
│   ├── emergencyinfo       employee emergency contacts
│   ├── employee            staff records
│   ├── inventoryitem       per-store stock of a product (composite key)
│   ├── itemsupplier        which supplier provides which product to which store
│   ├── product             product catalogue
│   ├── sales               individual sales
│   ├── store               store locations
│   ├── storemanager        store → manager assignment
│   └── supplies            deliveries received
└── service                 transactional business logic
```

## Data model notes

- `InventoryItem` and `ItemSupplier` use composite keys (`@EmbeddedId` +
  `@MapsId`). Their ID classes implement `equals`/`hashCode`, which JPA requires
  for `findById` to work.
- `StoreManager` derives its primary key from the store (`@MapsId`), so a store
  has at most one manager; a unique constraint on `manager_id` stops one employee
  from managing two stores.
- `Supplies` does not store the store and product again — its `ItemSupplier`
  reference already identifies both, exposed via `getStoreSupplied()` and
  `getItemSupplied()`.
- Timestamps (`Sales.madeAt`, `Supplies.suppliedAt`) are set by Hibernate's
  `@CreationTimestamp` on insert.

## Known gaps

These are deliberate and not yet implemented:

- **No `Supplier` entity.** `ItemSupplier.supplierId` is a bare number, so
  supplier names and contact details have nowhere to live. Adding a `Supplier`
  entity and pointing `ItemSupplierId` at it is the next modelling step.
- **No HTTP layer.** There are no controllers or DTOs; the services are only
  reachable from Java. Add `spring-boot-starter-web` when an API is needed.
- **No schema migrations.** `spring.jpa.hibernate.ddl-auto=update` lets Hibernate
  patch the schema, which is fine locally but unsafe for a deployed database.
  Move to Flyway or Liquibase before deploying.
- **No authentication or authorisation.**
