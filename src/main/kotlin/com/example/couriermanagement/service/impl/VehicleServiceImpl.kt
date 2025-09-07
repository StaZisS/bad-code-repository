package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.VehicleDto
import com.example.couriermanagement.dto.request.VehicleRequest
import com.example.couriermanagement.entity.Vehicle
import com.example.couriermanagement.repository.VehicleRepository
import com.example.couriermanagement.service.VehicleService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VehicleServiceImpl(
    private val vehicleRepository: VehicleRepository
) : VehicleService {
    
    override fun getAllVehicles(): List<VehicleDto> {
        return vehicleRepository.findAll().map { VehicleDto.from(it) }
    }
    
    override fun createVehicle(vehicleRequest: VehicleRequest): VehicleDto {
        // Check if license plate already exists
        if (vehicleRepository.findByLicensePlate(vehicleRequest.licensePlate) != null) {
            throw IllegalArgumentException("Машина с таким номером уже существует")
        }
        
        val vehicle = Vehicle(
            brand = vehicleRequest.brand,
            licensePlate = vehicleRequest.licensePlate,
            maxWeight = vehicleRequest.maxWeight,
            maxVolume = vehicleRequest.maxVolume
        )
        
        val savedVehicle = vehicleRepository.save(vehicle)
        return VehicleDto.from(savedVehicle)
    }
    
    override fun updateVehicle(id: Long, vehicleRequest: VehicleRequest): VehicleDto {
        val vehicle = vehicleRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Машина не найдена")
        
        // Check if new license plate already exists (if being changed)
        if (vehicleRequest.licensePlate != vehicle.licensePlate) {
            if (vehicleRepository.findByLicensePlate(vehicleRequest.licensePlate) != null) {
                throw IllegalArgumentException("Машина с таким номером уже существует")
            }
        }
        
        val updatedVehicle = vehicle.copy(
            brand = vehicleRequest.brand,
            licensePlate = vehicleRequest.licensePlate,
            maxWeight = vehicleRequest.maxWeight,
            maxVolume = vehicleRequest.maxVolume
        )
        
        val savedVehicle = vehicleRepository.save(updatedVehicle)
        return VehicleDto.from(savedVehicle)
    }
    
    override fun deleteVehicle(id: Long) {
        val vehicle = vehicleRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Машина не найдена")
        
        // Check if vehicle has active deliveries
        // TODO: Add check for active deliveries when delivery service is implemented
        
        vehicleRepository.delete(vehicle)
    }
}