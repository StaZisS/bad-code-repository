package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.Delivery
import com.example.couriermanagement.entity.DeliveryStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class DeliveryDto(
    val id: Long,
    val deliveryNumber: String?,
    val courier: UserDto?,
    val vehicle: VehicleDto?,
    val createdBy: UserDto,
    val deliveryDate: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
    val status: DeliveryStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deliveryPoints: List<DeliveryPointDto>,
    val totalWeight: BigDecimal,
    val totalVolume: BigDecimal,
    val canEdit: Boolean = false
) {
    companion object {
        fun from(delivery: Delivery): DeliveryDto {
            // Calculate totals from all delivery point products
            val allProducts = delivery.deliveryPoints.flatMap { it.deliveryPointProducts }
            val totalWeight = allProducts.sumOf { 
                it.product.weight * BigDecimal(it.quantity) 
            }
            val totalVolume = allProducts.sumOf { 
                it.product.getVolume() * BigDecimal(it.quantity) 
            }
            
            // Check if delivery can be edited (more than 3 days before delivery date)
            val canEdit = delivery.deliveryDate.isAfter(LocalDate.now().plusDays(3))
            
            return DeliveryDto(
                id = delivery.id,
                deliveryNumber = "DEL-${delivery.deliveryDate.year}-${delivery.id.toString().padStart(3, '0')}",
                courier = delivery.courier?.let { UserDto.from(it) },
                vehicle = delivery.vehicle?.let { VehicleDto.from(it) },
                createdBy = UserDto.from(delivery.createdBy),
                deliveryDate = delivery.deliveryDate,
                timeStart = delivery.timeStart,
                timeEnd = delivery.timeEnd,
                status = delivery.status,
                createdAt = delivery.createdAt,
                updatedAt = delivery.updatedAt,
                deliveryPoints = delivery.deliveryPoints.map { DeliveryPointDto.from(it) },
                totalWeight = totalWeight,
                totalVolume = totalVolume,
                canEdit = canEdit
            )
        }
    }
}

data class DeliveryFilterRequest(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val courierId: Long?,
    val status: DeliveryStatus?
)