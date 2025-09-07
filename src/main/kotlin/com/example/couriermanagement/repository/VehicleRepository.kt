package com.example.couriermanagement.repository

import com.example.couriermanagement.entity.Vehicle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface VehicleRepository : JpaRepository<Vehicle, Long> {
    
    fun findByLicensePlate(licensePlate: String): Vehicle?
    
    @Query("""
        SELECT v FROM Vehicle v 
        WHERE v.maxWeight >= :minWeight AND v.maxVolume >= :minVolume
    """)
    fun findByMinCapacity(
        @Param("minWeight") minWeight: BigDecimal,
        @Param("minVolume") minVolume: BigDecimal
    ): List<Vehicle>
    
    @Query("""
        SELECT v FROM Vehicle v 
        WHERE v.id NOT IN (
            SELECT d.vehicle.id FROM Delivery d 
            WHERE d.deliveryDate = :date AND d.vehicle IS NOT NULL
        )
    """)
    fun findAvailableVehiclesForDate(@Param("date") date: LocalDate): List<Vehicle>
    
    fun existsByLicensePlate(licensePlate: String): Boolean
}