package com.example.couriermanagement.dto.request

import com.example.couriermanagement.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные для создания пользователя")
data class UserRequest(
    @field:NotBlank(message = "Логин обязателен")
    @Schema(description = "Логин пользователя", example = "courier1")
    val login: String,

    @field:NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль пользователя", example = "password123")
    val password: String,

    @field:NotBlank(message = "Имя обязательно")
    @Schema(description = "Имя пользователя", example = "Иван Иванов")
    val name: String,

    @field:NotNull(message = "Роль обязательна")
    @Schema(description = "Роль пользователя")
    val role: UserRole
)