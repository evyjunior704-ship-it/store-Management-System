package com.salias.storems.config;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loads a handful of sample rows so the app has something to work with locally.
 *
 * <p>Only active under the {@code dev} profile: run with
 * {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev}. Does nothing if
 * stores already exist.
 */
@Configuration
@Profile("dev")
public class DevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    @Bean
    ApplicationRunner seedDevData(StoreRepository storeRepository,
                                  EmployeeRepository employeeRepository,
                                  ProductRepository productRepository,
                                  InventoryItemRepository inventoryItemRepository) {
        return args -> seed(storeRepository, employeeRepository, productRepository, inventoryItemRepository);
    }

    @Transactional
    void seed(StoreRepository storeRepository,
              EmployeeRepository employeeRepository,
              ProductRepository productRepository,
              InventoryItemRepository inventoryItemRepository) {

        if (storeRepository.count() > 0) {
            log.info("Dev data already present, skipping seed");
            return;
        }

        Store storeA = storeRepository.save(new Store("Store A", "50 Berwick St"));
        Store storeB = storeRepository.save(new Store("Store B", "30 Brampton Ave"));

        employeeRepository.save(new Employee(
                "Evy Junior", "Kembou Dongmo", LocalDate.of(2007, 1, 22),
                "79 Cargo Rd", "Cashier", storeA));
        employeeRepository.save(new Employee(
                "Daniel", "McFarlane", LocalDate.of(1997, 1, 5),
                "35 Sunset Rd", "Store Associate", storeA));
        employeeRepository.save(new Employee(
                "John", "Givings", LocalDate.of(1992, 4, 16),
                "80 Fulton Rd", "Store Manager", storeB));

        Product milk = productRepository.save(
                new Product("Whole Milk 2L", "Dairyland", "Pasteurised whole milk, 2 litre carton"));
        Product bread = productRepository.save(
                new Product("Sourdough Loaf", "Baker Bros", "Stone-baked sourdough, 800g"));

        inventoryItemRepository.save(
                new InventoryItem(storeA, milk, "Whole Milk 2L", new BigDecimal("3.49"), 120));
        inventoryItemRepository.save(
                new InventoryItem(storeA, bread, "Sourdough", new BigDecimal("4.25"), 40));
        inventoryItemRepository.save(
                new InventoryItem(storeB, milk, "Milk (2L)", new BigDecimal("3.59"), 75));

        log.info("Seeded {} stores, {} employees, {} products",
                storeRepository.count(), employeeRepository.count(), productRepository.count());
    }
}
