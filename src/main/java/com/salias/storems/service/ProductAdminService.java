package com.salias.storems.service;

import com.salias.storems.model.inventoryitem.InventoryItemRepository;
import com.salias.storems.model.itemsupplier.ItemSupplierRepository;
import com.salias.storems.model.product.Product;
import com.salias.storems.model.product.ProductRepository;
import com.salias.storems.model.sales.SalesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catalogue management: manager-only.
 *
 * <p>"Removing" a product discontinues it rather than deleting the row, because a
 * product with sales history is referenced by {@code sales} and deleting it
 * would either fail on the foreign key or destroy that history. Hard deletion is
 * offered only for products nothing references yet.
 */
@Service
@Transactional(readOnly = true)
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final SalesRepository salesRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ItemSupplierRepository itemSupplierRepository;

    public ProductAdminService(ProductRepository productRepository,
                               SalesRepository salesRepository,
                               InventoryItemRepository inventoryItemRepository,
                               ItemSupplierRepository itemSupplierRepository) {
        this.productRepository = productRepository;
        this.salesRepository = salesRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.itemSupplierRepository = itemSupplierRepository;
    }

    public List<Product> searchActive(String term) {
        if (term == null || term.isBlank()) {
            return productRepository.findAll().stream().filter(Product::isActive).limit(200).toList();
        }
        return productRepository.findByActiveTrueAndProductNameContainingIgnoreCase(term.trim());
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No product with id " + productId));
    }

    @Transactional
    public Product create(String name, String manufacturer, String description) {
        return productRepository.save(new Product(name, manufacturer, description));
    }

    @Transactional
    public Product update(Long productId, String name, String manufacturer, String description) {
        Product product = findById(productId);
        product.setProductName(name);
        product.setManufacturer(manufacturer);
        product.setProductDescription(description);
        return product;
    }

    /** Hides the product from the catalogue and the till, keeping its history. */
    @Transactional
    public Product discontinue(Long productId) {
        Product product = findById(productId);
        product.discontinue();
        return product;
    }

    @Transactional
    public Product reinstate(Long productId) {
        Product product = findById(productId);
        product.reinstate();
        return product;
    }

    /** True when no sale, stock row or supply arrangement points at this product. */
    public boolean isDeletable(Long productId) {
        return !salesRepository.existsByProduct_ProductId(productId)
                && !inventoryItemRepository.existsByProduct_ProductId(productId)
                && !itemSupplierRepository.existsByProduct_ProductId(productId);
    }

    /**
     * Permanently deletes a product, but only if nothing references it.
     *
     * @throws IllegalArgumentException if the product has history; discontinue it instead
     */
    @Transactional
    public void deletePermanently(Long productId) {
        Product product = findById(productId);
        if (!isDeletable(productId)) {
            throw new IllegalArgumentException(
                    ("%s has sales or stock history and cannot be deleted. "
                            + "Discontinue it instead to keep the history intact.")
                            .formatted(product.getProductName()));
        }
        productRepository.delete(product);
    }
}
