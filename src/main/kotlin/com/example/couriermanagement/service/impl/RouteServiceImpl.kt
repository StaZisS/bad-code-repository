package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.request.RouteCalculationRequest
import com.example.couriermanagement.dto.response.RouteCalculationResponse
import com.example.couriermanagement.dto.response.SuggestedTime
import com.example.couriermanagement.service.RouteService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalTime
import kotlin.math.*
import kotlin.random.Random

@Service
class RouteServiceImpl : RouteService {
    
    override fun calculateRoute(request: RouteCalculationRequest): RouteCalculationResponse {
        // Simple stub implementation - calculate approximate distance and time
        
        if (request.points.size < 2) {
            throw IllegalArgumentException("Маршрут должен содержать минимум 2 точки")
        }
        
        // Calculate total distance using Haversine formula (approximate)
        var totalDistance = BigDecimal.ZERO
        
        for (i in 0 until request.points.size - 1) {
            val point1 = request.points[i]
            val point2 = request.points[i + 1]
            
            val distance = calculateDistance(
                point1.latitude.toDouble(),
                point1.longitude.toDouble(),
                point2.latitude.toDouble(),
                point2.longitude.toDouble()
            )
            
            totalDistance = totalDistance.add(BigDecimal.valueOf(distance))
        }
        
        // Approximate travel time based on average speed of 30 km/h in city
        val averageSpeedKmh = 30.0
        val durationHours = totalDistance.toDouble() / averageSpeedKmh
        val durationMinutes = (durationHours * 60).toInt()
        
        // Add some buffer time for stops and delays (20-30% extra)
        val bufferMultiplier = 1.0 + (Random.nextDouble(0.2, 0.3))
        val totalDurationMinutes = (durationMinutes * bufferMultiplier).toInt()
        
        // Suggest optimal time window
        val suggestedStart = LocalTime.of(9, 0) // Start at 9 AM
        val suggestedEnd = suggestedStart.plusMinutes(totalDurationMinutes.toLong())
        
        return RouteCalculationResponse(
            distanceKm = totalDistance.setScale(2, java.math.RoundingMode.HALF_UP),
            durationMinutes = totalDurationMinutes,
            suggestedTime = SuggestedTime(
                start = suggestedStart,
                end = suggestedEnd
            )
        )
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
}