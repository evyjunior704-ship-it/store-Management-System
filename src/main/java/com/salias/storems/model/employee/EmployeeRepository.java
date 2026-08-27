package com.salias.storems.model.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByStoreAssociatedTo_StoreId(Long storeId);
}
