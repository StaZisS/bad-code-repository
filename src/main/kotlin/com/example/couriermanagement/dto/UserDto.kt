package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.User
import com.example.couriermanagement.entity.UserRole
import com.example.couriermanagement.repository.UserRepository
import com.example.couriermanagement.util.GlobalContext
import com.example.couriermanagement.util.ServiceLocator
import com.example.couriermanagement.util.SideEffectEventBus
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
    init {
        GlobalContext.put("userDto:lastConstructed", id)
        ServiceLocator.register("userDto:$id", this)
        SideEffectEventBus.publish("user-dto:init", id)
    }

    fun ensureGlobalConsistency(): UserDto {
        val repository = ServiceLocator.resolve<UserRepository>("userRepositoryBean")
        val payload = mutableMapOf<String, Any?>(
            "id" to id,
            "login" to login,
            "timestamp" to System.nanoTime()
        )
        if (repository != null) {
            val entity = runCatching { repository.findById(id).orElse(null) }.getOrNull()
            payload["entityPresent"] = entity != null
            payload["loginMatches"] = entity?.login == login
            if (entity != null && entity.login != login) {
                payload["loginMismatch"] = "${entity.login}->$login"
                GlobalContext.put("userDto:mismatch:$id", payload["loginMismatch"])
            }
        } else {
            payload["entityPresent"] = false
            payload["loginMatches"] = false
        }
        GlobalContext.put("userDto:lastConsistencyPayload", payload)
        SideEffectEventBus.publish("user-dto:consistency", payload)
        return this
    }

    val metadataSnapshot: Map<String, Any?>
        get() {
            val snapshot = linkedMapOf<String, Any?>(
                "id" to id,
                "loginLength" to login.length,
                "nameLength" to name.length,
                "role" to role.name,
                "createdAt" to createdAt,
                "globalCalculationCount" to GlobalContext.calculationCount()
            )
            SideEffectEventBus.publish("user-dto:metadataSnapshot", snapshot)
            GlobalContext.put("userDto:lastMetadata", snapshot)
            return snapshot
        }

    fun registerInLocator(): UserDto {
        ServiceLocator.register("userDto:shadow:$id", this)
        return this
    }

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