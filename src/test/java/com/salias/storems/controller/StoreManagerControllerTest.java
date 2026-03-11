package com.salias.storems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salias.storems.dto.storemanager.CreateStoreManagerRequest;
import com.salias.storems.dto.storemanager.StoreManagerDto;
import com.salias.storems.exceptions.GlobalExceptionHandler;
import com.salias.storems.services.StoreManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoreManagerControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private StoreManagerService storeManagerService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        storeManagerService = Mockito.mock(StoreManagerService.class);
        StoreManagerController controller = new StoreManagerController(storeManagerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        CreateStoreManagerRequest request = new CreateStoreManagerRequest(1L, 10L);
        StoreManagerDto response = new StoreManagerDto(1L, 10L);
        when(storeManagerService.create(any(CreateStoreManagerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/store-managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(1L))
                .andExpect(jsonPath("$.managerEmployeeId").value(10L));
    }

    @Test
    void getByStoreId_returnsOk() throws Exception {
        when(storeManagerService.getByStoreId(1L)).thenReturn(new StoreManagerDto(1L, 10L));

        mockMvc.perform(get("/api/v1/store-managers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(1L));
    }

    @Test
    void list_returnsPage() throws Exception {
        when(storeManagerService.list(any())).thenReturn(
                new PageImpl<>(List.of(new StoreManagerDto(1L, 10L)), PageRequest.of(0, 20), 1)
        );

        mockMvc.perform(get("/api/v1/store-managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeId").value(1L));
    }

    @Test
    void update_returnsOk() throws Exception {
        CreateStoreManagerRequest request = new CreateStoreManagerRequest(1L, 20L);
        when(storeManagerService.update(eq(1L), any(CreateStoreManagerRequest.class)))
                .thenReturn(new StoreManagerDto(1L, 20L));

        mockMvc.perform(put("/api/v1/store-managers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerEmployeeId").value(20L));
    }
}
