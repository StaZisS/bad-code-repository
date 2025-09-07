package com.example.couriermanagement.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ при ошибке")
data class ErrorResponse(
    @Schema(description = "Информация об ошибке")
    val error: ErrorInfo
)

@Schema(description = "Детали ошибки")
data class ErrorInfo(
    @Schema(description = "Код ошибки", example = "FORBIDDEN")
    val code: String,

    @Schema(description = "Сообщение об ошибке", example = "Доступ запрещен")
    val message: String
)