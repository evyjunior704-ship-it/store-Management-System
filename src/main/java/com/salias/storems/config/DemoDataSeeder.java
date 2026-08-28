package com.salias.storems.config;

import com.salias.storems.model.emergencyinfo.EmergencyInfo;
import com.salias.storems.model.emergencyinfo.EmergencyInfoRepository;
import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.sales.SalesRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.model.storemanager.StoreManager;
import com.salias.storems.model.storemanager.StoreManagerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A small, realistic dataset for exercising the UI and the two roles.
 *
 * <p>Run with {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=demoseed}.
 *
 * <p>Unlike the bulk seeder, this produces a shape you can actually log into:
 * three stores, each with one manager and four staff, so manager-only screens and
 * employee-only screens are both reachable. Every login shares one password,
 * which is why this profile is for local use only.
 */
@Component
@Profile("demoseed")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final int STORES = 3;
    private static final int STAFF_PER_STORE = 4;
    private static final int PRODUCTS = 25;
    private static final int SALES_DAYS = 5;

    private static final String[][] STORE_DEFS = {
            {"Riverside Grocery", "50 Berwick St, Toronto"},
            {"Brampton Market", "30 Brampton Ave, Brampton"},
            {"Lakeshore Foods", "12 Lakeshore Rd, Burlington"}
    };

    private static final String[][] PRODUCT_DEFS = {
            {"Whole Milk 2L", "Dairyland", "Pasteurised whole milk, 2 litre carton", "3.49"},
            {"Skim Milk 1L", "Dairyland", "Skimmed milk, 1 litre", "2.79"},
            {"Greek Yogurt 500g", "Dairyland", "Plain Greek yogurt", "4.99"},
            {"Cheddar Block 400g", "Northern Mills", "Aged cheddar", "6.49"},
            {"Butter 250g", "Dairyland", "Salted butter", "4.29"},
            {"Sourdough Loaf", "Baker Bros", "Stone-baked sourdough, 800g", "4.25"},
            {"Bagels 6-pack", "Baker Bros", "Plain bagels", "3.99"},
            {"Croissants 4-pack", "Baker Bros", "Butter croissants", "5.49"},
            {"Orange Juice 1L", "ThirstCo", "Not from concentrate", "3.89"},
            {"Cola 12-pack", "ThirstCo", "Cans, 355ml each", "8.99"},
            {"Sparkling Water 6-pack", "ThirstCo", "Unflavoured", "5.29"},
            {"Ground Coffee 500g", "Lakeside Foods", "Medium roast", "11.49"},
            {"Tea Bags 100ct", "Lakeside Foods", "English breakfast", "6.19"},
            {"Potato Chips 200g", "Crunch Foods", "Sea salt", "3.29"},
            {"Crackers 250g", "Crunch Foods", "Water crackers", "2.99"},
            {"Granola 750g", "Lakeside Foods", "Oat and honey", "7.49"},
            {"Cereal 500g", "Northern Mills", "Bran flakes", "5.79"},
            {"Pasta 1kg", "Northern Mills", "Durum wheat penne", "3.59"},
            {"Rice 2kg", "Northern Mills", "Long grain white", "8.29"},
            {"Apples 1kg", "GreenField", "Gala apples", "4.49"},
            {"Bananas 1kg", "GreenField", "Cavendish", "2.19"},
            {"Frozen Peas 1kg", "FrostLine", "Garden peas", "3.99"},
            {"Frozen Pizza", "FrostLine", "Margherita, 400g", "6.99"},
            {"Laundry Detergent 2L", "HomeEssentials", "Concentrated", "12.49"},
            {"Shampoo 400ml", "PureCare", "Everyday care", "7.29"}
    };

    private static final String[][] MANAGERS = {
            {"John", "Givings", "manager.riverside"},
            {"Amara", "Okafor", "manager.brampton"},
            {"Grace", "Osei", "manager.lakeshore"}
    };

    private static final String[][] STAFF = {
            {"Ada", "Kembou"}, {"Daniel", "McFarlane"}, {"Liam", "Nguyen"}, {"Mia", "Patel"},
            {"Omar", "Haddad"}, {"Sofia", "Silva"}, {"Ethan", "Tremblay"}, {"Nadia", "Dubois"},
            {"Kofi", "Mensah"}, {"Leah", "Rossi"}, {"Yusuf", "Bauer"}, {"Ines", "Costa"}
    };

    private static final String[] ROLES = {"Cashier", "Store Associate", "Stock Clerk", "Shift Supervisor"};
    private static final String[] RELATIONSHIPS = {"Spouse", "Parent", "Sibling", "Friend"};

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;
    private final StoreManagerRepository storeManagerRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SalesRepository salesRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${storems.demo.password:demo1234}")
    private String demoPassword;

    private final Random random = new Random(7);

    public DemoDataSeeder(StoreRepository storeRepository,
                          ProductRepository productRepository,
                          EmployeeRepository employeeRepository,
                          EmergencyInfoRepository emergencyInfoRepository,
                          StoreManagerRepository storeManagerRepository,
                          InventoryItemRepository inventoryItemRepository,
                          SalesRepository salesRepository,
                          PasswordEncoder passwordEncoder) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
        this.emergencyInfoRepository = emergencyInfoRepository;
        this.storeManagerRepository = storeManagerRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.salesRepository = salesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (storeRepository.count() > 0) {
            log.warn("Data already present ({} stores). Demo seeder refusing to run; "
                    + "truncate the tables first.", storeRepository.count());
            return;
        }

        String hash = passwordEncoder.encode(demoPassword);

        List<Product> products = seedProducts();
        List<String> logins = new ArrayList<>();

        for (int s = 0; s < STORES; s++) {
            Store store = storeRepository.save(new Store(STORE_DEFS[s][0], STORE_DEFS[s][1]));

            // Manager: an employee who also holds a store_manager row, which is what
            // grants ROLE_STORE_MANAGER at login.
            Employee manager = employeeRepository.save(new Employee(
                    MANAGERS[s][0], MANAGERS[s][1],
                    LocalDate.of(1985, 4, 16 + s), "%d Manager Way".formatted(10 + s),
                    "Store Manager", store));
            manager.setCredentials(MANAGERS[s][2], hash);
            storeManagerRepository.save(new StoreManager(store, manager));
            addEmergencyContact(manager, s);
            logins.add("%s  (manager, %s)".formatted(MANAGERS[s][2], store.getStoreName()));

            List<Employee> staff = new ArrayList<>();
            for (int i = 0; i < STAFF_PER_STORE; i++) {
                String[] name = STAFF[s * STAFF_PER_STORE + i];
                Employee employee = employeeRepository.save(new Employee(
                        name[0], name[1],
                        LocalDate.of(1995 + i, 1 + i, 5 + i),
                        "%d Staff Rd".formatted(20 + i),
                        ROLES[i % ROLES.length], store));

                // Two of the four get logins; the rest show the "no access" state.
                if (i < 2) {
                    String username = "%s.%s".formatted(
                            name[0].toLowerCase(), name[1].toLowerCase());
                    employee.setCredentials(username, hash);
                    logins.add("%s  (employee, %s)".formatted(username, store.getStoreName()));
                }
                addEmergencyContact(employee, i);
                staff.add(employee);
            }

            List<InventoryItem> stock = seedInventory(store, products);
            seedSales(store, staff, stock);
        }

        log.info("""

                        ================ DEMO DATA READY ================
                        Password for every account below: {}
                        {}
                        =================================================""",
                demoPassword, String.join("\n", logins));
    }

    private List<Product> seedProducts() {
        List<Product> products = new ArrayList<>(PRODUCTS);
        for (String[] def : PRODUCT_DEFS) {
            products.add(productRepository.save(new Product(def[0], def[1], def[2])));
        }
        return products;
    }

    private void addEmergencyContact(Employee employee, int seed) {
        emergencyInfoRepository.save(new EmergencyInfo(
                "%s %s".formatted(STAFF[(seed + 5) % STAFF.length][0],
                        employee.getEmpLastName()),
                "+1-416-%03d-%04d".formatted(200 + random.nextInt(700), random.nextInt(10000)),
                RELATIONSHIPS[seed % RELATIONSHIPS.length],
                employee));
    }

    private List<InventoryItem> seedInventory(Store store, List<Product> products) {
        List<InventoryItem> stock = new ArrayList<>();
        for (int p = 0; p < products.size(); p++) {
            BigDecimal listPrice = new BigDecimal(PRODUCT_DEFS[p][3]);
            // Slight per-store price variation.
            BigDecimal price = listPrice
                    .multiply(BigDecimal.valueOf(0.97 + random.nextInt(7) * 0.01))
                    .setScale(2, RoundingMode.HALF_UP);

            // A couple of items are deliberately low or out of stock so the
            // low-stock panel and the out-of-stock till state are visible.
            int quantity = switch (p) {
                case 1 -> 8;
                case 22 -> 0;
                default -> 40 + random.nextInt(260);
            };

            stock.add(inventoryItemRepository.save(
                    new InventoryItem(store, products.get(p), products.get(p).getProductName(),
                            price, quantity)));
        }
        return stock;
    }

    private void seedSales(Store store, List<Employee> staff, List<InventoryItem> stock) {
        for (int day = SALES_DAYS - 1; day >= 0; day--) {
            LocalDate date = LocalDate.now().minusDays(day);
            int transactions = 6 + random.nextInt(10);

            for (int t = 0; t < transactions; t++) {
                Employee seller = staff.get(random.nextInt(staff.size()));
                InventoryItem item = stock.get(random.nextInt(stock.size()));
                if (item.getQuantity() <= 0) {
                    continue;
                }

                int quantity = 1 + random.nextInt(Math.min(4, item.getQuantity()));
                BigDecimal total = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP);

                LocalDateTime madeAt = date.atTime(9 + random.nextInt(11), random.nextInt(60));

                item.setQuantity(item.getQuantity() - quantity);
                // soldBy is what makes the per-employee daily reports meaningful.
                salesRepository.save(new Sales(
                        item.getProduct(), store, seller, quantity, total, madeAt));
            }
        }
    }
}
