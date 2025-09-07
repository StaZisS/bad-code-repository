package com.example.couriermanagement.dto

import com.example.couriermanagement.entity.Vehicle
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class VehicleDto(
    val id: Long,
    val brand: String,
    val licensePlate: String,
    val maxWeight: BigDecimal,
    val maxVolume: BigDecimal
) {
    companion object {
        fun from(vehicle: Vehicle): VehicleDto {
            return VehicleDto(
                id = vehicle.id,
                brand = vehicle.brand,
                licensePlate = vehicle.licensePlate,
                maxWeight = vehicle.maxWeight,
                maxVolume = vehicle.maxVolume
            )
        }
    }
}

data class CreateVehicleRequest(
    @field:NotBlank(message = "Марка не может быть пустой")
    @field:Size(max = 100, message = "Марка не может быть длиннее 100 символов")
    val brand: String,

    @field:NotBlank(message = "Госномер не может быть пустым")
    @field:Size(max = 20, message = "Госномер не может быть длиннее 20 символов")
    val licensePlate: String,

    @field:NotNull(message = "Максимальный вес должен быть указан")
    @field:Positive(message = "Максимальный вес должен быть положительным")
    val maxWeight: BigDecimal,

    @field:NotNull(message = "Максимальный объем должен быть указан")
    @field:Positive(message = "Максимальный объем должен быть положительным")
    val maxVolume: BigDecimal
)

data class UpdateVehicleRequest(
    val brand: String?,
    val licensePlate: String?,
    val maxWeight: BigDecimal?,
    val maxVolume: BigDecimal?
)