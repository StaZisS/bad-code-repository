package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal

@Schema(description = "Запрос для расчета маршрута")
data class RouteCalculationRequest(
    @field:NotEmpty(message = "Точки маршрута обязательны")
    @field:Valid
    @Schema(description = "Точки маршрута (минимум 2)")
    val points: List<RoutePoint>
)

@Schema(description = "Точка маршрута для расчета")
data class RoutePoint(
    @Schema(description = "Широта", example = "55.7558")
    val latitude: BigDecimal,

    @Schema(description = "Долгота", example = "37.6173")
    val longitude: BigDecimal
)