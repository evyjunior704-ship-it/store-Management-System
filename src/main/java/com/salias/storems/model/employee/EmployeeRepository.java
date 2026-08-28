package com.salias.storems.model.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByStoreAssociatedTo_StoreId(Long storeId);

    /**
     * Loads an employee for authentication. The store is fetch-joined because the
     * principal needs the store id, and the association is lazy.
     */
    @Query("""
            SELECT e FROM Employee e
            JOIN FETCH e.storeAssociatedTo
            WHERE e.username = :username
            """)
    Optional<Employee> findByUsernameWithStore(String username);

    boolean existsByUsername(String username);
}
