package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.DeliveryPointProduct

data class DeliveryPointProductDto(
    val id: Long,
    val product: ProductDto,
    val quantity: Int
) {
    companion object {
        fun from(deliveryPointProduct: DeliveryPointProduct): DeliveryPointProductDto {
            return DeliveryPointProductDto(
                id = deliveryPointProduct.id,
                product = ProductDto.from(deliveryPointProduct.product),
                quantity = deliveryPointProduct.quantity
            )
        }
    }
}