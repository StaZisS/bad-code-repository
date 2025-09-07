package com.example.couriermanagement.service.impl

import com.example.couriermanagement.service.OpenStreetMapService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class OpenStreetMapServiceImpl(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) : OpenStreetMapService {
    
    private val openRouteServiceUrl = "https://api.openrouteservice.org/v2/directions/driving-car"
    
    override fun calculateDistance(
        startLatitude: BigDecimal,
        startLongitude: BigDecimal,
        endLatitude: BigDecimal,
        endLongitude: BigDecimal
    ): BigDecimal {
        try {
            val url = "$openRouteServiceUrl?start=${startLongitude},${startLatitude}&end=${endLongitude},${endLatitude}"
            
            val response = restTemplate.getForObject(url, String::class.java)
                ?: throw RuntimeException("Failed to get response from OpenStreetMap")
            
            val jsonNode = objectMapper.readTree(response)
            val distance = extractDistanceFromResponse(jsonNode)
            
            // Convert meters to kilometers
            return distance.divide(BigDecimal(1000), 2, RoundingMode.HALF_UP)
            
        } catch (e: Exception) {
            // Fallback to Haversine formula if API fails
            return calculateHaversineDistance(startLatitude, startLongitude, endLatitude, endLongitude)
        }
    }
    
    private fun extractDistanceFromResponse(jsonNode: JsonNode): BigDecimal {
        val features = jsonNode.get("features")
        if (features != null && features.isArray && features.size() > 0) {
            val properties = features.get(0).get("properties")
            if (properties != null) {
                val summary = properties.get("summary")
                if (summary != null) {
                    val distance = summary.get("distance")
                    if (distance != null) {
                        return BigDecimal.valueOf(distance.asDouble())
                    }
                }
            }
        }
        throw RuntimeException("Unable to extract distance from OpenStreetMap response")
    }
    
    /**
     * Fallback calculation using Haversine formula
     */
    private fun calculateHaversineDistance(
        lat1: BigDecimal,
        lon1: BigDecimal,
        lat2: BigDecimal,
        lon2: BigDecimal
    ): BigDecimal {
        val earthRadius = 6371.0 // Earth radius in kilometers
        
        val dLat = Math.toRadians(lat2.toDouble() - lat1.toDouble())
        val dLon = Math.toRadians(lon2.toDouble() - lon1.toDouble())
        
        val a = kotlin.math.sin(dLat / 2).let { it * it } + 
                kotlin.math.cos(Math.toRadians(lat1.toDouble())) * 
                kotlin.math.cos(Math.toRadians(lat2.toDouble())) * 
                kotlin.math.sin(dLon / 2).let { it * it }
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return BigDecimal.valueOf(earthRadius * c).setScale(2, RoundingMode.HALF_UP)
    }
}