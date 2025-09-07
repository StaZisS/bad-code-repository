package com.example.couriermanagement.service

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

class RouteTimeValidationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var deliveryService: com.example.couriermanagement.service.DeliveryService

    @Test
    fun `openStreetMapService mock should work for long distance route`() {
        val distance = openStreetMapService.calculateDistance(
            BigDecimal("55.7558"), // Moscow
            BigDecimal("37.6176"),
            BigDecimal("59.9311"), // St. Petersburg  
            BigDecimal("30.3609")
        )
        
        // Should return our mocked value
        assert(distance == BigDecimal("635.0")) { 
            "Expected 635.0 km, but got $distance" 
        }
        
        println("✅ Long distance mock works: $distance km")
    }

    @Test
    fun `openStreetMapService mock should work for short distance route`() {
        val distance = openStreetMapService.calculateDistance(
            BigDecimal("55.7558"), // Moscow center
            BigDecimal("37.6176"),
            BigDecimal("55.7600"), // Moscow nearby
            BigDecimal("37.6200")
        )
        
        // Should return our mocked value
        assert(distance == BigDecimal("2.5")) { 
            "Expected 2.5 km, but got $distance" 
        }
        
        println("✅ Short distance mock works: $distance km")
    }

    @Test
    fun `delivery validation should pass for short route with sufficient time`() {
        val vehicle = createVehicle()
        val product = createProduct()
        
        // Create delivery request with short route and sufficient time (9 hours)
        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0), // 9 hours
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"), // Moscow center
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(productId = product.id, quantity = 1)
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("55.7600"), // Moscow nearby (~2.5 km)
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(productId = product.id, quantity = 1)
                    )
                )
            )
        )
        
        // This should succeed - short route with plenty of time
        try {
            // Установить контекст авторизации для manager-а (только manager может создавать доставки)
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val result = deliveryService.createDelivery(deliveryRequest)
            println("✅ Short route delivery created successfully: ${result.id}")
        } catch (e: Exception) {
            throw AssertionError("Expected delivery creation to succeed for short route, but got: ${e.message}")
        } finally {
            // Очистить контекст после теста
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `delivery validation should fail for long route with insufficient time`() {
        val vehicle = createVehicle()
        val product = createProduct()
        
        // Create delivery request with long route and insufficient time (30 minutes)
        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(9, 30), // Only 30 minutes
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"), // Moscow
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(productId = product.id, quantity = 1)
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("59.9311"), // St. Petersburg (~635 km)
                    longitude = BigDecimal("30.3609"),
                    products = listOf(
                        DeliveryProductRequest(productId = product.id, quantity = 1)
                    )
                )
            )
        )
        
        // This should fail - long route with insufficient time
        try {
            // Установить контекст авторизации для manager-а (только manager может создавать доставки)
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            deliveryService.createDelivery(deliveryRequest)
            throw AssertionError("Expected delivery creation to fail for long route with insufficient time")
        } catch (e: IllegalArgumentException) {
            println("✅ Long route delivery correctly rejected: ${e.message}")
            assert(e.message?.contains("Недостаточно времени") == true) {
                "Expected time validation error, but got: ${e.message}"
            }
        } finally {
            // Очистить контекст после теста
            SecurityContextHolder.clearContext()
        }
    }
}