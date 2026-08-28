package com.salias.storems.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * The logged-in user, backed by an {@code Employee} row.
 *
 * <p>Carries the employee and store ids so request handling can scope every query
 * to the user's own store without another lookup.
 */
public class StoremsPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Long employeeId;
    private final Long storeId;
    private final String storeName;
    private final String displayName;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public StoremsPrincipal(Long employeeId,
                            Long storeId,
                            String storeName,
                            String displayName,
                            String username,
                            String passwordHash,
                            boolean enabled,
                            List<GrantedAuthority> authorities) {
        this.employeeId = employeeId;
        this.storeId = storeId;
        this.storeName = storeName;
        this.displayName = displayName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = List.copyOf(authorities);
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isStoreManager() {
        return authorities.stream()
                .anyMatch(a -> Roles.ROLE_STORE_MANAGER.equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
