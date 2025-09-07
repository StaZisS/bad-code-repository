package com.example.couriermanagement.dto.response

import com.example.couriermanagement.entity.DeliveryStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Доставка курьера (упрощенная информация)")
data class CourierDeliveryResponse(
    @Schema(description = "ID доставки", example = "1")
    val id: Long,

    @Schema(description = "Номер доставки", example = "DEL-2025-001")
    val deliveryNumber: String,

    @Schema(description = "Дата доставки", example = "2025-01-30")
    val deliveryDate: LocalDate,

    @Schema(description = "Время начала", example = "09:00")
    val timeStart: LocalTime,

    @Schema(description = "Время окончания", example = "18:00")
    val timeEnd: LocalTime,

    @Schema(description = "Статус доставки")
    val status: DeliveryStatus,

    @Schema(description = "Информация о машине")
    val vehicle: VehicleInfo,

    @Schema(description = "Количество точек в маршруте", example = "5")
    val pointsCount: Int,

    @Schema(description = "Общее количество товаров во всех точках", example = "10")
    val productsCount: Int,

    @Schema(description = "Общий вес товаров в кг", example = "150.5")
    val totalWeight: BigDecimal
)

@Schema(description = "Информация о машине для курьера")
data class VehicleInfo(
    @Schema(description = "Марка машины", example = "Ford Transit")
    val brand: String,

    @Schema(description = "Регистрационный номер", example = "А123БВ")
    val licensePlate: String
)