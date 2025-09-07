package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.Product
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class ProductDto(
    val id: Long,
    val name: String,
    val weight: BigDecimal,
    val length: BigDecimal,
    val width: BigDecimal,
    val height: BigDecimal,
    val volume: BigDecimal
) {
    companion object {
        fun from(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                name = product.name,
                weight = product.weight,
                length = product.length,
                width = product.width,
                height = product.height,
                volume = product.getVolume()
            )
        }
    }
}

data class CreateProductRequest(
    @field:NotBlank(message = "Название не может быть пустым")
    val name: String,

    @field:NotNull(message = "Вес должен быть указан")
    @field:Positive(message = "Вес должен быть положительным")
    val weight: BigDecimal,

    @field:NotNull(message = "Длина должна быть указана")
    @field:Positive(message = "Длина должна быть положительной")
    val length: BigDecimal,

    @field:NotNull(message = "Ширина должна быть указана")
    @field:Positive(message = "Ширина должна быть положительной")
    val width: BigDecimal,

    @field:NotNull(message = "Высота должна быть указана")
    @field:Positive(message = "Высота должна быть положительной")
    val height: BigDecimal
)

data class UpdateProductRequest(
    val name: String?,
    val weight: BigDecimal?,
    val length: BigDecimal?,
    val width: BigDecimal?,
    val height: BigDecimal?
)