package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Данные для создания/обновления доставки")
data class DeliveryRequest(
    @field:NotNull(message = "ID курьера обязателен")
    @Schema(description = "ID курьера", example = "1")
    val courierId: Long,

    @field:NotNull(message = "ID машины обязателен")
    @Schema(description = "ID машины", example = "1")
    val vehicleId: Long,

    @field:NotNull(message = "Дата доставки обязательна")
    @Schema(description = "Дата доставки", example = "2025-01-30")
    val deliveryDate: LocalDate,

    @field:NotNull(message = "Время начала обязательно")
    @Schema(description = "Время начала", example = "09:00")
    val timeStart: LocalTime,

    @field:NotNull(message = "Время окончания обязательно")
    @Schema(description = "Время окончания", example = "18:00")
    val timeEnd: LocalTime,

    @field:NotEmpty(message = "Точки маршрута обязательны")
    @field:Valid
    @Schema(description = "Точки маршрута с товарами")
    val points: List<DeliveryPointRequest>
)