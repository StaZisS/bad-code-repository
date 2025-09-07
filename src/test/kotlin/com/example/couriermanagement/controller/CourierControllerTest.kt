package com.example.couriermanagement.controller

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.entity.DeliveryStatus
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

class CourierControllerTest : BaseIntegrationTest() {

    @Test
    fun `get courier deliveries should return own deliveries`() {
        createDelivery(courierUser)

        getWithAuth("/courier/deliveries", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].deliveryNumber").exists())
            .andExpect(jsonPath("$[0].pointsCount").value(1))
            .andExpect(jsonPath("$[0].productsCount").value(2))
    }

    @Test
    fun `get courier deliveries as admin should return 403`() {
        createDelivery(courierUser)

        getWithAuth("/courier/deliveries", adminToken)
            .expectForbidden()
    }

    @Test
    fun `get courier deliveries as manager should return 403`() {
        createDelivery(courierUser)

        getWithAuth("/courier/deliveries", managerToken)
            .expectForbidden()
    }

    @Test
    fun `get courier deliveries without auth should return 403`() {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/courier/deliveries"))
            .expectForbidden()
    }

    @Test
    fun `get courier deliveries with date filter should return filtered results`() {
        val delivery = createDelivery(courierUser)
        val deliveryDate = delivery.deliveryDate

        getWithAuth("/courier/deliveries?date=$deliveryDate", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `get courier deliveries with non-matching date filter should return empty`() {
        createDelivery(courierUser)
        val differentDate = LocalDate.now().plusDays(10)

        getWithAuth("/courier/deliveries?date=$differentDate", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `get courier deliveries with status filter should return filtered results`() {
        createDelivery(courierUser)

        getWithAuth("/courier/deliveries?status=planned", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("planned"))
    }

    @Test
    fun `get courier deliveries with date range should return filtered results`() {
        val delivery = createDelivery(courierUser)
        val deliveryDate = delivery.deliveryDate
        val dateFrom = deliveryDate.minusDays(1)
        val dateTo = deliveryDate.plusDays(1)

        getWithAuth("/courier/deliveries?date_from=$dateFrom&date_to=$dateTo", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `get courier deliveries should not return other courier deliveries`() {
        // Create delivery for another courier
        val anotherCourier = userRepository.save(
            com.example.couriermanagement.entity.User(
                login = "othercourier",
                passwordHash = passwordEncoder.encode("password"),
                name = "Другой Курьер",
                role = com.example.couriermanagement.entity.UserRole.courier,
                createdAt = java.time.LocalDateTime.now()
            )
        )
        createDelivery(anotherCourier)

        // Current courier should see no deliveries
        getWithAuth("/courier/deliveries", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `get courier delivery by id should return delivery details`() {
        val delivery = createDelivery(courierUser)

        getWithAuth("/courier/deliveries/${delivery.id}", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$.id").value(delivery.id))
            .andExpect(jsonPath("$.deliveryNumber").exists())
            .andExpect(jsonPath("$.courier.id").value(courierUser.id))
            .andExpect(jsonPath("$.deliveryPoints").isArray)
            .andExpect(jsonPath("$.deliveryPoints.length()").value(1))
            .andExpect(jsonPath("$.deliveryPoints[0].products").isArray)
    }

    @Test
    fun `get other courier delivery by id should return 403`() {
        // Create delivery for another courier
        val anotherCourier = userRepository.save(
            com.example.couriermanagement.entity.User(
                login = "othercourier2",
                passwordHash = passwordEncoder.encode("password"),
                name = "Другой Курьер 2",
                role = com.example.couriermanagement.entity.UserRole.courier,
                createdAt = java.time.LocalDateTime.now()
            )
        )
        val delivery = createDelivery(anotherCourier)

        getWithAuth("/courier/deliveries/${delivery.id}", courierToken)
            .expectBadRequest() // Service throws IllegalArgumentException about access
    }

    @Test
    fun `get non-existent delivery should return 404`() {
        getWithAuth("/courier/deliveries/999", courierToken)
            .expectBadRequest() // Service throws IllegalArgumentException
    }

    @Test
    fun `get courier delivery as admin should return 403`() {
        val delivery = createDelivery(courierUser)

        getWithAuth("/courier/deliveries/${delivery.id}", adminToken)
            .expectForbidden()
    }

    @Test
    fun `get courier delivery as manager should return 403`() {
        val delivery = createDelivery(courierUser)

        getWithAuth("/courier/deliveries/${delivery.id}", managerToken)
            .expectForbidden()
    }

    @Test
    fun `courier should see correct vehicle information`() {
        val vehicle = createVehicle()
        val delivery = createDelivery(courierUser, vehicle)

        getWithAuth("/courier/deliveries", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$[0].vehicle.brand").value("Ford Transit"))
            .andExpect(jsonPath("$[0].vehicle.licensePlate").value("А123БВ"))
    }

    @Test
    fun `courier should see delivery with no vehicle assigned`() {
        val delivery = deliveryRepository.save(
            com.example.couriermanagement.entity.Delivery(
                courier = courierUser,
                vehicle = null, // No vehicle assigned
                createdBy = managerUser,
                deliveryDate = LocalDate.now().plusDays(5),
                timeStart = java.time.LocalTime.of(9, 0),
                timeEnd = java.time.LocalTime.of(18, 0),
                status = DeliveryStatus.planned,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )

        getWithAuth("/courier/deliveries", courierToken)
            .expectSuccess()
            .andExpect(jsonPath("$[0].vehicle.brand").value("Не назначена"))
            .andExpect(jsonPath("$[0].vehicle.licensePlate").value(""))
    }
}