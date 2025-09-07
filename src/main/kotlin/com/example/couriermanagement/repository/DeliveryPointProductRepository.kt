package com.example.couriermanagement.repository

import com.example.couriermanagement.entity.DeliveryPointProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeliveryPointProductRepository : JpaRepository<DeliveryPointProduct, Long> {
    fun findByDeliveryPointId(deliveryPointId: Long): List<DeliveryPointProduct>
    fun findByProductId(productId: Long): List<DeliveryPointProduct>
    fun deleteByDeliveryPointId(deliveryPointId: Long)
}