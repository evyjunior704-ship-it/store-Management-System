package com.salias.storems.service;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.sales.SalesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The till.
 *
 * <p>Prices are never accepted from the client: each line is priced from
 * {@code inventory_item.unit_price} inside the transaction, so a cashier cannot
 * set their own price. Stock is re-checked at the same moment, so two tills
 * selling the last unit cannot both succeed.
 */
@Service
public class PosService {

    /** One basket line as submitted by the till. */
    public record BasketLine(Long productId, int quantity) {
    }

    /** What a completed checkout produced, for the confirmation screen. */
    public record Receipt(List<Sales> sales, BigDecimal total, int lineCount) {
    }

    private final InventoryItemRepository inventoryItemRepository;
    private final SalesRepository salesRepository;
    private final EmployeeRepository employeeRepository;

    public PosService(InventoryItemRepository inventoryItemRepository,
                      SalesRepository salesRepository,
                      EmployeeRepository employeeRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.salesRepository = salesRepository;
        this.employeeRepository = employeeRepository;
    }

    /** Active, in-catalogue items at this store matching a search term. */
    @Transactional(readOnly = true)
    public List<InventoryItem> search(Long storeId, String term) {
        String needle = (term == null) ? "" : term.trim();
        return inventoryItemRepository.searchForTill(storeId, needle);
    }

    /**
     * Commits a basket: deducts stock and writes one {@code Sales} row per line,
     * all in one transaction. If any line is short of stock, nothing is recorded.
     *
     * @throws InsufficientStockException if a line exceeds available stock
     */
    @Transactional
    public Receipt checkout(Long storeId, Long employeeId, List<BasketLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot complete a sale with an empty basket");
        }

        Employee soldBy = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + employeeId));

        List<Sales> recorded = new ArrayList<>(lines.size());
        BigDecimal total = BigDecimal.ZERO;

        for (BasketLine line : lines) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive for product " + line.productId());
            }

            InventoryItem item = inventoryItemRepository
                    .findOneWithRefs(storeId, line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "This store does not stock product " + line.productId()));

            if (!item.getProduct().isActive()) {
                throw new IllegalArgumentException(
                        "%s has been discontinued and cannot be sold"
                                .formatted(item.getProduct().getProductName()));
            }

            if (item.getQuantity() < line.quantity()) {
                throw new InsufficientStockException(
                        "%s: only %d in stock, cannot sell %d"
                                .formatted(item.getProduct().getProductName(),
                                        item.getQuantity(), line.quantity()));
            }

            item.setQuantity(item.getQuantity() - line.quantity());

            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.quantity()));
            total = total.add(lineTotal);

            recorded.add(salesRepository.save(new Sales(
                    item.getProduct(), item.getStore(), soldBy, line.quantity(), lineTotal)));
        }

        return new Receipt(recorded, total, recorded.size());
    }
}
