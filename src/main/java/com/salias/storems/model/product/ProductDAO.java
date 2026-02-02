package com.salias.storems.model.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDAO extends JpaRepository<Product,Long> {
}
