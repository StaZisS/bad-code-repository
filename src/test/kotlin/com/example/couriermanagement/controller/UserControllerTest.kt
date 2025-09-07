package com.example.couriermanagement.controller

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.UserRequest
import com.example.couriermanagement.dto.request.UserUpdateRequest
import com.example.couriermanagement.entity.UserRole
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class UserControllerTest : BaseIntegrationTest() {

    @Test
    fun `get all users as admin should succeed`() {
        getWithAuth("/users", adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3)) // admin, manager, courier
    }

    @Test
    fun `get all users as manager should return 403`() {
        getWithAuth("/users", managerToken)
            .expectForbidden()
    }

    @Test
    fun `get all users as courier should return 403`() {
        getWithAuth("/users", courierToken)
            .expectForbidden()
    }

    @Test
    fun `get all users without auth should return 403`() {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users"))
            .expectForbidden()
    }

    @Test
    fun `get users filtered by role should return filtered results`() {
        getWithAuth("/users?role=courier", adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].role").value("courier"))
    }

    @Test
    fun `create user as admin should succeed`() {
        val userRequest = UserRequest(
            login = "newcourier",
            password = "password123",
            name = "Новый Курьер",
            role = UserRole.courier
        )

        postJson("/users", userRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.login").value("newcourier"))
            .andExpect(jsonPath("$.name").value("Новый Курьер"))
            .andExpect(jsonPath("$.role").value("courier"))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `create user as manager should return 403`() {
        val userRequest = UserRequest(
            login = "newcourier",
            password = "password123",
            name = "Новый Курьер",
            role = UserRole.courier
        )

        postJson("/users", userRequest, managerToken)
            .expectForbidden()
    }

    @Test
    fun `create user with duplicate login should return 400`() {
        val userRequest = UserRequest(
            login = "admin", // Already exists
            password = "password123",
            name = "Другой Админ",
            role = UserRole.admin
        )

        postJson("/users", userRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `create user with invalid data should return 400`() {
        val userRequest = UserRequest(
            login = "", // Empty login
            password = "", // Empty password
            name = "", // Empty name
            role = UserRole.courier
        )

        postJson("/users", userRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `create manager user should succeed`() {
        val userRequest = UserRequest(
            login = "newmanager",
            password = "password123",
            name = "Новый Менеджер",
            role = UserRole.manager
        )

        postJson("/users", userRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.role").value("manager"))
    }

    @Test
    fun `update user as admin should succeed`() {
        val updateRequest = UserUpdateRequest(
            name = "Обновленное Имя",
            login = "updatedcourier",
            role = UserRole.manager,
            password = "newpassword"
        )

        putJson("/users/${courierUser.id}", updateRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.name").value("Обновленное Имя"))
            .andExpect(jsonPath("$.login").value("updatedcourier"))
            .andExpect(jsonPath("$.role").value("manager"))
    }

    @Test
    fun `update user as manager should return 403`() {
        val updateRequest = UserUpdateRequest(
            name = "Обновленное Имя",
            login = null,
            role = null,
            password = null
        )

        putJson("/users/${courierUser.id}", updateRequest, managerToken)
            .expectForbidden()
    }

    @Test
    fun `update user with duplicate login should return 400`() {
        val updateRequest = UserUpdateRequest(
            name = null,
            login = "admin", // Already exists
            role = null,
            password = null
        )

        putJson("/users/${courierUser.id}", updateRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `update non-existent user should return 404`() {
        val updateRequest = UserUpdateRequest(
            name = "Обновленное Имя",
            login = null,
            role = null,
            password = null
        )

        putJson("/users/999", updateRequest, adminToken)
            .expectBadRequest() // Service throws IllegalArgumentException, which becomes 400
    }

    @Test
    fun `update user partial data should succeed`() {
        val updateRequest = UserUpdateRequest(
            name = "Только Новое Имя",
            login = null,
            role = null,
            password = null
        )

        putJson("/users/${courierUser.id}", updateRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.name").value("Только Новое Имя"))
            .andExpect(jsonPath("$.login").value(courierUser.login)) // Unchanged
            .andExpect(jsonPath("$.role").value(courierUser.role.name)) // Unchanged
    }

    @Test
    fun `delete user as admin should succeed`() {
        // Create a user to delete
        val userRequest = UserRequest(
            login = "todelete",
            password = "password123",
            name = "Для Удаления",
            role = UserRole.courier
        )
        
        val createResponse = postJson("/users", userRequest, adminToken)
            .expectSuccess()
            .andReturn()
        
        val createdUserId = objectMapper.readTree(createResponse.response.contentAsString)
            .get("id").asLong()

        deleteWithAuth("/users/$createdUserId", adminToken)
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete user as manager should return 403`() {
        deleteWithAuth("/users/${courierUser.id}", managerToken)
            .expectForbidden()
    }

    @Test
    fun `delete non-existent user should return 404`() {
        deleteWithAuth("/users/999", adminToken)
            .expectBadRequest() // Service throws IllegalArgumentException, which becomes 400
    }
}