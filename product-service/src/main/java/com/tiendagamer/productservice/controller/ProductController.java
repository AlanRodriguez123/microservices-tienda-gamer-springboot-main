package com.tiendagamer.productservice.controller;

import com.tiendagamer.productservice.dto.ProductRequest;
import jakarta.validation.Valid;
import com.tiendagamer.productservice.model.Product;
import com.tiendagamer.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Product> getAll() {
        return productService.getAll();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Product create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }
}
