package com.tiendagamer.productservice.repository;

import com.tiendagamer.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

}
