package com.example.devsecops.controller;

import com.example.devsecops.dto.EmployeeRequest;
import com.example.devsecops.dto.EmployeeResponse;
import com.example.devsecops.exception.DuplicateEmailException;
import com.example.devsecops.exception.EmployeeNotFoundException;
import com.example.devsecops.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Serves the Thymeleaf pages. */
@Controller
@RequiredArgsConstructor
public class EmployeeWebController {

    private final EmployeeService service;

    @GetMapping("/")
    public String home() {
        return "redirect:/employees";
    }

    @GetMapping("/employees")
    public String list(Model model) {
        model.addAttribute("employees", service.findAll());
        return "employees";
    }

    @GetMapping("/employees/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new EmployeeRequest());
        model.addAttribute("editing", false);
        return "employee-form";
    }

    @PostMapping("/employees")
    public String create(@Valid @ModelAttribute("employee") EmployeeRequest request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.create(request);
                redirect.addFlashAttribute("success", "Employee created successfully.");
                return "redirect:/employees";
            } catch (DuplicateEmailException ex) {
                result.rejectValue("email", "duplicate", "Email already exists");
            }
        }
        model.addAttribute("editing", false);
        return "employee-form";
    }

    @GetMapping("/employees/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            EmployeeResponse e = service.findById(id);
            model.addAttribute("employee",
                    new EmployeeRequest(e.getName(), e.getEmail(), e.getDepartment(), e.getSalary()));
            model.addAttribute("employeeId", id);
            model.addAttribute("editing", true);
            return "employee-form";
        } catch (EmployeeNotFoundException ex) {
            redirect.addFlashAttribute("error", "Employee not found.");
            return "redirect:/employees";
        }
    }

    @PostMapping("/employees/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("employee") EmployeeRequest request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.update(id, request);
                redirect.addFlashAttribute("success", "Employee updated successfully.");
                return "redirect:/employees";
            } catch (DuplicateEmailException ex) {
                result.rejectValue("email", "duplicate", "Email already exists");
            } catch (EmployeeNotFoundException ex) {
                redirect.addFlashAttribute("error", "Employee not found.");
                return "redirect:/employees";
            }
        }
        model.addAttribute("employeeId", id);
        model.addAttribute("editing", true);
        return "employee-form";
    }

    // GET is used because the spec asks for /employees/delete/{id} as a link.
    @GetMapping("/employees/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.delete(id);
            redirect.addFlashAttribute("success", "Employee deleted successfully.");
        } catch (EmployeeNotFoundException ex) {
            redirect.addFlashAttribute("error", "Employee not found.");
        }
        return "redirect:/employees";
    }
}
