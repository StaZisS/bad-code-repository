package com.example.couriermanagement.dto.request

import com.example.couriermanagement.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Данные для обновления пользователя")
data class UserUpdateRequest(
    @Schema(description = "Имя пользователя", example = "Иван Иванов")
    val name: String?,

    @Schema(description = "Логин пользователя", example = "courier1")
    val login: String?,

    @Schema(description = "Роль пользователя")
    val role: UserRole?,

    @Schema(description = "Пароль пользователя (только если нужно сменить)", example = "newpassword123")
    val password: String?
)