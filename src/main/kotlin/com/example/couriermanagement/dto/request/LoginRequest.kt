package com.example.couriermanagement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Данные для входа в систему")
data class LoginRequest(
    @field:NotBlank(message = "Логин обязателен")
    @Schema(description = "Логин пользователя", example = "admin")
    val login: String,

    @field:NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль пользователя", example = "password")
    val password: String
)