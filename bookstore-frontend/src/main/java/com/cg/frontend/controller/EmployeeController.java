package com.cg.frontend.controller;

import com.cg.frontend.dto.EmployeeDto;
import com.cg.frontend.dto.JobDto;
import com.cg.frontend.dto.PublisherDtoForEmployee;
import com.cg.frontend.dto.PageMetaDto;
import com.cg.frontend.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public String listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String search,
            Model model) {

        List<EmployeeDto> employees;
        PageMetaDto pageMeta;

        if (search != null && !search.isBlank()) {
            employees = employeeService.searchEmployees(search);
            pageMeta = new PageMetaDto();
            pageMeta.setTotalElements(employees.size());
            pageMeta.setTotalPages(1);
            pageMeta.setNumber(0);
            pageMeta.setSize(employees.size());
        } else {
            employees = employeeService.getAllEmployees(page, size);
            pageMeta = employeeService.getPageMeta(page, size);
        }

        model.addAttribute("employees", employees);
        model.addAttribute("pageMeta", pageMeta);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("newEmployee", new EmployeeDto());
        model.addAttribute("pageTitle", "Employees — Swati's Module");
        return "employees/list";
    }

    @GetMapping("/{empId}")
    public String viewEmployee(@PathVariable String empId, Model model) {
        EmployeeDto employee = employeeService.getEmployeeById(empId);
        if (employee == null) {
            return "redirect:/employees";
        }
        JobDto job = null;
        if (employee.getJobId() != null) {
            job = employeeService.getJobById(employee.getJobId());
        }
        PublisherDtoForEmployee publisher = null;
        if (employee.getPubId() != null) {
            publisher = employeeService.getPublisherById(employee.getPubId());
        }
        model.addAttribute("employee", employee);
        model.addAttribute("job", job);
        model.addAttribute("publisher", publisher);
        model.addAttribute("pageTitle", "Employee Detail — " + employee.getFullName());
        return "employees/detail";
    }

    @PostMapping("/create")
    public String createEmployee(
            @RequestParam(required = false) String empId,
            @RequestParam(required = false) String fname,
            @RequestParam(required = false) String minit,
            @RequestParam(required = false) String lname,
            @RequestParam(required = false) Short jobId,
            @RequestParam(required = false) Integer jobLvl,
            @RequestParam(required = false) String pubId,
            @RequestParam(required = false) String hireDateStr,
            RedirectAttributes ra) {

        EmployeeDto employee = new EmployeeDto();
        employee.setEmpId(empId);
        employee.setFname(fname);
        employee.setMinit(minit);
        employee.setLname(lname);
        employee.setJobId(jobId);
        employee.setJobLvl(jobLvl);
        employee.setPubId(pubId);
        employee.setHireDate(parseDateTime(hireDateStr));

        String error = employeeService.createEmployee(employee);
        if (error != null) {
            ra.addFlashAttribute("toastError", error);
            return "redirect:/employees?toast=error";
        }
        return "redirect:/employees?toast=created";
    }

    @PostMapping("/update/{empId}")
    public String updateEmployee(
            @PathVariable String empId,
            @RequestParam(required = false) String fname,
            @RequestParam(required = false) String minit,
            @RequestParam(required = false) String lname,
            @RequestParam(required = false) Short jobId,
            @RequestParam(required = false) Integer jobLvl,
            @RequestParam(required = false) String pubId,
            @RequestParam(required = false) String hireDateStr,
            RedirectAttributes ra) {

        EmployeeDto employee = new EmployeeDto();
        employee.setEmpId(empId);
        employee.setFname(fname);
        employee.setMinit(minit);
        employee.setLname(lname);
        employee.setJobId(jobId);
        employee.setJobLvl(jobLvl);
        employee.setPubId(pubId);
        employee.setHireDate(parseDateTime(hireDateStr));

        String error = employeeService.updateEmployee(empId, employee);
        if (error != null) {
            ra.addFlashAttribute("toastError", error);
            return "redirect:/employees?toast=error";
        }
        return "redirect:/employees?toast=updated";
    }

    // ── Helper ──────────────────────────────────────────────────

    /**
     * Parses a datetime-local string "yyyy-MM-ddTHH:mm" or "yyyy-MM-dd" into LocalDateTime.
     * Returns null if the input is blank.
     */
    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            if (str.length() == 10) str += "T00:00:00";
            else if (str.length() == 16) str += ":00";
            return LocalDateTime.parse(str);
        } catch (Exception e) {
            System.err.println("[EmployeeController] parseDateTime failed for: " + str);
            return null;
        }
    }
}