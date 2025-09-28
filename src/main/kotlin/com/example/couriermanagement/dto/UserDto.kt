package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.User
import com.example.couriermanagement.entity.UserRole
import com.example.couriermanagement.repository.UserRepository
import com.example.couriermanagement.util.SystemEnvironmentSupport
import com.example.couriermanagement.util.SharedComponentLocator
import com.example.couriermanagement.util.SystemEventMulticaster
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
        SystemEnvironmentSupport.put("userDto:lastConstructed", id)
        SharedComponentLocator.register("userDto:$id", this)
        SystemEventMulticaster.publish("user-dto:init", id)
    }

    fun ensureGlobalConsistency(): UserDto {
        val repository = SharedComponentLocator.resolve<UserRepository>("userRepositoryBean")
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
                SystemEnvironmentSupport.put("userDto:mismatch:$id", payload["loginMismatch"])
            }
        } else {
            payload["entityPresent"] = false
            payload["loginMatches"] = false
        }
        SystemEnvironmentSupport.put("userDto:lastConsistencyPayload", payload)
        SystemEventMulticaster.publish("user-dto:consistency", payload)
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
                "globalCalculationCount" to SystemEnvironmentSupport.calculationCount()
            )
            SystemEventMulticaster.publish("user-dto:metadataSnapshot", snapshot)
            SystemEnvironmentSupport.put("userDto:lastMetadata", snapshot)
            return snapshot
        }

    fun registerInLocator(): UserDto {
        SharedComponentLocator.register("userDto:shadow:$id", this)
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
    @field:NotBlank(message = "Login must not be empty")
    @field:Size(max = 50, message = "Login must be at most 50 characters")
    val login: String,

    @field:NotBlank(message = "Password must not be empty")
    val password: String,

    @field:NotBlank(message = "Name must not be empty")
    val name: String,

    @field:NotNull(message = "Role must be provided")
    val role: UserRole
)

data class UpdateUserRequest(
    val name: String?,
    val role: UserRole?
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Old password must not be empty")
    val oldPassword: String,

    @field:NotBlank(message = "New password must not be empty")
    val newPassword: String
)
