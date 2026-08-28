package com.salias.storems.model.sales;

import com.salias.storems.report.ProductSalesLine;
import com.salias.storems.report.SalesTotals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesRepository extends JpaRepository<Sales, Long> {

    /** Store sales in a window, newest first, for the manager's sales screen. */
    @Query("""
            SELECT s FROM Sales s
            JOIN FETCH s.product
            LEFT JOIN FETCH s.soldBy
            WHERE s.store.storeId = :storeId
              AND s.madeAt >= :from AND s.madeAt < :to
            ORDER BY s.madeAt DESC
            """)
    List<Sales> findStoreSales(Long storeId, LocalDateTime from, LocalDateTime to);

    /** Same window but filtered to one employee. */
    @Query("""
            SELECT s FROM Sales s
            JOIN FETCH s.product
            LEFT JOIN FETCH s.soldBy
            WHERE s.store.storeId = :storeId
              AND s.soldBy.id = :employeeId
              AND s.madeAt >= :from AND s.madeAt < :to
            ORDER BY s.madeAt DESC
            """)
    List<Sales> findEmployeeSales(Long storeId, Long employeeId, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT new com.salias.storems.report.SalesTotals(
                       COUNT(s), COALESCE(SUM(s.quantityBought), 0), COALESCE(SUM(s.price), 0))
            FROM Sales s
            WHERE s.store.storeId = :storeId
              AND s.madeAt >= :from AND s.madeAt < :to
            """)
    SalesTotals totalsForStore(Long storeId, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT new com.salias.storems.report.SalesTotals(
                       COUNT(s), COALESCE(SUM(s.quantityBought), 0), COALESCE(SUM(s.price), 0))
            FROM Sales s
            WHERE s.store.storeId = :storeId
              AND s.soldBy.id = :employeeId
              AND s.madeAt >= :from AND s.madeAt < :to
            """)
    SalesTotals totalsForEmployee(Long storeId, Long employeeId, LocalDateTime from, LocalDateTime to);

    /** Per-product breakdown used in the daily report body. */
    @Query("""
            SELECT new com.salias.storems.report.ProductSalesLine(
                       p.productId, p.productName,
                       COALESCE(SUM(s.quantityBought), 0), COALESCE(SUM(s.price), 0))
            FROM Sales s JOIN s.product p
            WHERE s.store.storeId = :storeId
              AND s.soldBy.id = :employeeId
              AND s.madeAt >= :from AND s.madeAt < :to
            GROUP BY p.productId, p.productName
            ORDER BY SUM(s.price) DESC
            """)
    List<ProductSalesLine> breakdownForEmployee(Long storeId, Long employeeId,
                                                LocalDateTime from, LocalDateTime to);

    /** Per-product breakdown for a whole store, for the store-wide day report. */
    @Query("""
            SELECT new com.salias.storems.report.ProductSalesLine(
                       p.productId, p.productName,
                       COALESCE(SUM(s.quantityBought), 0), COALESCE(SUM(s.price), 0))
            FROM Sales s JOIN s.product p
            WHERE s.store.storeId = :storeId
              AND s.madeAt >= :from AND s.madeAt < :to
            GROUP BY p.productId, p.productName
            ORDER BY SUM(s.price) DESC
            """)
    List<ProductSalesLine> breakdownForStore(Long storeId, LocalDateTime from, LocalDateTime to);

    /** Employees who rang up at least one sale in the window. */
    @Query("""
            SELECT DISTINCT s.soldBy.id FROM Sales s
            WHERE s.store.storeId = :storeId
              AND s.soldBy IS NOT NULL
              AND s.madeAt >= :from AND s.madeAt < :to
            """)
    List<Long> findSellingEmployeeIds(Long storeId, LocalDateTime from, LocalDateTime to);

    Page<Sales> findByStore_StoreIdOrderByMadeAtDesc(Long storeId, Pageable pageable);

    boolean existsByProduct_ProductId(Long productId);
}
