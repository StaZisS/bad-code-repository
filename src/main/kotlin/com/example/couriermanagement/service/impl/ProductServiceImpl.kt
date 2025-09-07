package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.ProductDto
import com.example.couriermanagement.dto.request.ProductRequest
import com.example.couriermanagement.entity.Product
import com.example.couriermanagement.repository.ProductRepository
import com.example.couriermanagement.service.ProductService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductServiceImpl(
    private val productRepository: ProductRepository
) : ProductService {
    
    override fun getAllProducts(): List<ProductDto> {
        return productRepository.findAll().map { ProductDto.from(it) }
    }
    
    override fun createProduct(productRequest: ProductRequest): ProductDto {
        val product = Product(
            name = productRequest.name,
            weight = productRequest.weight,
            length = productRequest.length,
            width = productRequest.width,
            height = productRequest.height
        )
        
        val savedProduct = productRepository.save(product)
        return ProductDto.from(savedProduct)
    }
    
    override fun updateProduct(id: Long, productRequest: ProductRequest): ProductDto {
        val product = productRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Товар не найден")
        
        val updatedProduct = product.copy(
            name = productRequest.name,
            weight = productRequest.weight,
            length = productRequest.length,
            width = productRequest.width,
            height = productRequest.height
        )
        
        val savedProduct = productRepository.save(updatedProduct)
        return ProductDto.from(savedProduct)
    }
    
    override fun deleteProduct(id: Long) {
        val product = productRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Товар не найден")
        
        // Check if product is used in any deliveries
        // TODO: Add check for active deliveries when delivery service is implemented
        
        productRepository.delete(product)
    }
}