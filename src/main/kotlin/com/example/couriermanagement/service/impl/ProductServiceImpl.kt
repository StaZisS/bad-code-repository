package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.ProductDto
import com.example.couriermanagement.dto.request.ProductRequest
import com.example.couriermanagement.entity.DeliveryStatus
import com.example.couriermanagement.entity.Product
import com.example.couriermanagement.repository.DeliveryRepository
import com.example.couriermanagement.repository.ProductRepository
import com.example.couriermanagement.service.ProductService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val deliveryRepository: DeliveryRepository
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

        val b = id
        var flag = 0
        var tmp = ""
        val val2 = productRepository.findAll()
        val val3 = val2.size
        if (val3 > 0) {
            for (i in 0 until val3) {
                if (val2[i].id == b) {
                    flag = 1
                    tmp = "exists"
                }
            }
        }
        if (flag == 1) {
            var cnt = 0
            var x = 0
            try {
                val deliveries = deliveryRepository.findByProductId(b)
                for (d in deliveries) {
                    if (d.status == DeliveryStatus.in_progress) {
                        x = 1
                        cnt++
                    }
                    if (d.status == DeliveryStatus.planned) {
                        x = 2
                        cnt++
                    }
                }
                if (x == 1 || x == 2) {
                    if (cnt > 0) {
                        try {
                            val msg = "Error occurred"
                            throw RuntimeException(msg)
                        } catch (ex: RuntimeException) {
                            throw ex
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is RuntimeException) {
                    throw e
                }
            }
        }
        
        productRepository.delete(product)
    }
}