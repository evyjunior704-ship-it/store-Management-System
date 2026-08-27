package com.salias.storems.model.sales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesRepository extends JpaRepository<Sales, Long> {

    List<Sales> findByStore_StoreId(Long storeId);

    List<Sales> findByStore_StoreIdAndMadeAtBetween(Long storeId, LocalDateTime from, LocalDateTime to);
}
