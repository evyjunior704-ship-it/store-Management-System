package com.salias.storems.service;

import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.sales.SalesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Recording sales and reading them back.
 */
@Service
@Transactional(readOnly = true)
public class SalesService {

    private final SalesRepository salesRepository;
    private final InventoryService inventoryService;

    public SalesService(SalesRepository salesRepository, InventoryService inventoryService) {
        this.salesRepository = salesRepository;
        this.inventoryService = inventoryService;
    }

    public List<Sales> findByStore(Long storeId) {
        return salesRepository.findByStore_StoreId(storeId);
    }

    public List<Sales> findByStoreBetween(Long storeId, LocalDateTime from, LocalDateTime to) {
        return salesRepository.findByStore_StoreIdAndMadeAtBetween(storeId, from, to);
    }

    /**
     * Sells {@code quantity} units of a product at a store: deducts the stock and
     * writes a sale priced at the item's current unit price. Both happen in one
     * transaction, so a stock shortfall records no sale.
     *
     * @throws InsufficientStockException if the store does not hold enough units
     */
    @Transactional
    public Sales recordSale(Long storeId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got " + quantity);
        }

        InventoryItem item = inventoryService.deductStock(storeId, productId, quantity);
        BigDecimal total = item.getUnitPrice().multiply(BigDecimal.valueOf(quantity));

        return salesRepository.save(new Sales(item.getProduct(), item.getStore(), quantity, total));
    }
}
