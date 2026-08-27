package com.salias.storems.service;

import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SalesServiceTest {

    @Autowired
    private SalesService salesService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        store = storeRepository.save(new Store("Test Store", "1 Test Way"));
        product = productRepository.save(new Product("Test Widget", "ACME", "A widget for tests"));
        inventoryItemRepository.save(
                new InventoryItem(store, product, "Widget", new BigDecimal("2.50"), 10));
    }

    @Test
    void recordSaleDeductsStockAndPricesAtUnitPrice() {
        Sales sale = salesService.recordSale(store.getStoreId(), product.getProductId(), 4);
        entityManager.flush();

        assertThat(sale.getSaleNumber()).isNotNull();
        assertThat(sale.getQuantityBought()).isEqualTo(4);
        assertThat(sale.getPrice()).isEqualByComparingTo("10.00");
        // @CreationTimestamp fills this in on insert; the old code declared
        // made-at/supplied-at NOT NULL but never assigned them.
        assertThat(sale.getMadeAt()).isNotNull();

        InventoryItem remaining =
                inventoryService.findItem(store.getStoreId(), product.getProductId());
        assertThat(remaining.getQuantity()).isEqualTo(6);
    }

    @Test
    void recordSaleRejectsQuantityAboveStockAndLeavesStockUntouched() {
        assertThatThrownBy(() ->
                salesService.recordSale(store.getStoreId(), product.getProductId(), 11))
                .isInstanceOf(InsufficientStockException.class);

        InventoryItem unchanged =
                inventoryService.findItem(store.getStoreId(), product.getProductId());
        assertThat(unchanged.getQuantity()).isEqualTo(10);
    }

    @Test
    void recordSaleRejectsNonPositiveQuantity() {
        assertThatThrownBy(() ->
                salesService.recordSale(store.getStoreId(), product.getProductId(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findItemThrowsWhenStoreDoesNotStockProduct() {
        Product unstocked = productRepository.save(
                new Product("Unstocked", "ACME", "Not on any shelf"));

        assertThatThrownBy(() ->
                inventoryService.findItem(store.getStoreId(), unstocked.getProductId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inventoryItemIsRetrievableByItsCompositeKey() {
        // Composite-key lookup only works because InventoryItemId implements
        // equals/hashCode, which the original class did not.
        InventoryItem found =
                inventoryService.findItem(store.getStoreId(), product.getProductId());

        assertThat(found.getStore().getStoreId()).isEqualTo(store.getStoreId());
        assertThat(found.getProduct().getProductId()).isEqualTo(product.getProductId());
        assertThat(found.getInStoreProductName()).isEqualTo("Widget");
    }
}
