package com.salias.storems.model.inventory_item;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemDAO extends JpaRepository<InventoryItem,InventoryItemId> {
}
