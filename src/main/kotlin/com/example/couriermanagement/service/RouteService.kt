package com.example.couriermanagement.service

import com.example.couriermanagement.dto.request.RouteCalculationRequest
import com.example.couriermanagement.dto.response.RouteCalculationResponse

interface RouteService {
    fun calculateRoute(request: RouteCalculationRequest): RouteCalculationResponse
}