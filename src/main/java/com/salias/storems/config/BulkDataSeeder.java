package com.salias.storems.config;

import com.salias.storems.model.emergencyinfo.EmergencyInfo;
import com.salias.storems.model.emergencyinfo.EmergencyInfoRepository;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.itemsupplier.ItemSupplier;
import com.salias.storems.model.itemsupplier.ItemSupplierRepository;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.sales.SalesRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.model.storemanager.StoreManager;
import com.salias.storems.model.storemanager.StoreManagerRepository;
import com.salias.storems.model.supplies.Supplies;
import com.salias.storems.model.supplies.SuppliesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Loads exactly {@value #ROWS_PER_TABLE} rows into each of the nine tables.
 *
 * <p>Run with {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=bulkseed}.
 * Refuses to run if any stores already exist, so it cannot silently double-load.
 *
 * <p>The sales rows are not uniform noise. Quantities are generated from a base
 * demand per store/product, an annual seasonal cycle, a weekend uplift, and a
 * price-elasticity term with intermittent promotions, so a model trained on this
 * data has real structure to find. The generator is seeded with a fixed value, so
 * the dataset is reproducible.
 */
@Component
@Profile("bulkseed")
public class BulkDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BulkDataSeeder.class);

    private static final int ROWS_PER_TABLE = 1000;

    /** Inventory and supplier rows cover this grid: 40 x 25 = 1000. */
    private static final int INVENTORY_STORES = 40;
    private static final int INVENTORY_PRODUCTS = 25;

    /** Sales concentrate on this grid so each series has history: 10 x 10 = 100 series. */
    private static final int SALES_STORES = 10;
    private static final int SALES_PRODUCTS = 10;
    private static final int SALES_PER_SERIES = ROWS_PER_TABLE / (SALES_STORES * SALES_PRODUCTS);

    /** Sales and deliveries are spread over this many months, ending today. */
    private static final int HISTORY_MONTHS = 24;

    private static final String[] CITIES = {
            "Toronto", "Montreal", "Vancouver", "Calgary", "Ottawa", "Edmonton", "Winnipeg",
            "Halifax", "Victoria", "Saskatoon", "Regina", "Windsor", "Kingston", "Guelph",
            "Kelowna", "Moncton", "Sudbury", "Barrie", "Laval", "Burnaby"
    };

    private static final String[] STREETS = {
            "Berwick", "Brampton", "Cargo", "Fulton", "Sunset", "Maple", "King", "Queen",
            "Dundas", "Bloor", "Yonge", "Bay", "Front", "College", "Spadina", "Adelaide",
            "Richmond", "Wellington", "Simcoe", "Peter", "John", "Church", "Jarvis", "Sherbourne",
            "Parliament"
    };

    private static final String[] CATEGORIES = {
            "Dairy", "Bakery", "Beverages", "Snacks", "Produce", "Frozen", "Household", "Personal Care"
    };

    private static final String[] MANUFACTURERS = {
            "Dairyland", "Baker Bros", "ThirstCo", "Crunch Foods", "GreenField", "FrostLine",
            "HomeEssentials", "PureCare", "Northern Mills", "Lakeside Foods"
    };

    private static final String[] PRODUCT_NOUNS = {
            "Milk", "Yogurt", "Cheese", "Loaf", "Bagels", "Juice", "Soda", "Chips", "Crackers",
            "Apples", "Bananas", "Peas", "Pizza", "Detergent", "Soap", "Shampoo", "Cereal",
            "Pasta", "Rice", "Coffee", "Tea", "Butter", "Eggs", "Salsa", "Granola"
    };

    private static final String[] SIZES = {"250g", "500g", "1L", "2L", "6-pack", "12-pack", "750ml", "1kg"};

    private static final String[] FIRST_NAMES = {
            "Ada", "Grace", "Alan", "Evy", "Daniel", "John", "Amara", "Liam", "Noah", "Olivia",
            "Emma", "Lucas", "Mia", "Ethan", "Zara", "Omar", "Priya", "Chen", "Sofia", "Mateo",
            "Nadia", "Yusuf", "Leah", "Kofi", "Ines"
    };

    private static final String[] LAST_NAMES = {
            "Kembou", "McFarlane", "Givings", "Okafor", "Nguyen", "Patel", "Silva", "Dubois",
            "Rossi", "Kowalski", "Haddad", "Tremblay", "Osei", "Mensah", "Fournier", "Lindqvist",
            "Novak", "Ferreira", "Bauer", "Costa"
    };

    private static final String[] ROLES = {
            "Cashier", "Store Associate", "Store Manager", "Shift Supervisor", "Stock Clerk",
            "Department Lead", "Customer Service"
    };

    private static final String[] RELATIONSHIPS = {
            "Spouse", "Parent", "Sibling", "Child", "Friend", "Guardian", "Partner"
    };

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;
    private final StoreManagerRepository storeManagerRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ItemSupplierRepository itemSupplierRepository;
    private final SuppliesRepository suppliesRepository;
    private final SalesRepository salesRepository;

    private final Random random = new Random(42);

    public BulkDataSeeder(StoreRepository storeRepository,
                          ProductRepository productRepository,
                          EmployeeRepository employeeRepository,
                          EmergencyInfoRepository emergencyInfoRepository,
                          StoreManagerRepository storeManagerRepository,
                          InventoryItemRepository inventoryItemRepository,
                          ItemSupplierRepository itemSupplierRepository,
                          SuppliesRepository suppliesRepository,
                          SalesRepository salesRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
        this.emergencyInfoRepository = emergencyInfoRepository;
        this.storeManagerRepository = storeManagerRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.itemSupplierRepository = itemSupplierRepository;
        this.suppliesRepository = suppliesRepository;
        this.salesRepository = salesRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (storeRepository.count() > 0) {
            log.warn("Data already present ({} stores). Seeder is refusing to run; "
                    + "truncate the tables first if you want a fresh load.", storeRepository.count());
            return;
        }

        log.info("Seeding {} rows per table...", ROWS_PER_TABLE);

        List<Store> stores = seedStores();
        List<Product> products = seedProducts();
        List<Employee> employees = seedEmployees(stores);
        seedEmergencyInfo(employees);
        seedStoreManagers(stores, employees);

        Map<String, BigDecimal> priceGrid = new HashMap<>();
        seedInventory(stores, products, priceGrid);
        List<ItemSupplier> suppliers = seedItemSuppliers(stores, products);
        seedSupplies(suppliers);
        seedSales(stores, products, priceGrid);

        log.info("Seeding complete.");
    }

    private List<Store> seedStores() {
        List<Store> stores = new ArrayList<>(ROWS_PER_TABLE);
        for (int i = 0; i < ROWS_PER_TABLE; i++) {
            String city = CITIES[i % CITIES.length];
            String street = STREETS[(i / CITIES.length) % STREETS.length];
            // The index keeps both name and address unique; address has a UNIQUE constraint.
            String name = "StoreMS %s #%04d".formatted(city, i + 1);
            String address = "%d %s St, %s".formatted(100 + i, street, city);
            stores.add(new Store(name, address));
        }
        return storeRepository.saveAll(stores);
    }

    private List<Product> seedProducts() {
        List<Product> products = new ArrayList<>(ROWS_PER_TABLE);
        for (int i = 0; i < ROWS_PER_TABLE; i++) {
            String category = CATEGORIES[i % CATEGORIES.length];
            String noun = PRODUCT_NOUNS[i % PRODUCT_NOUNS.length];
            String size = SIZES[i % SIZES.length];
            String manufacturer = MANUFACTURERS[i % MANUFACTURERS.length];
            String name = "%s %s %s #%04d".formatted(category, noun, size, i + 1);
            String description =
                    "%s category. %s %s supplied by %s. SKU %06d."
                            .formatted(category, noun, size, manufacturer, 100000 + i);
            products.add(new Product(name, manufacturer, description));
        }
        return productRepository.saveAll(products);
    }

    private List<Employee> seedEmployees(List<Store> stores) {
        List<Employee> employees = new ArrayList<>(ROWS_PER_TABLE);
        for (int i = 0; i < ROWS_PER_TABLE; i++) {
            String first = FIRST_NAMES[i % FIRST_NAMES.length];
            String last = LAST_NAMES[(i / FIRST_NAMES.length) % LAST_NAMES.length];
            // Employee i staffs store i: store_manager needs 1000 distinct
            // (store, employee) pairs, and manager_id is UNIQUE.
            Store store = stores.get(i);
            LocalDate dob = LocalDate.now()
                    .minusYears(20 + random.nextInt(40))
                    .minusDays(random.nextInt(365));
            String address = "%d %s Ave, %s"
                    .formatted(1 + random.nextInt(900),
                            STREETS[i % STREETS.length],
                            CITIES[i % CITIES.length]);
            String role = ROLES[i % ROLES.length];
            employees.add(new Employee(
                    "%s%d".formatted(first, i + 1), last, dob, address, role, store));
        }
        return employeeRepository.saveAll(employees);
    }

    private void seedEmergencyInfo(List<Employee> employees) {
        List<EmergencyInfo> contacts = new ArrayList<>(ROWS_PER_TABLE);
        for (int i = 0; i < ROWS_PER_TABLE; i++) {
            Employee employee = employees.get(i);
            String contactName = "%s %s".formatted(
                    FIRST_NAMES[(i + 7) % FIRST_NAMES.length],
                    LAST_NAMES[(i + 3) % LAST_NAMES.length]);
            String phone = "+1-%03d-%03d-%04d".formatted(
                    200 + random.nextInt(700), random.nextInt(1000), random.nextInt(10000));
            contacts.add(new EmergencyInfo(
                    contactName, phone, RELATIONSHIPS[i % RELATIONSHIPS.length], employee));
        }
        emergencyInfoRepository.saveAll(contacts);
    }

    private void seedStoreManagers(List<Store> stores, List<Employee> employees) {
        List<StoreManager> assignments = new ArrayList<>(ROWS_PER_TABLE);
        for (int i = 0; i < ROWS_PER_TABLE; i++) {
            assignments.add(new StoreManager(stores.get(i), employees.get(i)));
        }
        storeManagerRepository.saveAll(assignments);
    }

    private void seedInventory(List<Store> stores,
                               List<Product> products,
                               Map<String, BigDecimal> priceGrid) {
        List<InventoryItem> items = new ArrayList<>(ROWS_PER_TABLE);
        for (int s = 0; s < INVENTORY_STORES; s++) {
            for (int p = 0; p < INVENTORY_PRODUCTS; p++) {
                Store store = stores.get(s);
                Product product = products.get(p);
                BigDecimal unitPrice = basePrice(p)
                        // small per-store price variation, so price is not collinear with product
                        .multiply(BigDecimal.valueOf(0.95 + (s % 5) * 0.025))
                        .setScale(2, RoundingMode.HALF_UP);
                priceGrid.put(s + ":" + p, unitPrice);
                items.add(new InventoryItem(
                        store,
                        product,
                        shortName(product),
                        unitPrice,
                        50 + random.nextInt(450)));
            }
        }
        inventoryItemRepository.saveAll(items);
    }

    private List<ItemSupplier> seedItemSuppliers(List<Store> stores, List<Product> products) {
        List<ItemSupplier> suppliers = new ArrayList<>(ROWS_PER_TABLE);
        for (int s = 0; s < INVENTORY_STORES; s++) {
            for (int p = 0; p < INVENTORY_PRODUCTS; p++) {
                // One supplier per product line, shared across stores.
                long supplierId = 5000L + p;
                suppliers.add(new ItemSupplier(supplierId, stores.get(s), products.get(p)));
            }
        }
        return itemSupplierRepository.saveAll(suppliers);
    }

    private void seedSupplies(List<ItemSupplier> suppliers) {
        LocalDateTime start = LocalDateTime.now().minusMonths(HISTORY_MONTHS);
        long totalMinutes = HISTORY_MONTHS * 30L * 24 * 60;

        List<Supplies> deliveries = new ArrayList<>(ROWS_PER_TABLE);
        for (int i = 0; i < ROWS_PER_TABLE; i++) {
            ItemSupplier supplier = suppliers.get(i % suppliers.size());
            LocalDateTime suppliedAt = start.plusMinutes(random.nextLong(totalMinutes));
            deliveries.add(new Supplies(
                    supplier,
                    1 + random.nextInt(60),
                    "Pallet %06d, %d boxes, gate %d".formatted(700000 + i, 1 + i % 60, 1 + i % 8),
                    suppliedAt));
        }
        suppliesRepository.saveAll(deliveries);
    }

    /**
     * Generates sales with learnable structure rather than uniform noise:
     * base demand per store/product, an annual seasonal cycle, a weekend uplift,
     * price elasticity, and intermittent promotions.
     */
    private void seedSales(List<Store> stores,
                           List<Product> products,
                           Map<String, BigDecimal> priceGrid) {
        List<Sales> sales = new ArrayList<>(ROWS_PER_TABLE);
        LocalDateTime start = LocalDateTime.now().minusMonths(HISTORY_MONTHS);
        long stepMinutes = (HISTORY_MONTHS * 30L * 24 * 60) / SALES_PER_SERIES;

        for (int s = 0; s < SALES_STORES; s++) {
            for (int p = 0; p < SALES_PRODUCTS; p++) {
                BigDecimal listPrice = priceGrid.get(s + ":" + p);
                double baseDemand = 4 + (s % 5) * 3 + (p % 4) * 2;

                for (int n = 0; n < SALES_PER_SERIES; n++) {
                    LocalDateTime madeAt = start
                            .plusMinutes(n * stepMinutes + random.nextInt((int) stepMinutes));

                    // Annual cycle peaking in December and bottoming out in June,
                    // which is the shape retail grocery demand actually has.
                    // sin() hits +1 at month 12 with this offset.
                    double seasonal = 1 + 0.45 * Math.sin(
                            2 * Math.PI * (madeAt.getMonthValue() - 9) / 12.0);

                    // Weekends are busier.
                    int dow = madeAt.getDayOfWeek().getValue();
                    double weekend = (dow >= 6) ? 1.35 : 1.0;

                    // Roughly one in six sales happens under a 20% promotion.
                    boolean promo = random.nextInt(6) == 0;
                    BigDecimal effectivePrice = promo
                            ? listPrice.multiply(BigDecimal.valueOf(0.80))
                                    .setScale(2, RoundingMode.HALF_UP)
                            : listPrice;

                    // Price elasticity: cheaper unit price lifts quantity.
                    double elasticity = Math.pow(
                            listPrice.doubleValue() / effectivePrice.doubleValue(), 1.6);

                    double noise = 0.85 + random.nextDouble() * 0.30;

                    int quantity = (int) Math.max(1, Math.round(
                            baseDemand * seasonal * weekend * elasticity * noise));

                    BigDecimal total = effectivePrice
                            .multiply(BigDecimal.valueOf(quantity))
                            .setScale(2, RoundingMode.HALF_UP);

                    sales.add(new Sales(
                            products.get(p), stores.get(s), quantity, total, madeAt));
                }
            }
        }
        salesRepository.saveAll(sales);
    }

    private BigDecimal basePrice(int productIndex) {
        // Deterministic spread of prices from about 1.50 to 24.00.
        double price = 1.50 + (productIndex % 25) * 0.90;
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }

    private String shortName(Product product) {
        String name = product.getProductName();
        int hash = name.indexOf(" #");
        return hash > 0 ? name.substring(0, hash) : name;
    }
}
