package com.salias.storems.model.supplies;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuppliesRepository extends JpaRepository<Supplies, Long> {

    List<Supplies> findBySupplier_Id_StoreId(Long storeId);
}
