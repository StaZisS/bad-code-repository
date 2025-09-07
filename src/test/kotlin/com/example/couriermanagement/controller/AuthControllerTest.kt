package com.example.couriermanagement.controller

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.LoginRequest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class AuthControllerTest : BaseIntegrationTest() {

    @Test
    fun `login with valid credentials should return token and user info`() {
        val loginRequest = LoginRequest(
            login = "admin",
            password = "admin123"
        )

        postJson("/auth/login", loginRequest)
            .expectSuccess()
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.id").value(adminUser.id))
            .andExpect(jsonPath("$.user.login").value("admin"))
            .andExpect(jsonPath("$.user.name").value("Системный администратор"))
            .andExpect(jsonPath("$.user.role").value("admin"))
    }

    @Test
    fun `login with invalid login should return 400`() {
        val loginRequest = LoginRequest(
            login = "nonexistent",
            password = "password"
        )

        postJson("/auth/login", loginRequest)
            .expectBadRequest()
    }

    @Test
    fun `login with invalid password should return 400`() {
        val loginRequest = LoginRequest(
            login = "admin",
            password = "wrongpassword"
        )

        postJson("/auth/login", loginRequest)
            .expectBadRequest()
    }

    @Test
    fun `login with empty login should return 400`() {
        val loginRequest = LoginRequest(
            login = "",
            password = "admin123"
        )

        postJson("/auth/login", loginRequest)
            .expectBadRequest()
    }

    @Test
    fun `login with empty password should return 400`() {
        val loginRequest = LoginRequest(
            login = "admin",
            password = ""
        )

        postJson("/auth/login", loginRequest)
            .expectBadRequest()
    }

    @Test
    fun `login with manager credentials should return manager token`() {
        val loginRequest = LoginRequest(
            login = "manager",
            password = "password"
        )

        postJson("/auth/login", loginRequest)
            .expectSuccess()
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.role").value("manager"))
    }

    @Test
    fun `login with courier credentials should return courier token`() {
        val loginRequest = LoginRequest(
            login = "courier",
            password = "password"
        )

        postJson("/auth/login", loginRequest)
            .expectSuccess()
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.role").value("courier"))
    }
}