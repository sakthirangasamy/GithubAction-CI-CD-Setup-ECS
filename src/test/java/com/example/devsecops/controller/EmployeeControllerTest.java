package com.example.devsecops.controller;

import com.example.devsecops.dto.EmployeeResponse;
import com.example.devsecops.exception.DuplicateEmailException;
import com.example.devsecops.exception.EmployeeNotFoundException;
import com.example.devsecops.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({EmployeeController.class, EmployeeWebController.class})
class EmployeeControllerTest {

    private static final String VALID_JSON =
            "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"department\":\"IT\",\"salary\":5000}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmployeeService service;

    private EmployeeResponse alice() {
        return new EmployeeResponse(1L, "Alice", "alice@example.com", "IT", new BigDecimal("5000"));
    }

    // ---------- REST API ----------

    @Test
    void apiCreateReturns201() throws Exception {
        when(service.create(any())).thenReturn(alice());

        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void apiCreateWithInvalidBodyReturns400() throws Exception {
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"department\":\"IT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void apiCreateDuplicateEmailReturns409() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateEmailException("alice@example.com"));

        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void apiListReturns200() throws Exception {
        when(service.findAll()).thenReturn(List.of(alice()));

        mvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void apiGetMissingReturns404() throws Exception {
        when(service.findById(9L)).thenThrow(new EmployeeNotFoundException(9L));

        mvc.perform(get("/api/v1/employees/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void apiUpdateReturns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(alice());

        mvc.perform(put("/api/v1/employees/1").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void apiDeleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/employees/1")).andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void apiDeleteMissingReturns404() throws Exception {
        doThrow(new EmployeeNotFoundException(9L)).when(service).delete(9L);

        mvc.perform(delete("/api/v1/employees/9")).andExpect(status().isNotFound());
    }

    // ---------- Thymeleaf UI ----------

    @Test
    void rootRedirectsToEmployees() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/employees"));
    }

    @Test
    void listPageRenders() throws Exception {
        when(service.findAll()).thenReturn(List.of(alice()));

        mvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees"))
                .andExpect(model().attributeExists("employees"));
    }

    @Test
    void newFormRenders() throws Exception {
        mvc.perform(get("/employees/new")).andExpect(status().isOk()).andExpect(view().name("employee-form"));
    }

    @Test
    void invalidFormStaysOnFormWithErrors() throws Exception {
        mvc.perform(post("/employees").param("name", "").param("email", "bad").param("department", "IT"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attributeHasFieldErrors("employee", "name", "email", "salary"));
    }

    @Test
    void validFormRedirectsToList() throws Exception {
        mvc.perform(post("/employees").param("name", "Alice").param("email", "alice@example.com")
                        .param("department", "IT").param("salary", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));
    }

    @Test
    void editMissingEmployeeRedirectsWithError() throws Exception {
        when(service.findById(9L)).thenThrow(new EmployeeNotFoundException(9L));

        mvc.perform(get("/employees/edit/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));
    }

    @Test
    void deleteRedirectsToList() throws Exception {
        mvc.perform(get("/employees/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        verify(service).delete(1L);
    }
}
