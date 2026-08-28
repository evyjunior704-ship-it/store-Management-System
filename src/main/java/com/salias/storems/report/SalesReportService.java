package com.salias.storems.report;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.sales.SalesRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.service.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds daily sales reports from the sales table.
 *
 * <p>Reports are always derived, never stored. That means an employee closing
 * their browser instead of logging out loses nothing: the figures are still
 * recoverable for any employee and any date.
 */
@Service
@Transactional(readOnly = true)
public class SalesReportService {

    private final SalesRepository salesRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;

    public SalesReportService(SalesRepository salesRepository,
                              EmployeeRepository employeeRepository,
                              StoreRepository storeRepository) {
        this.salesRepository = salesRepository;
        this.employeeRepository = employeeRepository;
        this.storeRepository = storeRepository;
    }

    /** One employee's sales for a given day at a given store. */
    public DailySalesReport forEmployee(Long storeId, Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + employeeId));

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        SalesTotals totals = orEmpty(salesRepository.totalsForEmployee(storeId, employeeId, from, to));
        List<ProductSalesLine> lines = salesRepository.breakdownForEmployee(storeId, employeeId, from, to);

        return new DailySalesReport(
                date,
                storeId,
                storeName(storeId),
                employeeId,
                "%s %s".formatted(employee.getEmpFirstName(), employee.getEmpLastName()),
                totals,
                lines,
                LocalDateTime.now());
    }

    /** Every sale at a store for a given day, regardless of who rang it. */
    public DailySalesReport forStore(Long storeId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        SalesTotals totals = orEmpty(salesRepository.totalsForStore(storeId, from, to));
        List<ProductSalesLine> lines = salesRepository.breakdownForStore(storeId, from, to);

        return new DailySalesReport(
                date, storeId, storeName(storeId),
                null, null, totals, lines, LocalDateTime.now());
    }

    /**
     * Per-employee reports for everyone who sold at the store that day. This is
     * what the manager's reports screen lists.
     */
    public List<DailySalesReport> forAllSellersAtStore(Long storeId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        return salesRepository.findSellingEmployeeIds(storeId, from, to).stream()
                .map(employeeId -> forEmployee(storeId, employeeId, date))
                .sorted((a, b) -> b.totals().totalRevenue().compareTo(a.totals().totalRevenue()))
                .toList();
    }

    private String storeName(Long storeId) {
        return storeRepository.findById(storeId)
                .map(Store::getStoreName)
                .orElseThrow(() -> new ResourceNotFoundException("No store with id " + storeId));
    }

    private SalesTotals orEmpty(SalesTotals totals) {
        return totals == null ? SalesTotals.empty() : totals;
    }
}
