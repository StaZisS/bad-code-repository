package com.example.couriermanagement.controller

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.request.DeliveryRequest
import com.example.couriermanagement.dto.request.GenerateDeliveriesRequest
import com.example.couriermanagement.dto.response.GenerateDeliveriesResponse
import com.example.couriermanagement.entity.DeliveryStatus
import com.example.couriermanagement.service.DeliveryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/deliveries")
@Tag(name = "Deliveries", description = "Управление доставками (менеджер)")
@SecurityRequirement(name = "bearerAuth")
class DeliveryController(
    private val deliveryService: DeliveryService
) {

    @PostMapping("/generate")
    @PreAuthorize("hasRole('manager')")
    @Operation(
        summary = "Автоматически сгенерировать доставки на несколько дней",
        description = """
            Доступно только для менеджера. Система автоматически распределяет курьеров и машины.
            Запрос представляет собой мапу, где ключ - дата доставки, значение - список маршрутов с товарами.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Доставки успешно сгенерированы"),
            ApiResponse(responseCode = "400", description = "Ошибка при генерации"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    fun generateDeliveries(
        @Valid @RequestBody generateRequest: GenerateDeliveriesRequest
    ): ResponseEntity<GenerateDeliveriesResponse> {
        val response = deliveryService.generateDeliveries(generateRequest)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @PreAuthorize("hasRole('manager')")
    @Operation(
        summary = "Получить список доставок",
        description = "Получение списка доставок с фильтрацией. Доступно только для менеджера"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Список доставок"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    fun getAllDeliveries(
        @Parameter(description = "Фильтр по дате", example = "2025-01-30")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,

        @Parameter(description = "Фильтр по ID курьера", example = "1")
        @RequestParam(name = "courier_id", required = false)
        courierId: Long?,

        @Parameter(description = "Фильтр по статусу")
        @RequestParam(required = false)
        status: DeliveryStatus?
    ): ResponseEntity<List<DeliveryDto>> {
        val deliveries = deliveryService.getAllDeliveries(date, courierId, status)
        return ResponseEntity.ok(deliveries)
    }

    @PostMapping
    @PreAuthorize("hasRole('manager')")
    @Operation(
        summary = "Создать доставку вручную",
        description = "Доступно только для менеджера. Проверяется время маршрута и вместимость машины"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Доставка создана"),
            ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    fun createDelivery(
        @Valid @RequestBody deliveryRequest: DeliveryRequest
    ): ResponseEntity<DeliveryDto> {
        val delivery = deliveryService.createDelivery(deliveryRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(delivery)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить детали доставки",
        description = "Получение подробной информации о доставке"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Детали доставки"),
            ApiResponse(responseCode = "404", description = "Доставка не найдена")
        ]
    )
    fun getDeliveryById(
        @Parameter(description = "ID доставки", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<DeliveryDto> {
        val delivery = deliveryService.getDeliveryById(id)
        return ResponseEntity.ok(delivery)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('manager')")
    @Operation(
        summary = "Обновить доставку",
        description = "Доступно только для менеджера. Нельзя редактировать за 3 дня до доставки"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Доставка обновлена"),
            ApiResponse(responseCode = "400", description = "Ошибка валидации (время/вместимость)"),
            ApiResponse(responseCode = "403", description = "Редактирование запрещено"),
            ApiResponse(responseCode = "404", description = "Доставка не найдена")
        ]
    )
    fun updateDelivery(
        @Parameter(description = "ID доставки", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody deliveryRequest: DeliveryRequest
    ): ResponseEntity<DeliveryDto> {
        val delivery = deliveryService.updateDelivery(id, deliveryRequest)
        return ResponseEntity.ok(delivery)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('manager')")
    @Operation(
        summary = "Удалить доставку",
        description = "Доступно только для менеджера. Нельзя удалять за 3 дня до доставки"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Доставка удалена"),
            ApiResponse(responseCode = "403", description = "Удаление запрещено (срок или роль)"),
            ApiResponse(responseCode = "404", description = "Доставка не найдена")
        ]
    )
    fun deleteDelivery(
        @Parameter(description = "ID доставки", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        deliveryService.deleteDelivery(id)
        return ResponseEntity.noContent().build()
    }
}