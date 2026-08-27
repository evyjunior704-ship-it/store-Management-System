package com.salias.storems.model.storemanager;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreManagerRepository extends JpaRepository<StoreManager, Long> {

    Optional<StoreManager> findByManager_Id(Long employeeId);
}
