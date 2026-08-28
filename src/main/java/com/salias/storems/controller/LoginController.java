package com.salias.storems.controller;

import com.salias.storems.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    String login() {
        return "login";
    }

    /** Sends whoever lands on "/" to the right home screen for their role. */
    @GetMapping("/")
    String home(Authentication authentication) {
        boolean isManager = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> Roles.ROLE_STORE_MANAGER.equals(a.getAuthority()));
        return isManager ? "redirect:/dashboard" : "redirect:/pos";
    }
}
