package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.DeliveryPoint
import java.math.BigDecimal

data class DeliveryPointDto(
    val id: Long,
    val sequence: Int,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val products: List<DeliveryPointProductDto> = emptyList()
) {
    companion object {
        fun from(deliveryPoint: DeliveryPoint): DeliveryPointDto {
            return DeliveryPointDto(
                id = deliveryPoint.id,
                sequence = deliveryPoint.sequence,
                latitude = deliveryPoint.latitude,
                longitude = deliveryPoint.longitude,
                products = deliveryPoint.deliveryPointProducts.map { DeliveryPointProductDto.from(it) }
            )
        }
    }
}