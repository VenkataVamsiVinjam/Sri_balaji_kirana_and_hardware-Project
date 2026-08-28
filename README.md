# Sri Balaji Kirana and Hardware - Inventory & Billing ERP

Spring Boot 3 / Spring Security / Spring Data JPA / MySQL 8 / Thymeleaf / Bootstrap 5 ERP
for a Maharashtra kirana + hardware shop. Handles dual-unit inventory (e.g. buy in Coils,
sell in Meters), GST-compliant billing (CGST+SGST), customer Udhaar (credit) tracking,
stock adjustments with a full audit trail, and PDF invoice generation/emailing.

## Tech Stack
- Java 17, Spring Boot 3.3.4
- Spring Security (form login, role-based access: ADMIN / CASHIER)
- Spring Data JPA + MySQL 8
- Thymeleaf + Bootstrap 5 (server-rendered UI, AJAX for POS/billing)
- iText 5 (PDF invoices), Spring Mail (emailing invoices)
- Maven

## Project Layout
```
src/main/java/com/sribalaji/erp/
  entity/        JPA entities (Product, Party, Invoice, StockAdjustment, ...)
  repository/    Spring Data JPA repositories
  service/       Business logic (dual-unit conversion, GST calc, stock, payments...)
  controller/    Thymeleaf page controllers
  controller/api/  REST controllers for AJAX (POS checkout, product search, payments)
  security/      Spring Security wiring (UserDetailsService, UserPrincipal)
  config/        Security config, JPA auditing, data seeding
  dto/           Request/response DTOs
  exception/     Custom exceptions + global handler
src/main/resources/
  application*.properties   dev/prod config
  templates/                Thymeleaf views
  static/                   CSS/JS
deploy/           systemd unit, nginx config, VPS bootstrap + deploy scripts
.github/workflows/  CI pipeline (build + test + package on every push)
```

## Running Locally (Dev)

1. **Install prerequisites**: JDK 17+, Maven 3.8+, MySQL 8 running locally.
2. **Create the database** (or let `createDatabaseIfNotExist=true` in
   `application-dev.properties` do it for you):
   ```sql
   CREATE DATABASE erp_dev CHARACTER SET utf8mb4;
   ```
3. **Set your local MySQL credentials** in `src/main/resources/application-dev.properties`
   (`spring.datasource.username` / `password`) and, if you want invoice emailing to
   work locally, your SMTP details.
4. **Run it**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
5. Open **http://localhost:8080** and log in with the seeded default accounts
   (created automatically on first run since the DB starts empty):
   - `admin` / `Admin@123` (full access)
   - `cashier` / `Cashier@123` (billing, stock view/adjust only)

   **Change these passwords immediately** via the Users screen (as `admin`) once
   you've logged in.

## Building a Deployable Jar
```bash
mvn clean package
# -> target/erp.jar
```

## Deploying to a VPS
See **[deploy/DEVOPS.md](deploy/DEVOPS.md)** for the full step-by-step DevOps guide:
server setup, systemd service, Nginx + free SSL, CI/CD with GitHub Actions, backups,
and day-2 operations.

## Key Business Rules Implemented
- **Dual units**: `Product.currentStock` is always stored in `purchaseUnit`. Sales are
  entered in `saleUnit` and converted via `conversionFactor` (see `InvoiceService`).
- **No interest, no credit limits** on customer Udhaar balances (see `PartyService`,
  `PaymentService`) - intentional, per business requirement.
- **Stock adjustments are isolated**: `StockAdjustmentService` never touches
  `Invoice`/`PurchaseOrder` tables, so damage/theft/expiry corrections never pollute
  Sales or Purchase reports - they only show up in the dedicated adjustment history.
- **Role enforcement**: `SecurityConfig` + `@PreAuthorize` restrict master-data deletion,
  GST Summary, and Outstanding reports to `ADMIN`; `CASHIER` can bill, view stock, and
  adjust stock but not delete data or see those two sensitive reports.
