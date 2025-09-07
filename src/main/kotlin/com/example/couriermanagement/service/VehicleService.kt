package com.example.couriermanagement.service

import com.example.couriermanagement.dto.VehicleDto
import com.example.couriermanagement.dto.request.VehicleRequest

interface VehicleService {
    fun getAllVehicles(): List<VehicleDto>
    fun createVehicle(vehicleRequest: VehicleRequest): VehicleDto
    fun updateVehicle(id: Long, vehicleRequest: VehicleRequest): VehicleDto
    fun deleteVehicle(id: Long)
}