package com.salias.storems.controller;

import com.salias.storems.security.CurrentUser;
import com.salias.storems.service.EmployeeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Staff records, emergency contacts and system access. Manager-only, and always
 * scoped to the manager's own store.
 */
@Controller
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    String list(Model model) {
        model.addAttribute("staff", employeeService.findStaff(CurrentUser.storeId()));
        return "manager/employees";
    }

    @GetMapping("/employees/{id}")
    String detail(@PathVariable Long id, Model model) {
        Long storeId = CurrentUser.storeId();
        model.addAttribute("employee", employeeService.findAtStore(storeId, id));
        model.addAttribute("contacts", employeeService.findEmergencyContacts(storeId, id));
        return "manager/employee-detail";
    }

    @PostMapping("/employees")
    String create(@RequestParam String firstName,
                  @RequestParam String lastName,
                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
                  @RequestParam String address,
                  @RequestParam String role,
                  @RequestParam(required = false) String username,
                  @RequestParam(required = false) String password,
                  RedirectAttributes flash) {
        try {
            employeeService.create(CurrentUser.storeId(), firstName, lastName, dob,
                    address, role, username, password);
            flash.addFlashAttribute("success",
                    "%s %s added to this store.".formatted(firstName, lastName));
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employees";
    }

    @PostMapping("/employees/{id}/update")
    String update(@PathVariable Long id,
                  @RequestParam String firstName,
                  @RequestParam String lastName,
                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
                  @RequestParam String address,
                  @RequestParam String role,
                  RedirectAttributes flash) {
        employeeService.update(CurrentUser.storeId(), id, firstName, lastName, dob, address, role);
        flash.addFlashAttribute("success", "Employee details updated.");
        return "redirect:/employees/" + id;
    }

    @PostMapping("/employees/{id}/access")
    String setAccess(@PathVariable Long id,
                     @RequestParam String username,
                     @RequestParam(required = false) String password,
                     RedirectAttributes flash) {
        try {
            employeeService.setCredentials(CurrentUser.storeId(), id, username, password);
            flash.addFlashAttribute("success", "System access updated.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employees/" + id;
    }

    @PostMapping("/employees/{id}/revoke-access")
    String revokeAccess(@PathVariable Long id, RedirectAttributes flash) {
        employeeService.revokeAccess(CurrentUser.storeId(), id);
        flash.addFlashAttribute("success", "System access revoked. The employee record is kept.");
        return "redirect:/employees/" + id;
    }

    // ---------- emergency contacts ----------

    @PostMapping("/employees/{id}/emergency")
    String addContact(@PathVariable Long id,
                      @RequestParam String contactName,
                      @RequestParam String phoneNumber,
                      @RequestParam String relationship,
                      RedirectAttributes flash) {
        employeeService.addEmergencyContact(
                CurrentUser.storeId(), id, contactName, phoneNumber, relationship);
        flash.addFlashAttribute("success", "Emergency contact added.");
        return "redirect:/employees/" + id;
    }

    @PostMapping("/employees/{id}/emergency/{contactId}/update")
    String updateContact(@PathVariable Long id,
                         @PathVariable Long contactId,
                         @RequestParam String contactName,
                         @RequestParam String phoneNumber,
                         @RequestParam String relationship,
                         RedirectAttributes flash) {
        employeeService.updateEmergencyContact(
                CurrentUser.storeId(), contactId, contactName, phoneNumber, relationship);
        flash.addFlashAttribute("success", "Emergency contact updated.");
        return "redirect:/employees/" + id;
    }

    @PostMapping("/employees/{id}/emergency/{contactId}/delete")
    String deleteContact(@PathVariable Long id,
                         @PathVariable Long contactId,
                         RedirectAttributes flash) {
        employeeService.deleteEmergencyContact(CurrentUser.storeId(), contactId);
        flash.addFlashAttribute("success", "Emergency contact removed.");
        return "redirect:/employees/" + id;
    }
}
