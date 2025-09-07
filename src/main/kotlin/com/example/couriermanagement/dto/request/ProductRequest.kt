package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

@Schema(description = "Данные для создания/обновления товара")
data class ProductRequest(
    @field:NotBlank(message = "Название обязательно")
    @Schema(description = "Название товара", example = "Телефон")
    val name: String,

    @field:PositiveOrZero(message = "Вес должен быть положительным")
    @Schema(description = "Вес в кг", example = "0.2")
    val weight: BigDecimal,

    @field:PositiveOrZero(message = "Длина должна быть положительной")
    @Schema(description = "Длина в см", example = "15.0")
    val length: BigDecimal,

    @field:PositiveOrZero(message = "Ширина должна быть положительной")
    @Schema(description = "Ширина в см", example = "7.0")
    val width: BigDecimal,

    @field:PositiveOrZero(message = "Высота должна быть положительной")
    @Schema(description = "Высота в см", example = "1.0")
    val height: BigDecimal
)