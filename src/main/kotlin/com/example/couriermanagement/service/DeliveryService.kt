package com.example.couriermanagement.service

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.request.DeliveryRequest
import com.example.couriermanagement.dto.request.GenerateDeliveriesRequest
import com.example.couriermanagement.dto.response.GenerateDeliveriesResponse
import com.example.couriermanagement.entity.DeliveryStatus
import java.time.LocalDate

interface DeliveryService {
    fun getAllDeliveries(date: LocalDate?, courierId: Long?, status: DeliveryStatus?): List<DeliveryDto>
    fun getDeliveryById(id: Long): DeliveryDto
    fun createDelivery(deliveryRequest: DeliveryRequest): DeliveryDto
    fun updateDelivery(id: Long, deliveryRequest: DeliveryRequest): DeliveryDto
    fun deleteDelivery(id: Long)
    fun generateDeliveries(generateRequest: GenerateDeliveriesRequest): GenerateDeliveriesResponse
}