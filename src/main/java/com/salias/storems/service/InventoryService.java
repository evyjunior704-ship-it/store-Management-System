package com.salias.storems.service;

import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemId;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.itemsupplier.ItemSupplier;
import com.salias.storems.model.itemsupplier.ItemSupplierId;
import com.salias.storems.model.itemsupplier.ItemSupplierRepository;
import com.salias.storems.model.supplies.Supplies;
import com.salias.storems.model.supplies.SuppliesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Per-store stock levels and incoming deliveries.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    /** Units per box, used when converting a delivery into stock on hand. */
    private static final int UNITS_PER_BOX = 1;

    private final InventoryItemRepository inventoryItemRepository;
    private final ItemSupplierRepository itemSupplierRepository;
    private final SuppliesRepository suppliesRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            ItemSupplierRepository itemSupplierRepository,
                            SuppliesRepository suppliesRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.itemSupplierRepository = itemSupplierRepository;
        this.suppliesRepository = suppliesRepository;
    }

    public List<InventoryItem> findStock(Long storeId) {
        return inventoryItemRepository.findByStore_StoreId(storeId);
    }

    public List<InventoryItem> findLowStock(int threshold) {
        return inventoryItemRepository.findByQuantityLessThan(threshold);
    }

    public InventoryItem findItem(Long storeId, Long productId) {
        return inventoryItemRepository.findById(new InventoryItemId(storeId, productId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store %d does not stock product %d".formatted(storeId, productId)));
    }

    /**
     * Records a delivery and adds the received units to the store's stock.
     */
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
     * Removes {@code quantity} units from the store's stock of a product.
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
