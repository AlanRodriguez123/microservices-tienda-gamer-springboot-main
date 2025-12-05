package com.tiendagamer.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;
    @Positive
    @NotNull
    private Integer price;

    @NotBlank
    private String category;

    private List<String> images;

    private Map<String, Object> specs;
}

