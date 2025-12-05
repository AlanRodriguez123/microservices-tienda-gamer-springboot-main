package com.tiendagamer.productservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String description;

    private Integer price;

    private String category;

    private List<String> images;

    // Flexible attributes
    private Map<String, Object> specs;
}
