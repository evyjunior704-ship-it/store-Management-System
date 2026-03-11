package com.salias.storems.services;

import com.salias.storems.dto.product.CreateProductRequest;
import com.salias.storems.dto.product.ProductDto;
import com.salias.storems.dto.product.UpdateProductRequest;
import com.salias.storems.exceptions.ResourceNotFoundException;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductDto create(CreateProductRequest req) {
        Product product = new Product(null, req.name(), req.manufacturer(), req.description());
        Product saved = productRepository.save(product);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        return productRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> list(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toDto);
    }

    public ProductDto update(Long id, UpdateProductRequest req) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        existing.setProductName(req.name());
        existing.setManufacturer(req.manufacturer());
        existing.setProductDescription(req.description());

        Product saved = productRepository.save(existing);
        return toDto(saved);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(p.getProductID(), p.getProductName(), p.getManufacturer(), p.getProductDescription());
    }
}
