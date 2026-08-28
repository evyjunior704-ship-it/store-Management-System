package com.salias.storems.security;

/**
 * Role names used across security config, controllers and templates.
 *
 * <p>A store manager holds both roles: managers can also work a till, so
 * {@code STORE_MANAGER} is additive rather than an alternative to
 * {@code EMPLOYEE}.
 */
public final class Roles {

    public static final String STORE_MANAGER = "STORE_MANAGER";
    public static final String EMPLOYEE = "EMPLOYEE";

    public static final String ROLE_STORE_MANAGER = "ROLE_" + STORE_MANAGER;
    public static final String ROLE_EMPLOYEE = "ROLE_" + EMPLOYEE;

    private Roles() {
    }
}
