package com.salias.storems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salias.storems.dto.product.CreateProductRequest;
import com.salias.storems.dto.product.ProductDto;
import com.salias.storems.dto.product.UpdateProductRequest;
import com.salias.storems.exceptions.GlobalExceptionHandler;
import com.salias.storems.services.ProductService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        productService = Mockito.mock(ProductService.class);
        ProductController controller = new ProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        ProductDto dto = new ProductDto(1L, "Milk", "Acme", "2% milk");
        when(productService.create(any(CreateProductRequest.class))).thenReturn(dto);

        CreateProductRequest request = new CreateProductRequest("Milk", "Acme", "2% milk");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Milk"));
    }

    @Test
    void getById_returnsOk() throws Exception {
        ProductDto dto = new ProductDto(5L, "Bread", "Acme", "Whole wheat");
        when(productService.getById(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/products/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Bread"));
    }

    @Test
    void list_returnsPagedResult() throws Exception {
        ProductDto dto = new ProductDto(1L, "Milk", "Acme", "2% milk");
        when(productService.list(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void update_returnsOk() throws Exception {
        ProductDto dto = new ProductDto(3L, "Eggs", "Farm Co", "Large");
        when(productService.update(eq(3L), any(UpdateProductRequest.class))).thenReturn(dto);

        UpdateProductRequest request = new UpdateProductRequest("Eggs", "Farm Co", "Large");

        mockMvc.perform(put("/api/v1/products/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(productService).delete(7L);

        mockMvc.perform(delete("/api/v1/products/7"))
                .andExpect(status().isNoContent());
    }
}
