package com.example.couriermanagement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalTime

@Schema(description = "Результат расчета маршрута")
data class RouteCalculationResponse(
    @Schema(description = "Расстояние в км", example = "25.5")
    val distanceKm: BigDecimal,

    @Schema(description = "Время в пути в минутах", example = "120")
    val durationMinutes: Int,

    @Schema(description = "Рекомендуемое время")
    val suggestedTime: SuggestedTime?
)

@Schema(description = "Рекомендуемое время для маршрута")
data class SuggestedTime(
    @Schema(description = "Рекомендуемое время начала", example = "09:00")
    val start: LocalTime,

    @Schema(description = "Рекомендуемое время окончания", example = "12:00")
    val end: LocalTime
)