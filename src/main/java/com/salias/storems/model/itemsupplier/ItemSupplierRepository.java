package com.salias.storems.model.itemsupplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemSupplierRepository extends JpaRepository<ItemSupplier, ItemSupplierId> {

    List<ItemSupplier> findByStore_StoreId(Long storeId);

    List<ItemSupplier> findByProduct_ProductId(Long productId);

    boolean existsByProduct_ProductId(Long productId);
}
