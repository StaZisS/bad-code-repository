package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Запрос для автоматической генерации доставок")
data class GenerateDeliveriesRequest(
    @field:NotEmpty(message = "Данные для генерации не могут быть пустыми")
    @field:Valid
    @Schema(
        description = "Мапа дат и маршрутов для доставки",
        example = """
        {
            "2025-01-30": [
                {
                    "route": [
                        {"latitude": 55.7558, "longitude": 37.6173},
                        {"latitude": 55.7600, "longitude": 37.6200}
                    ],
                    "products": [
                        {"product_id": 1, "quantity": 5}
                    ]
                }
            ]
        }
        """
    )
    val deliveryData: Map<LocalDate, List<RouteWithProducts>>
)

@Schema(description = "Маршрут с товарами для генерации доставки")
data class RouteWithProducts(
    @field:NotEmpty(message = "Маршрут не может быть пустым")
    @field:Valid
    @Schema(description = "Точки маршрута в порядке следования")
    val route: List<DeliveryPointRequest>,

    @field:NotEmpty(message = "Товары для доставки обязательны")
    @field:Valid
    @Schema(description = "Товары для доставки по этому маршруту")
    val products: List<DeliveryProductRequest>
)