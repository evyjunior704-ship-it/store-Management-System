package com.salias.storems.controller;

import com.salias.storems.security.Roles;
import com.salias.storems.security.StoremsPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Puts the current user and their role flags into every model.
 *
 * <p>Templates use these to hide navigation the user cannot reach. That is
 * cosmetic only — the real gate is the URL rules in {@code SecurityConfig} plus
 * store-scoping in each handler. Hiding a link is not authorisation.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUser")
    StoremsPrincipal currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StoremsPrincipal p) {
            return p;
        }
        return null;
    }

    @ModelAttribute("isManager")
    boolean isManager(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> Roles.ROLE_STORE_MANAGER.equals(a.getAuthority()));
    }
}
