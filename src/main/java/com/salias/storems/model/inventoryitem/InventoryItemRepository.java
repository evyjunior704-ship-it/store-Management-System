package com.salias.storems.model.inventoryitem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {

    List<InventoryItem> findByStore_StoreId(Long storeId);

    List<InventoryItem> findByQuantityLessThan(Integer threshold);

    /** Store stock with product eagerly loaded, for list screens. */
    @Query("""
            SELECT i FROM InventoryItem i
            JOIN FETCH i.product p
            WHERE i.store.storeId = :storeId
            ORDER BY p.productName
            """)
    List<InventoryItem> findStockWithProducts(Long storeId);

    /** Items at or below the given threshold, for the low-stock panel. */
    @Query("""
            SELECT i FROM InventoryItem i
            JOIN FETCH i.product p
            WHERE i.store.storeId = :storeId AND i.quantity <= :threshold
            ORDER BY i.quantity ASC
            """)
    List<InventoryItem> findLowStock(Long storeId, int threshold);

    /** Active, in-stock items matching a search term, for the till. */
    @Query("""
            SELECT i FROM InventoryItem i
            JOIN FETCH i.product p
            WHERE i.store.storeId = :storeId
              AND p.active = true
              AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY p.productName
            """)
    List<InventoryItem> searchForTill(Long storeId, String term);

    @Query("""
            SELECT i FROM InventoryItem i
            JOIN FETCH i.product
            JOIN FETCH i.store
            WHERE i.store.storeId = :storeId AND i.product.productId = :productId
            """)
    Optional<InventoryItem> findOneWithRefs(Long storeId, Long productId);

    boolean existsByStore_StoreIdAndProduct_ProductId(Long storeId, Long productId);

    boolean existsByProduct_ProductId(Long productId);

    long countByStore_StoreIdAndQuantityLessThanEqual(Long storeId, int threshold);
}
