package com.example.devsecops.service;

import com.example.devsecops.dto.EmployeeRequest;
import com.example.devsecops.dto.EmployeeResponse;
import com.example.devsecops.entity.Employee;
import com.example.devsecops.exception.DuplicateEmailException;
import com.example.devsecops.exception.EmployeeNotFoundException;
import com.example.devsecops.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    private EmployeeRequest request() {
        return new EmployeeRequest("Alice", "alice@example.com", "IT", new BigDecimal("5000.00"));
    }

    private Employee employee(Long id) {
        return new Employee(id, "Alice", "alice@example.com", "IT", new BigDecimal("5000.00"));
    }

    @Test
    void createSavesEmployee() {
        when(repository.existsByEmail("alice@example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenReturn(employee(1L));

        EmployeeResponse response = service.create(request());

        assertEquals(1L, response.getId());
        assertEquals("alice@example.com", response.getEmail());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(repository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.create(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void findAllReturnsResponses() {
        when(repository.findAll()).thenReturn(List.of(employee(1L), employee(2L)));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void updateChangesFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee(1L)));
        when(repository.existsByEmailAndIdNot("alice@example.com", 1L)).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRequest req = new EmployeeRequest("Alice B", "alice@example.com", "HR", new BigDecimal("6000"));
        EmployeeResponse response = service.update(1L, req);

        assertEquals("Alice B", response.getName());
        assertEquals("HR", response.getDepartment());
    }

    @Test
    void updateRejectsEmailUsedByAnotherEmployee() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee(1L)));
        when(repository.existsByEmailAndIdNot("alice@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.update(1L, request()));
    }

    @Test
    void deleteRemovesEmployee() {
        Employee e = employee(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        service.delete(1L);

        verify(repository).delete(e);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.delete(5L));
    }
}
