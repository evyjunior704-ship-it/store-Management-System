package com.salias.storems.service;

import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemId;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.itemsupplier.ItemSupplier;
import com.salias.storems.model.itemsupplier.ItemSupplierId;
import com.salias.storems.model.itemsupplier.ItemSupplierRepository;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.model.supplies.Supplies;
import com.salias.storems.model.supplies.SuppliesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-store stock levels, incoming deliveries, and the manager's stock controls.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    /** Items at or below this quantity show in the low-stock panel. */
    public static final int LOW_STOCK_THRESHOLD = 20;

    /** Units per box, used when converting a delivery into stock on hand. */
    private static final int UNITS_PER_BOX = 1;

    private final InventoryItemRepository inventoryItemRepository;
    private final ItemSupplierRepository itemSupplierRepository;
    private final SuppliesRepository suppliesRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            ItemSupplierRepository itemSupplierRepository,
                            SuppliesRepository suppliesRepository,
                            ProductRepository productRepository,
                            StoreRepository storeRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.itemSupplierRepository = itemSupplierRepository;
        this.suppliesRepository = suppliesRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    // ---------- reads ----------

    public List<InventoryItem> findStock(Long storeId) {
        return inventoryItemRepository.findStockWithProducts(storeId);
    }

    public List<InventoryItem> findLowStock(Long storeId) {
        return inventoryItemRepository.findLowStock(storeId, LOW_STOCK_THRESHOLD);
    }

    public long countLowStock(Long storeId) {
        return inventoryItemRepository
                .countByStore_StoreIdAndQuantityLessThanEqual(storeId, LOW_STOCK_THRESHOLD);
    }

    public InventoryItem findItem(Long storeId, Long productId) {
        return inventoryItemRepository.findById(new InventoryItemId(storeId, productId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store %d does not stock product %d".formatted(storeId, productId)));
    }

    // ---------- manager stock controls ----------

    /** Sets stock to an absolute figure, e.g. after a physical count. */
    @Transactional
    public InventoryItem setQuantity(Long storeId, Long productId, int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative, got " + newQuantity);
        }
        InventoryItem item = findItem(storeId, productId);
        item.setQuantity(newQuantity);
        return item;
    }

    /** Adds to (or with a negative delta, removes from) stock on hand. */
    @Transactional
    public InventoryItem adjustQuantity(Long storeId, Long productId, int delta) {
        InventoryItem item = findItem(storeId, productId);
        int updated = item.getQuantity() + delta;
        if (updated < 0) {
            throw new IllegalArgumentException(
                    "Adjustment of %d would take stock below zero (currently %d)"
                            .formatted(delta, item.getQuantity()));
        }
        item.setQuantity(updated);
        return item;
    }

    @Transactional
    public InventoryItem setUnitPrice(Long storeId, Long productId, BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price must be zero or more");
        }
        InventoryItem item = findItem(storeId, productId);
        item.setUnitPrice(unitPrice);
        return item;
    }

    /** Puts a catalogue product on this store's shelves. */
    @Transactional
    public InventoryItem addProductToStore(Long storeId,
                                           Long productId,
                                           String inStoreName,
                                           BigDecimal unitPrice,
                                           int quantity) {
        if (inventoryItemRepository.existsByStore_StoreIdAndProduct_ProductId(storeId, productId)) {
            throw new IllegalArgumentException("This store already stocks product " + productId);
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("No store with id " + storeId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No product with id " + productId));

        String displayName = (inStoreName == null || inStoreName.isBlank())
                ? product.getProductName()
                : inStoreName.trim();

        return inventoryItemRepository.save(
                new InventoryItem(store, product, displayName, unitPrice, quantity));
    }

    /** Takes a product off this store's shelves. Sales history is unaffected. */
    @Transactional
    public void removeProductFromStore(Long storeId, Long productId) {
        InventoryItem item = findItem(storeId, productId);
        inventoryItemRepository.delete(item);
    }

    // ---------- deliveries ----------

    @Transactional
    public Supplies receiveDelivery(ItemSupplierId supplierId, int boxes, String deliveryNote) {
        ItemSupplier supplier = itemSupplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("No supply arrangement " + supplierId));

        Supplies delivery = suppliesRepository.save(new Supplies(supplier, boxes, deliveryNote));

        InventoryItem item = findItem(supplierId.getStoreId(), supplierId.getProductId());
        item.setQuantity(item.getQuantity() + (boxes * UNITS_PER_BOX));

        return delivery;
    }

    /**
     * Removes units from stock.
     *
     * @throws InsufficientStockException if the store holds fewer units than requested
     */
    @Transactional
    public InventoryItem deductStock(Long storeId, Long productId, int quantity) {
        InventoryItem item = findItem(storeId, productId);
        if (item.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Store %d has %d units of product %d, cannot deduct %d"
                            .formatted(storeId, item.getQuantity(), productId, quantity));
        }
        item.setQuantity(item.getQuantity() - quantity);
        return item;
    }
}
