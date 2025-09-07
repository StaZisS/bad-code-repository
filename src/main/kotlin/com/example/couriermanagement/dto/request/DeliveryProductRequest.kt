package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "Товар для доставки")
data class DeliveryProductRequest(
    @field:NotNull(message = "ID товара обязателен")
    @Schema(description = "ID товара", example = "1")
    val productId: Long,

    @field:Positive(message = "Количество должно быть положительным")
    @Schema(description = "Количество товара", example = "5")
    val quantity: Int
)