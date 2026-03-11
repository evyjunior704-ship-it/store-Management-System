package com.salias.storems.services;

import com.salias.storems.dto.sales.CreateSaleRequest;
import com.salias.storems.dto.sales.SalesDto;
import com.salias.storems.exceptions.ResourceNotFoundException;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.sales.Sales;
import com.salias.storems.model.sales.SalesRepository;
import com.salias.storems.model.store.Store;
import com.salias.storems.model.store.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SalesService {

    private final SalesRepository salesRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public SalesService(SalesRepository salesRepository, ProductRepository productRepository, StoreRepository storeRepository) {
        this.salesRepository = salesRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    public SalesDto create(CreateSaleRequest req) {
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.productId()));
        Store store = storeRepository.findById(req.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + req.storeId()));

        Sales sale = new Sales(req.quantityBought(), req.price(), store);
        sale.setItemID(product);

        Sales saved = salesRepository.save(sale);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<SalesDto> list(Pageable pageable) {
        return salesRepository.findAll(pageable).map(this::toDto);
    }

    private SalesDto toDto(Sales s) {
        Long productId = (s.getItemID() == null) ? null : s.getItemID().getProductID();
        Long storeId = (s.getStore() == null) ? null : s.getStore().getStoreID();
        return new SalesDto(s.getSaleNumber(), productId, s.getQuantityBought(), s.getPrice(), storeId, s.getMadeAt());
    }
}
