package com.example.couriermanagement.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ при ошибке валидации")
data class ValidationErrorResponse(
    @Schema(description = "Информация об ошибке валидации")
    val error: ValidationErrorInfo
)

@Schema(description = "Детали ошибки валидации")
data class ValidationErrorInfo(
    @Schema(description = "Код ошибки", example = "VALIDATION_FAILED")
    val code: String,

    @Schema(description = "Общее сообщение об ошибке", example = "Ошибка валидации данных")
    val message: String,

    @Schema(description = "Детализированные ошибки по полям")
    val details: Map<String, String>?
)