package com.example.couriermanagement.repository

import com.example.couriermanagement.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    
    fun findByNameContainingIgnoreCase(name: String): List<Product>
    
    @Query("""
        SELECT p FROM Product p 
        WHERE p.weight <= :maxWeight
    """)
    fun findByMaxWeight(@Param("maxWeight") maxWeight: BigDecimal): List<Product>
    
    @Query("""
        SELECT p FROM Product p 
        WHERE (p.length * p.width * p.height / 1000000) <= :maxVolume
    """)
    fun findByMaxVolume(@Param("maxVolume") maxVolume: BigDecimal): List<Product>
    
    @Query("""
        SELECT p FROM Product p 
        WHERE p.weight <= :maxWeight 
        AND (p.length * p.width * p.height / 1000000) <= :maxVolume
    """)
    fun findByMaxWeightAndVolume(
        @Param("maxWeight") maxWeight: BigDecimal,
        @Param("maxVolume") maxVolume: BigDecimal
    ): List<Product>
}