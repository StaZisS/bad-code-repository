package com.example.couriermanagement.service

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.response.CourierDeliveryResponse
import com.example.couriermanagement.entity.DeliveryStatus
import java.time.LocalDate

interface CourierService {
    fun getCourierDeliveries(
        date: LocalDate?,
        status: DeliveryStatus?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?
    ): List<CourierDeliveryResponse>
    
    fun getCourierDeliveryById(id: Long): DeliveryDto
}