package com.example.couriermanagement.service

import com.example.couriermanagement.dto.ProductDto
import com.example.couriermanagement.dto.request.ProductRequest

interface ProductService {
    fun getAllProducts(): List<ProductDto>
    fun createProduct(productRequest: ProductRequest): ProductDto
    fun updateProduct(id: Long, productRequest: ProductRequest): ProductDto
    fun deleteProduct(id: Long)
}