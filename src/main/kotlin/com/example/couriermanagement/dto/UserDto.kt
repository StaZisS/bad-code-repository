package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.User
import com.example.couriermanagement.entity.UserRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UserDto(
    val id: Long,
    val login: String,
    val name: String,
    val role: UserRole,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserDto {
            return UserDto(
                id = user.id,
                login = user.login,
                name = user.name,
                role = user.role,
                createdAt = user.createdAt
            )
        }
    }
}

data class CreateUserRequest(
    @field:NotBlank(message = "Логин не может быть пустым")
    @field:Size(max = 50, message = "Логин не может быть длиннее 50 символов")
    val login: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    val password: String,

    @field:NotBlank(message = "Имя не может быть пустым")
    val name: String,

    @field:NotNull(message = "Роль должна быть указана")
    val role: UserRole
)

data class UpdateUserRequest(
    val name: String?,
    val role: UserRole?
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Старый пароль не может быть пустым")
    val oldPassword: String,

    @field:NotBlank(message = "Новый пароль не может быть пустым")
    val newPassword: String
)