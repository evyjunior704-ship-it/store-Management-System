package com.salias.storems.security;

import com.salias.storems.model.employee.Employee;
import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import com.salias.storems.model.storemanager.StoreManager;
import com.salias.storems.model.storemanager.StoreManagerRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates against {@code Employee} rows.
 *
 * <p>Roles are derived from data already in the schema rather than a role table:
 * an employee whose id appears in {@code store_manager} is a manager, everyone
 * else is ordinary staff.
 */
@Service
public class StoremsUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final StoreManagerRepository storeManagerRepository;
    private final StoreRepository storeRepository;

    public StoremsUserDetailsService(EmployeeRepository employeeRepository,
                                     StoreManagerRepository storeManagerRepository,
                                     StoreRepository storeRepository) {
        this.employeeRepository = employeeRepository;
        this.storeManagerRepository = storeManagerRepository;
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsernameWithStore(username)
                // Deliberately vague: do not reveal whether the username exists.
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        Store employedAt = employee.getStoreAssociatedTo();
        Optional<StoreManager> managed = storeManagerRepository.findByManager_Id(employee.getId());

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(Roles.ROLE_EMPLOYEE));
        managed.ifPresent(m -> authorities.add(new SimpleGrantedAuthority(Roles.ROLE_STORE_MANAGER)));

        // A manager is scoped to the store they manage. Normally that is also where
        // they are employed; if the data disagrees, the managed store wins.
        Store scopedStore = managed
                .map(StoreManager::getStoreId)
                .filter(managedStoreId -> !managedStoreId.equals(employedAt.getStoreId()))
                .flatMap(storeRepository::findById)
                .orElse(employedAt);

        return new StoremsPrincipal(
                employee.getId(),
                scopedStore.getStoreId(),
                scopedStore.getStoreName(),
                "%s %s".formatted(employee.getEmpFirstName(), employee.getEmpLastName()),
                employee.getUsername(),
                employee.getPasswordHash(),
                employee.isEnabled(),
                authorities);
    }
}
