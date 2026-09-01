# storems — Store Management System

A Spring Boot web application for managing small retail/grocery store operations.
Each user is scoped to a single store. Managers handle day-to-day operations
(dashboard, inventory, catalogue, staff, reports); employees work the POS till
and view their own sales.

## Tech stack

- Java 21
- Spring Boot 4.0.1 (Spring Data JPA, Spring Web/MVC, Bean Validation)
- Spring Security (form login, BCrypt, role-based access)
- Thymeleaf (server-rendered UI)
- OpenPDF (librepdf 2.0.3) for PDF report generation
- PostgreSQL in development/production, H2 for tests
- Maven (bundled wrapper)

## Requirements

- JDK 21+
- PostgreSQL 14+ running locally (not needed for tests)
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

### Seed profiles

| Profile     | Description |
| ----------- | ----------- |
| `dev`       | Small sample dataset (a few stores, employees, products and stock) |
| `demoseed`  | 3 stores × (1 manager + 4 staff), 25 products, several days of sales. Every login shares one demo password — useful for exercising both roles in the UI. |
| `bulkseed`  | 1000 rows per table with learnable patterns for forecasting work (see below). |

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=demoseed
./mvnw spring-boot:run -Dspring-boot.run.profiles=bulkseed
```

All seeders refuse to run if any stores already exist. To reload, truncate first:

```sql
TRUNCATE sales, supplies, item_supplier, inventory_item, store_manager,
         emergency_info, employee, product, store RESTART IDENTITY CASCADE;
```

## Bulk dataset for forecasting work

The `bulkseed` profile loads exactly 1000 rows into each of the nine tables.
Sales rows are not uniform noise — quantity is generated from a base demand
per store/product, an annual cycle peaking in December, a 1.35x weekend uplift,
and a price-elasticity term with roughly one sale in six under a 20% promotion.
The generator uses a fixed seed, so the dataset is reproducible.

| Table | Rows | Shape |
| --- | --- | --- |
| `store`, `product`, `employee`, `emergency_info`, `store_manager` | 1000 each | employee *i* staffs and manages store *i* |
| `inventory_item`, `item_supplier` | 1000 each | grid of 40 stores x 25 products |
| `supplies` | 1000 | deliveries sampled over 24 months |
| `sales` | 1000 | 10 stores x 10 products = 100 series x 10 points, over 24 months |

## Security

- Two roles: `ROLE_STORE_MANAGER` (full access) and `ROLE_EMPLOYEE` (POS + own sales only).
  Managers also hold `ROLE_EMPLOYEE` so they can work a till.
- Store scoping is enforced from the authenticated principal (`StoremsPrincipal`), never
  trusted from URL path parameters. `CurrentUser.assertOwnStore()` guards every request.
- Successful login routes managers to `/dashboard`, staff to `/pos`.
- Manager-only areas: `/dashboard`, `/inventory`, `/products`, `/employees`, `/sales`, `/reports`.
- Till areas: `/pos`, `/my-sales`.

## Features

- **POS Till** — Ring up sales with a session basket; prices and stock are read
  from the DB server-side and the whole checkout is one transaction (rolls back
  on insufficient stock).
- **Dashboard** — Today's revenue, low-stock alerts (threshold: 20 units), per-seller shift reports.
- **Inventory** — Per-store stock tracking, adjust quantity, set price, add/remove products,
  receive supplier deliveries.
- **Product Catalogue** — CRUD with soft discontinue; hard delete only when nothing references the product.
- **Employee Management** — Staff records, set/revoke login credentials, emergency contacts.
- **Sales Reports** — Filter by date/employee, download as CSV or PDF.
- **End-of-Shift Reports** — Employees get a summary when they log out.

## Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database, so they need neither a local PostgreSQL
instance nor any credentials.

## Project layout

```
com.salias.storems
├── StoremsApplication          entry point
├── config                      data seeders (dev, demoseed, bulkseed profiles)
├── controller                  Thymeleaf MVC controllers, session basket, model advice
├── model                       entities + Spring Data repositories, one package per aggregate
│   ├── emergencyinfo           employee emergency contacts
│   ├── employee                staff records
│   ├── inventoryitem           per-store stock of a product (composite key)
│   ├── itemsupplier            which supplier provides which product to which store
│   ├── product                 product catalogue
│   ├── sales                   individual sales
│   ├── store                   store locations
│   ├── storemanager            store → manager assignment
│   └── supplies                deliveries received
├── report                      sales report service + CSV/PDF writers
├── security                    SecurityConfig, principal, roles, user details service
└── service                     transactional business logic
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
- Timestamps (`Sales.madeAt`, `Supplies.suppliedAt`) are assigned explicitly in
  Java rather than via `@CreationTimestamp`, because the date a sale happened is
  business data — an audit-generated timestamp would make historical backfill
  impossible.

## Known gaps

- **No `Supplier` entity.** `ItemSupplier.supplierId` is a bare number, so
  supplier names and contact details have nowhere to live. Adding a `Supplier`
  entity and pointing `ItemSupplierId` at it is the next modelling step.
- **No schema migrations.** `spring.jpa.hibernate.ddl-auto=update` lets Hibernate
  patch the schema, which is fine locally but unsafe for a deployed database.
  Move to Flyway or Liquibase before deploying.
