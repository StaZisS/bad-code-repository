package com.example.couriermanagement.service

import java.math.BigDecimal

interface OpenStreetMapService {
    /**
     * Calculate distance between two points using OpenStreetMap routing
     */
    fun calculateDistance(
        startLatitude: BigDecimal,
        startLongitude: BigDecimal,
        endLatitude: BigDecimal,
        endLongitude: BigDecimal
    ): BigDecimal
}