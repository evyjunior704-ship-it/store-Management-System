package com.salias.storems.model.product;

import jakarta.persistence.*;

@Entity
public class Product {

    @Id
    @SequenceGenerator(name = "product_sequence",
            sequenceName =  "product_sequence",
            allocationSize = 1
            )
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator =  "product_sequence")
    @Column(name = "product_Id",
            updatable = false)
    private Long productID;

    @Column(name = "product_Name",
            nullable = false)
    private String productName;

    @Column(name = "Manufacturer",
            updatable = false)
    private String manufacturer;

    @Column(name = "Description",
            nullable = false)
    private String productDescription;

    public Product(){}

    public Product(Long productID,String productName,String manufacturer, String productDescription){
        this.productID = productID;
        this.productDescription = productDescription;
        this.productName = productName;
        this.manufacturer = manufacturer;

    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getProductID() {
        return productID;
    }

    public void setProductID(Long productID) {
        this.productID = productID;
    }
}
