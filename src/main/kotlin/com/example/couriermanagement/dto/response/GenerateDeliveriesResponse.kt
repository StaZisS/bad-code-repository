package com.example.couriermanagement.dto.response

import com.example.couriermanagement.dto.DeliveryDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Результат генерации доставок")
data class GenerateDeliveriesResponse(
    @Schema(description = "Общее количество созданных доставок", example = "5")
    val totalGenerated: Int,

    @Schema(description = "Результаты генерации по датам")
    val byDate: Map<LocalDate, GenerationResultByDate>
)

@Schema(description = "Результат генерации доставок для конкретной даты")
data class GenerationResultByDate(
    @Schema(description = "Количество доставок на эту дату", example = "3")
    val generatedCount: Int,

    @Schema(description = "Созданные доставки")
    val deliveries: List<DeliveryDto>,

    @Schema(description = "Предупреждения при генерации")
    val warnings: List<String>?
)