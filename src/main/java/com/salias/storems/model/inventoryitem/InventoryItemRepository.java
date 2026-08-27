package com.salias.storems.model.inventoryitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {

    List<InventoryItem> findByStore_StoreId(Long storeId);

    List<InventoryItem> findByQuantityLessThan(Integer threshold);
}
