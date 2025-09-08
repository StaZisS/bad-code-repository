package com.example.couriermanagement.controller

import com.example.couriermanagement.dto.request.LoginRequest
import com.example.couriermanagement.dto.response.LoginResponse
import com.example.couriermanagement.service.AuthService
import com.example.couriermanagement.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Аутентификация")
class AuthController(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/login")
    @Operation(
        summary = "Вход в систему",
        description = "Аутентификация пользователя по логину и паролю"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Успешная авторизация"),
            ApiResponse(responseCode = "401", description = "Неверные данные для входа"),
            ApiResponse(responseCode = "400", description = "Ошибка валидации")
        ]
    )
    @SecurityRequirement(name = "")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(loginRequest)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/debug")
    @SecurityRequirement(name = "")
    fun debug(): ResponseEntity<Map<String, String>> {
        val adminUser = userRepository.findByLogin("admin")
        val newHash = passwordEncoder.encode("admin123")
        val result = mutableMapOf<String, String>()
        
        if (adminUser != null) {
            result["currentHash"] = adminUser.passwordHash
            result["newHash"] = newHash
            result["matches"] = passwordEncoder.matches("admin123", adminUser.passwordHash).toString()

            val updatedUser = adminUser.copy(passwordHash = newHash)
            userRepository.save(updatedUser)
            result["updated"] = "true"
        } else {
            result["error"] = "Admin user not found"
        }
        
        return ResponseEntity.ok(result)
    }
}