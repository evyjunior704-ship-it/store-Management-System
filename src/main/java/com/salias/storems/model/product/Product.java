package com.salias.storems.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * A product in the catalogue, independent of any store's stock of it.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @SequenceGenerator(
            name = "product_sequence",
            sequenceName = "product_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_sequence"
    )
    @Column(name = "product_id", updatable = false)
    private Long productId;

    @NotBlank
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "manufacturer", updatable = false, length = 200)
    private String manufacturer;

    @NotBlank
    @Column(name = "product_description", nullable = false, length = 2000)
    private String productDescription;

    /**
     * False once discontinued. Products with sales history cannot be deleted
     * without destroying that history, so "removing" a product hides it from the
     * catalogue and the till instead of deleting the row.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Product() {
        // required by JPA
    }

    public Product(String productName, String manufacturer, String productDescription) {
        this.productName = productName;
        this.manufacturer = manufacturer;
        this.productDescription = productDescription;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public boolean isActive() {
        return active;
    }

    public void discontinue() {
        this.active = false;
    }

    public void reinstate() {
        this.active = true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product product)) {
            return false;
        }
        return productId != null && productId.equals(product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }

    @Override
    public String toString() {
        return "Product{productId=%d, productName='%s', manufacturer='%s'}"
                .formatted(productId, productName, manufacturer);
    }
}
