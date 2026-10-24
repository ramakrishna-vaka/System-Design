package com.example.locking.controller;

import com.example.locking.model.Employee;
import com.example.locking.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public Employee createEmployee(@RequestBody CreateEmployeeRequest request) {
        return employeeService.createEmployee(
                request.name(),
                request.salary()
        );
    }

    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }

    @PostMapping("/{id}/salary/optimistic")
    public Employee incrementSalaryOptimistically(
            @PathVariable Long id,
            @RequestBody SalaryIncrementRequest request) {

        return employeeService.incrementSalaryOptimistically(
                id,
                request.amount()
        );
    }

    @PostMapping("/{id}/salary/pessimistic")
    public Employee incrementSalaryPessimistically(
            @PathVariable Long id,
            @RequestBody SalaryIncrementRequest request) {

        return employeeService.incrementSalaryPessimistically(
                id,
                request.amount()
        );
    }

    public record CreateEmployeeRequest(
            String name,
            BigDecimal salary
    ) {}

    public record SalaryIncrementRequest(
            BigDecimal amount
    ) {
    }
}