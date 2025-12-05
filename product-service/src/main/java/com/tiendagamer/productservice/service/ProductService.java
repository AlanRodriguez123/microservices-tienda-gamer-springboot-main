package com.tiendagamer.productservice.service;

import com.tiendagamer.productservice.dto.ProductRequest;
import com.tiendagamer.productservice.model.Product;
import com.tiendagamer.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Product create(ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImages(request.getImages());
        product.setSpecs(request.getSpecs());

        return productRepository.save(product);
    }
}
