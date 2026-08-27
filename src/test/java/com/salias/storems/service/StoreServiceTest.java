package com.salias.storems.service;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.storemanager.StoreManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StoreServiceTest {

    @Autowired
    private StoreService storeService;
    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee hire(Store store, String firstName, String role) {
        return employeeRepository.save(new Employee(
                firstName, "Tester", LocalDate.of(1990, 5, 1), "1 Any Rd", role, store));
    }

    @Test
    void assignManagerLinksEmployeeToTheirOwnStore() {
        Store store = storeService.create("Store A", "10 First St");
        Employee employee = hire(store, "Ada", "Store Manager");

        StoreManager assignment = storeService.assignManager(store.getStoreId(), employee.getId());

        assertThat(assignment.getStoreId()).isEqualTo(store.getStoreId());
        assertThat(assignment.getManager().getId()).isEqualTo(employee.getId());
    }

    @Test
    void assignManagerReplacesTheExistingManagerRatherThanFailing() {
        Store store = storeService.create("Store B", "20 Second St");
        Employee first = hire(store, "Ada", "Store Manager");
        Employee second = hire(store, "Grace", "Store Manager");

        storeService.assignManager(store.getStoreId(), first.getId());
        StoreManager updated = storeService.assignManager(store.getStoreId(), second.getId());

        assertThat(updated.getStoreId()).isEqualTo(store.getStoreId());
        assertThat(updated.getManager().getId()).isEqualTo(second.getId());
    }

    @Test
    void assignManagerRejectsEmployeeFromAnotherStore() {
        Store storeC = storeService.create("Store C", "30 Third St");
        Store storeD = storeService.create("Store D", "40 Fourth St");
        Employee outsider = hire(storeD, "Alan", "Cashier");

        assertThatThrownBy(() -> storeService.assignManager(storeC.getStoreId(), outsider.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oneStoreHoldsManyEmployees() {
        // The original mapping was @OneToOne, which allowed only one employee per store.
        Store store = storeService.create("Store E", "50 Fifth St");
        hire(store, "Ada", "Cashier");
        hire(store, "Grace", "Store Associate");
        hire(store, "Alan", "Cashier");

        assertThat(storeService.findStaff(store.getStoreId())).hasSize(3);
    }

    @Test
    void findByIdThrowsForUnknownStore() {
        assertThatThrownBy(() -> storeService.findById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
