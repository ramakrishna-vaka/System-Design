package com.example.locking.service;

import com.example.locking.model.Employee;
import com.example.locking.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Employee createEmployee(String name, BigDecimal salary) {
        Employee employee = new Employee(name, salary);
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Employee not found: " + id));
    }

    // Optimistic locking
    @Transactional
    public Employee incrementSalaryOptimistically(
            Long id, BigDecimal amount) {


        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Employee not found: " + id));

        pauseForConcurrencyTest(); // Temporary test delay

        employee.setSalary(employee.getSalary().add(amount));

        return employeeRepository.saveAndFlush(employee);
    }

    // Pessimistic locking
    @Transactional
    public Employee incrementSalaryPessimistically(
            Long id, BigDecimal amount) {

        Employee employee = employeeRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Employee not found: " + id));

        pauseForConcurrencyTest(); // Temporary test delay

        employee.setSalary(employee.getSalary().add(amount));

        return employeeRepository.saveAndFlush(employee);
    }

    private void pauseForConcurrencyTest() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Concurrency test interrupted", e);
        }
    }

}
