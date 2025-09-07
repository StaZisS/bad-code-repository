package com.example.couriermanagement.repository

import com.example.couriermanagement.entity.Delivery
import com.example.couriermanagement.entity.DeliveryPoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DeliveryPointRepository : JpaRepository<DeliveryPoint, Long> {
    
    fun findByDeliveryOrderBySequence(delivery: Delivery): List<DeliveryPoint>
    
    fun findByDeliveryId(deliveryId: Long): List<DeliveryPoint>
    
    fun deleteByDeliveryId(deliveryId: Long)
    
    @Query("""
        SELECT dp FROM DeliveryPoint dp 
        WHERE dp.delivery.id = :deliveryId 
        ORDER BY dp.sequence
    """)
    fun findByDeliveryIdOrderBySequence(@Param("deliveryId") deliveryId: Long): List<DeliveryPoint>
    
    @Query("""
        SELECT MAX(dp.sequence) FROM DeliveryPoint dp 
        WHERE dp.delivery.id = :deliveryId
    """)
    fun findMaxSequenceByDeliveryId(@Param("deliveryId") deliveryId: Long): Int?
    
    fun findByDeliveryAndSequence(delivery: Delivery, sequence: Int): DeliveryPoint?
}