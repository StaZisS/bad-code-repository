package com.example.couriermanagement.controller

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.VehicleRequest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

class VehicleControllerTest : BaseIntegrationTest() {

    @Test
    fun `get all vehicles should return list of vehicles`() {
        createVehicle()

        getWithAuth("/vehicles", adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].brand").value("Ford Transit"))
            .andExpect(jsonPath("$[0].licensePlate").value("А123БВ"))
    }

    @Test
    fun `get all vehicles without auth should return 403`() {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/vehicles"))
            .expectForbidden()
    }

    @Test
    fun `create vehicle as admin should succeed`() {
        val vehicleRequest = VehicleRequest(
            brand = "Mercedes Sprinter",
            licensePlate = "В456ГД",
            maxWeight = BigDecimal("1500.0"),
            maxVolume = BigDecimal("20.0")
        )

        postJson("/vehicles", vehicleRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.brand").value("Mercedes Sprinter"))
            .andExpect(jsonPath("$.licensePlate").value("В456ГД"))
            .andExpect(jsonPath("$.maxWeight").value(1500.0))
            .andExpect(jsonPath("$.maxVolume").value(20.0))
    }

    @Test
    fun `create vehicle as manager should return 403`() {
        val vehicleRequest = VehicleRequest(
            brand = "Mercedes Sprinter",
            licensePlate = "В456ГД",
            maxWeight = BigDecimal("1500.0"),
            maxVolume = BigDecimal("20.0")
        )

        postJson("/vehicles", vehicleRequest, managerToken)
            .expectForbidden()
    }

    @Test
    fun `create vehicle as courier should return 403`() {
        val vehicleRequest = VehicleRequest(
            brand = "Mercedes Sprinter",
            licensePlate = "В456ГД",
            maxWeight = BigDecimal("1500.0"),
            maxVolume = BigDecimal("20.0")
        )

        postJson("/vehicles", vehicleRequest, courierToken)
            .expectForbidden()
    }

    @Test
    fun `create vehicle with duplicate license plate should return 400`() {
        createVehicle() // Creates vehicle with А123БВ

        val vehicleRequest = VehicleRequest(
            brand = "Mercedes Sprinter",
            licensePlate = "А123БВ", // Same license plate
            maxWeight = BigDecimal("1500.0"),
            maxVolume = BigDecimal("20.0")
        )

        postJson("/vehicles", vehicleRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `create vehicle with invalid data should return 400`() {
        val vehicleRequest = VehicleRequest(
            brand = "",
            licensePlate = "",
            maxWeight = BigDecimal("-100.0"), // Negative weight
            maxVolume = BigDecimal("-10.0")   // Negative volume
        )

        postJson("/vehicles", vehicleRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `update vehicle as admin should succeed`() {
        val vehicle = createVehicle()

        val vehicleRequest = VehicleRequest(
            brand = "Updated Ford",
            licensePlate = "Г789ЕЖ",
            maxWeight = BigDecimal("2000.0"),
            maxVolume = BigDecimal("25.0")
        )

        putJson("/vehicles/${vehicle.id}", vehicleRequest, adminToken)
            .expectSuccess()
            .andExpect(jsonPath("$.brand").value("Updated Ford"))
            .andExpect(jsonPath("$.licensePlate").value("Г789ЕЖ"))
    }

    @Test
    fun `update vehicle as manager should return 403`() {
        val vehicle = createVehicle()

        val vehicleRequest = VehicleRequest(
            brand = "Updated Ford",
            licensePlate = "Г789ЕЖ",
            maxWeight = BigDecimal("2000.0"),
            maxVolume = BigDecimal("25.0")
        )

        putJson("/vehicles/${vehicle.id}", vehicleRequest, managerToken)
            .expectForbidden()
    }

    @Test
    fun `update non-existent vehicle should return 404`() {
        val vehicleRequest = VehicleRequest(
            brand = "Updated Ford",
            licensePlate = "Г789ЕЖ",
            maxWeight = BigDecimal("2000.0"),
            maxVolume = BigDecimal("25.0")
        )

        putJson("/vehicles/999", vehicleRequest, adminToken)
            .expectBadRequest() // Service throws IllegalArgumentException, which becomes 400
    }

    @Test
    fun `update vehicle with duplicate license plate should return 400`() {
        val vehicle1 = createVehicle()
        val vehicle2 = vehicleRepository.save(
            com.example.couriermanagement.entity.Vehicle(
                brand = "Mercedes",
                licensePlate = "В456ГД",
                maxWeight = BigDecimal("1500.0"),
                maxVolume = BigDecimal("20.0")
            )
        )

        val vehicleRequest = VehicleRequest(
            brand = "Updated Mercedes",
            licensePlate = "А123БВ", // Same as vehicle1
            maxWeight = BigDecimal("2000.0"),
            maxVolume = BigDecimal("25.0")
        )

        putJson("/vehicles/${vehicle2.id}", vehicleRequest, adminToken)
            .expectBadRequest()
    }

    @Test
    fun `delete vehicle as admin should succeed`() {
        val vehicle = createVehicle()

        deleteWithAuth("/vehicles/${vehicle.id}", adminToken)
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete vehicle as manager should return 403`() {
        val vehicle = createVehicle()

        deleteWithAuth("/vehicles/${vehicle.id}", managerToken)
            .expectForbidden()
    }

    @Test
    fun `delete non-existent vehicle should return 404`() {
        deleteWithAuth("/vehicles/999", adminToken)
            .expectBadRequest() // Service throws IllegalArgumentException, which becomes 400
    }
}