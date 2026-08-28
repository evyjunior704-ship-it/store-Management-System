package com.salias.storems.service;

import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.sales.SalesRepository;
import com.salias.storems.report.SalesTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reading sales back, plus the single-line "sell N units" operation.
 *
 * <p>Multi-line till checkouts go through {@link PosService}, which records the
 * operator; this class covers programmatic single-product sales.
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

    /** Every sale at a store on one day, newest first. */
    public List<Sales> findStoreSalesOn(Long storeId, LocalDate date) {
        return salesRepository.findStoreSales(storeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /** One employee's sales at a store on one day. */
    public List<Sales> findEmployeeSalesOn(Long storeId, Long employeeId, LocalDate date) {
        return salesRepository.findEmployeeSales(
                storeId, employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public List<Sales> findStoreSalesBetween(Long storeId, LocalDateTime from, LocalDateTime to) {
        return salesRepository.findStoreSales(storeId, from, to);
    }

    public SalesTotals totalsForStoreOn(Long storeId, LocalDate date) {
        SalesTotals totals = salesRepository.totalsForStore(
                storeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        return totals == null ? SalesTotals.empty() : totals;
    }

    public SalesTotals totalsForEmployeeOn(Long storeId, Long employeeId, LocalDate date) {
        SalesTotals totals = salesRepository.totalsForEmployee(
                storeId, employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        return totals == null ? SalesTotals.empty() : totals;
    }

    /**
     * Sells {@code quantity} units of one product: deducts stock and writes a sale
     * priced at the item's current unit price, in one transaction.
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
