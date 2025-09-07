package com.example.couriermanagement.dto.response

import com.example.couriermanagement.dto.UserDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ при успешной авторизации")
data class LoginResponse(
    @Schema(description = "JWT токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,

    @Schema(description = "Информация о пользователе")
    val user: UserDto
)