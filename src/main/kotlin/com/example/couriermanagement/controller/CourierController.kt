package com.example.couriermanagement.controller

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.response.CourierDeliveryResponse
import com.example.couriermanagement.entity.DeliveryStatus
import com.example.couriermanagement.service.CourierService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/courier")
@Tag(name = "Courier", description = "Операции курьера")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('courier')")
class CourierController(
    private val courierService: CourierService
) {

    @GetMapping("/deliveries")
    @Operation(
        summary = "Получить свои доставки",
        description = "Курьер видит только назначенные на него доставки с фильтрацией по статусу и дате"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Список доставок курьера"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    fun getCourierDeliveries(
        @Parameter(description = "Фильтр по дате доставки", example = "2025-01-30")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,

        @Parameter(description = "Фильтр по статусу")
        @RequestParam(required = false)
        status: DeliveryStatus?,

        @Parameter(description = "Начальная дата периода", example = "2025-01-25")
        @RequestParam(name = "date_from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateFrom: LocalDate?,

        @Parameter(description = "Конечная дата периода", example = "2025-01-31")
        @RequestParam(name = "date_to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateTo: LocalDate?
    ): ResponseEntity<List<CourierDeliveryResponse>> {
        val deliveries = courierService.getCourierDeliveries(date, status, dateFrom, dateTo)
        return ResponseEntity.ok(deliveries)
    }

    @GetMapping("/deliveries/{id}")
    @Operation(
        summary = "Получить детали своей доставки",
        description = "Получение подробной информации о доставке. Доступна только своя доставка"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Детали доставки"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен (не своя доставка)"),
            ApiResponse(responseCode = "404", description = "Доставка не найдена")
        ]
    )
    fun getCourierDeliveryById(
        @Parameter(description = "ID доставки", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<DeliveryDto> {
        val delivery = courierService.getCourierDeliveryById(id)
        return ResponseEntity.ok(delivery)
    }
}