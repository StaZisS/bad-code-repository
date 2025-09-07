package com.example.couriermanagement.controller

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.*
import com.example.couriermanagement.entity.DeliveryStatus
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

class DeliveryControllerTest : BaseIntegrationTest() {

    @Test
    fun `get all deliveries as manager should succeed`() {
        createDelivery()

        getWithAuth("/deliveries", managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `get all deliveries as courier should return 403`() {
        getWithAuth("/deliveries", courierToken)
            .expectForbidden()
    }

    @Test
    fun `get all deliveries as admin should return 403`() {
        getWithAuth("/deliveries", adminToken)
            .expectForbidden()
    }

    @Test
    fun `get deliveries with date filter should return filtered results`() {
        val delivery = createDelivery()
        val deliveryDate = delivery.deliveryDate

        getWithAuth("/deliveries?date=$deliveryDate", managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `get deliveries with courier filter should return filtered results`() {
        createDelivery()

        getWithAuth("/deliveries?courier_id=${courierUser.id}", managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `get deliveries with status filter should return filtered results`() {
        createDelivery()

        getWithAuth("/deliveries?status=planned", managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("planned"))
    }

    @Test
    fun `create delivery as manager should succeed`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 3
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.courier.id").value(courierUser.id))
            .andExpect(jsonPath("$.vehicle.id").value(vehicle.id))
            .andExpect(jsonPath("$.deliveryPoints.length()").value(1))
            .andExpect(jsonPath("$.deliveryPoints[0].products.length()").value(1))
    }

    @Test
    fun `create delivery as courier should return 403`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 3
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, courierToken)
            .expectForbidden()
    }

    @Test
    fun `create delivery with invalid courier role should return 400`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = adminUser.id, // Admin, not courier
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 3
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `create delivery with past date should return 400`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().minusDays(1), // Past date
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 3
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `create delivery with invalid time should return 400`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(18, 0), // Start after end
            timeEnd = LocalTime.of(9, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 3
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `get delivery by id should return delivery details`() {
        val delivery = createDelivery()

        getWithAuth("/deliveries/${delivery.id}", managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.id").value(delivery.id))
            .andExpect(jsonPath("$.deliveryPoints").isArray)
            .andExpect(jsonPath("$.canEdit").isBoolean)
    }

    @Test
    fun `update delivery as manager should succeed when more than 3 days before`() {
        val delivery = createDelivery()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = delivery.vehicle!!.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(10, 0), // Changed time
            timeEnd = LocalTime.of(19, 0),   // Changed time
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7600"), // Changed coordinates
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 5 // Changed quantity
                        )
                    )
                )
            )
        )

        putJson("/deliveries/${delivery.id}", deliveryRequest, managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.timeStart").value("10:00:00"))
            .andExpect(jsonPath("$.timeEnd").value("19:00:00"))
    }

    @Test
    fun `update delivery less than 3 days before should return 400`() {
        // Create delivery for tomorrow (less than 3 days)
        val vehicle = createVehicle()
        val nearDelivery = deliveryRepository.save(
            com.example.couriermanagement.entity.Delivery(
                courier = courierUser,
                vehicle = vehicle,
                createdBy = managerUser,
                deliveryDate = LocalDate.now().plusDays(1), // Tomorrow
                timeStart = LocalTime.of(9, 0),
                timeEnd = LocalTime.of(18, 0),
                status = DeliveryStatus.planned,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )

        val product = createProduct()
        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(10, 0),
            timeEnd = LocalTime.of(19, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 3
                        )
                    )
                )
            )
        )

        putJson("/deliveries/${nearDelivery.id}", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `delete delivery as manager should succeed when more than 3 days before`() {
        val delivery = createDelivery()

        deleteWithAuth("/deliveries/${delivery.id}", managerToken)
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete delivery less than 3 days before should return 400`() {
        val vehicle = createVehicle()
        val nearDelivery = deliveryRepository.save(
            com.example.couriermanagement.entity.Delivery(
                courier = courierUser,
                vehicle = vehicle,
                createdBy = managerUser,
                deliveryDate = LocalDate.now().plusDays(1), // Tomorrow
                timeStart = LocalTime.of(9, 0),
                timeEnd = LocalTime.of(18, 0),
                status = DeliveryStatus.planned,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )

        deleteWithAuth("/deliveries/${nearDelivery.id}", managerToken)
            .expectBadRequest()
    }

    @Test
    fun `generate deliveries as manager should succeed`() {
        val product = createProduct()

        val generateRequest = GenerateDeliveriesRequest(
            deliveryData = mapOf(
                LocalDate.now().plusDays(5) to listOf(
                    RouteWithProducts(
                        route = listOf(
                            DeliveryPointRequest(
                                sequence = 1,
                                latitude = BigDecimal("55.7558"),
                                longitude = BigDecimal("37.6176"),
                                products = emptyList()
                            )
                        ),
                        products = listOf(
                            DeliveryProductRequest(
                                productId = product.id,
                                quantity = 5
                            )
                        )
                    )
                )
            )
        )

        postJson("/deliveries/generate", generateRequest, managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.totalGenerated").exists())
            .andExpect(jsonPath("$.byDate").exists())
    }

    @Test
    fun `generate deliveries as courier should return 403`() {
        val product = createProduct()

        val generateRequest = GenerateDeliveriesRequest(
            deliveryData = mapOf(
                LocalDate.now().plusDays(5) to listOf(
                    RouteWithProducts(
                        route = listOf(
                            DeliveryPointRequest(
                                sequence = 1,
                                latitude = BigDecimal("55.7558"),
                                longitude = BigDecimal("37.6176"),
                                products = emptyList()
                            )
                        ),
                        products = listOf(
                            DeliveryProductRequest(
                                productId = product.id,
                                quantity = 5
                            )
                        )
                    )
                )
            )
        )

        postJson("/deliveries/generate", generateRequest, courierToken)
            .expectForbidden()
    }

    @Test
    fun `create delivery with insufficient time window should return 400`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(9, 30), // Only 30 minutes for long route
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"), // Moscow
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 1
                        )
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("59.9311"), // St. Petersburg - 635km away
                    longitude = BigDecimal("30.3609"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 1
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `update delivery with insufficient time window should return 400`() {
        val delivery = createDelivery()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = delivery.vehicle!!.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(10, 0),
            timeEnd = LocalTime.of(10, 30), // Only 30 minutes for long route
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"), // Moscow
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 1
                        )
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("59.9311"), // St. Petersburg - 635km away
                    longitude = BigDecimal("30.3609"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 1
                        )
                    )
                )
            )
        )

        putJson("/deliveries/${delivery.id}", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `create delivery with sufficient time window should succeed`() {
        val vehicle = createVehicle()
        val product = createProduct()

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0), // 9 hours should be enough for short routes
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"), // Moscow
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 1
                        )
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("55.7600"), // Short distance within Moscow - ~2.5 km
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = product.id,
                            quantity = 1
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectSuccess()
            .andExpect(jsonPath("$.deliveryPoints.length()").value(2))
    }

    @Test
    fun `create delivery should fail when vehicle weight capacity exceeded`() {
        // Create heavy product (500kg each)
        val heavyProduct = productRepository.save(
            com.example.couriermanagement.entity.Product(
                name = "Тяжелый товар",
                weight = BigDecimal("500.0"),
                length = BigDecimal("100.0"),
                width = BigDecimal("100.0"), 
                height = BigDecimal("100.0")
            )
        )
        
        // Create small vehicle (1000kg capacity)
        val smallVehicle = vehicleRepository.save(
            com.example.couriermanagement.entity.Vehicle(
                brand = "Маленький грузовик",
                licensePlate = "SMALL456",
                maxWeight = BigDecimal("1000.0"),
                maxVolume = BigDecimal("10.0")
            )
        )

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = smallVehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 3 // 3 * 500kg = 1500kg > 1000kg capacity
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectBadRequest()
    }

    @Test
    fun `create delivery should fail when vehicle volume capacity exceeded`() {
        // Create bulky product (8 m³ each)
        val bulkyProduct = productRepository.save(
            com.example.couriermanagement.entity.Product(
                name = "Объемный товар",
                weight = BigDecimal("10.0"),
                length = BigDecimal("200.0"), // 200cm
                width = BigDecimal("200.0"),  // 200cm
                height = BigDecimal("200.0")  // 200cm = 8 m³
            )
        )
        
        // Create small vehicle (10 m³ capacity)  
        val smallVehicle = vehicleRepository.save(
            com.example.couriermanagement.entity.Vehicle(
                brand = "Маленький грузовик",
                licensePlate = "SMALL789",
                maxWeight = BigDecimal("2000.0"),
                maxVolume = BigDecimal("10.0") // Only 10 m³
            )
        )

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = smallVehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = bulkyProduct.id,
                            quantity = 2 // 2 * 8 m³ = 16 m³ > 10 m³ capacity
                        )
                    )
                )
            )
        )

        postJson("/deliveries", deliveryRequest, managerToken)
            .expectBadRequest()
    }
}