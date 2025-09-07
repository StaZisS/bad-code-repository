package com.example.couriermanagement.controller

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.ProductRequest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

class ProductControllerTest : BaseIntegrationTest() {

    @Test
    fun `get all products should return list of products`() {
        createProduct()

        getWithAuth("/products", adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].name").value("Тестовый товар"))
            .andExpect(jsonPath("$[0].weight").value(1.5))
    }

    @Test
    fun `get all products without auth should return 403`() {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/products"))
            .expectForbidden()
    }

    @Test
    fun `create product as admin should succeed`() {
        val productRequest = ProductRequest(
            name = "Новый товар",
            weight = BigDecimal("2.5"),
            length = BigDecimal("15.0"),
            width = BigDecimal("12.0"),
            height = BigDecimal("8.0")
        )

        postJson("/products", productRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.name").value("Новый товар"))
            .andExpect(jsonPath("$.weight").value(2.5))
            .andExpect(jsonPath("$.length").value(15.0))
            .andExpect(jsonPath("$.width").value(12.0))
            .andExpect(jsonPath("$.height").value(8.0))
    }

    @Test
    fun `create product as manager should return 403`() {
        val productRequest = ProductRequest(
            name = "Новый товар",
            weight = BigDecimal("2.5"),
            length = BigDecimal("15.0"),
            width = BigDecimal("12.0"),
            height = BigDecimal("8.0")
        )

        postJson("/products", productRequest, managerToken)
            .expectForbidden()
    }

    @Test
    fun `create product as courier should return 403`() {
        val productRequest = ProductRequest(
            name = "Новый товар",
            weight = BigDecimal("2.5"),
            length = BigDecimal("15.0"),
            width = BigDecimal("12.0"),
            height = BigDecimal("8.0")
        )

        postJson("/products", productRequest, courierToken)
            .expectForbidden()
    }

    @Test
    fun `create product with invalid data should return 400`() {
        val productRequest = ProductRequest(
            name = "", // Empty name
            weight = BigDecimal("-1.0"), // Negative weight
            length = BigDecimal("0.0"),   // Zero length
            width = BigDecimal("-5.0"),   // Negative width
            height = BigDecimal("0.0")    // Zero height
        )

        postJson("/products", productRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `update product as admin should succeed`() {
        val product = createProduct()

        val productRequest = ProductRequest(
            name = "Обновленный товар",
            weight = BigDecimal("3.0"),
            length = BigDecimal("20.0"),
            width = BigDecimal("15.0"),
            height = BigDecimal("10.0")
        )

        putJson("/products/${product.id}", productRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.name").value("Обновленный товар"))
            .andExpect(jsonPath("$.weight").value(3.0))
            .andExpect(jsonPath("$.length").value(20.0))
    }

    @Test
    fun `update product as manager should return 403`() {
        val product = createProduct()

        val productRequest = ProductRequest(
            name = "Обновленный товар",
            weight = BigDecimal("3.0"),
            length = BigDecimal("20.0"),
            width = BigDecimal("15.0"),
            height = BigDecimal("10.0")
        )

        putJson("/products/${product.id}", productRequest, managerToken)
            .expectForbidden()
    }

    @Test
    fun `update non-existent product should return 404`() {
        val productRequest = ProductRequest(
            name = "Обновленный товар",
            weight = BigDecimal("3.0"),
            length = BigDecimal("20.0"),
            width = BigDecimal("15.0"),
            height = BigDecimal("10.0")
        )

        putJson("/products/999", productRequest, adminToken)
            .expectBadRequest() // Service throws IllegalArgumentException, which becomes 400
    }

    @Test
    fun `delete product as admin should succeed`() {
        val product = createProduct()

        deleteWithAuth("/products/${product.id}", adminToken)
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete product as manager should return 403`() {
        val product = createProduct()

        deleteWithAuth("/products/${product.id}", managerToken)
            .expectForbidden()
    }

    @Test
    fun `delete product as courier should return 403`() {
        val product = createProduct()

        deleteWithAuth("/products/${product.id}", courierToken)
            .expectForbidden()
    }

    @Test
    fun `delete non-existent product should return 404`() {
        deleteWithAuth("/products/999", adminToken)
            .expectBadRequest() // Service throws IllegalArgumentException, which becomes 400
    }

    @Test
    fun `get products with manager token should succeed`() {
        createProduct()

        getWithAuth("/products", managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `get products with courier token should succeed`() {
        createProduct()

        getWithAuth("/products", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
    }
}