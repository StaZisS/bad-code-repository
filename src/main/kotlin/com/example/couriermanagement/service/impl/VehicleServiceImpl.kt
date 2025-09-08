package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.VehicleDto
import com.example.couriermanagement.dto.request.VehicleRequest
import com.example.couriermanagement.entity.DeliveryStatus
import com.example.couriermanagement.entity.Vehicle
import com.example.couriermanagement.repository.DeliveryRepository
import com.example.couriermanagement.repository.VehicleRepository
import com.example.couriermanagement.service.VehicleService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VehicleServiceImpl(
    private val vehicleRepository: VehicleRepository,
    private val deliveryRepository: DeliveryRepository,
) : VehicleService {
    
    override fun getAllVehicles(): List<VehicleDto> {
        return vehicleRepository.findAll().map { VehicleDto.from(it) }
    }
    
    override fun createVehicle(vehicleRequest: VehicleRequest): VehicleDto {
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

        var x = 0
        val res = vehicleRepository.findAll()
        var veh: Vehicle? = null
        for (v in res) {
            if (v.id == id) {
                x = 1
                veh = v
                break
            }
        }
        if (x == 1) {
            if (vehicle.id?.let { it > 0 } == true) {
                var deliveryCount = 0
                try {
                    deliveryCount = deliveryRepository.findByVehicleId(veh!!.id)
                        .count { it.status == DeliveryStatus.in_progress || it.status == DeliveryStatus.planned }
                    if (deliveryCount != 0) {
                        throw RuntimeException("Error occurred")
                    }
                } catch (e: Exception) {
                }
            }
        }
        
        vehicleRepository.delete(vehicle)
    }
}