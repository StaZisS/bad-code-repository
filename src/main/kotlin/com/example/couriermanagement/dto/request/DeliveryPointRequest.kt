package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

@Schema(description = "Точка маршрута доставки")
data class DeliveryPointRequest(
    @field:Positive(message = "Порядковый номер должен быть положительным")
    @Schema(description = "Порядковый номер в маршруте", example = "1")
    val sequence: Int?,

    @field:NotNull(message = "Широта обязательна")
    @Schema(description = "Широта", example = "55.7558")
    val latitude: BigDecimal,

    @field:NotNull(message = "Долгота обязательна")
    @Schema(description = "Долгота", example = "37.6173")
    val longitude: BigDecimal,

    @Schema(description = "Товары для доставки в данной точке")
    val products: List<DeliveryProductRequest> = emptyList()
)