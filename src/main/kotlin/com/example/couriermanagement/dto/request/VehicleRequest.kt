package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

@Schema(description = "Данные для создания/обновления машины")
data class VehicleRequest(
    @field:NotBlank(message = "Марка обязательна")
    @Schema(description = "Марка машины", example = "Ford Transit")
    val brand: String,

    @field:NotBlank(message = "Номер обязателен")
    @Schema(description = "Регистрационный номер", example = "А123БВ")
    val licensePlate: String,

    @field:PositiveOrZero(message = "Максимальный вес должен быть положительным")
    @Schema(description = "Максимальный вес в кг", example = "1000.0")
    val maxWeight: BigDecimal,

    @field:PositiveOrZero(message = "Максимальный объём должен быть положительным")
    @Schema(description = "Максимальный объём в м³", example = "15.5")
    val maxVolume: BigDecimal
)