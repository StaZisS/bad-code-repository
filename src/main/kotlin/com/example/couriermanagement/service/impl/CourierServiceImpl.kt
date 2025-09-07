package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.response.CourierDeliveryResponse
import com.example.couriermanagement.dto.response.VehicleInfo
import com.example.couriermanagement.entity.DeliveryStatus
import com.example.couriermanagement.repository.DeliveryRepository
import com.example.couriermanagement.service.AuthService
import com.example.couriermanagement.service.CourierService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CourierServiceImpl(
    private val deliveryRepository: DeliveryRepository,
    private val authService: AuthService
) : CourierService {
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    override fun getCourierDeliveries(
        date: LocalDate?,
        status: DeliveryStatus?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?
    ): List<CourierDeliveryResponse> {
        val currentUser = authService.getCurrentUser()
            ?: throw IllegalStateException("Пользователь не авторизован")
        
        val deliveries = when {
            date != null && status != null ->
                deliveryRepository.findByDeliveryDateAndCourierIdAndStatusWithDetails(date, currentUser.id, status)
            date != null ->
                deliveryRepository.findByDeliveryDateAndCourierIdWithDetails(date, currentUser.id)
            status != null && dateFrom != null && dateTo != null ->
                deliveryRepository.findByCourierIdAndStatusAndDeliveryDateBetweenWithDetails(currentUser.id, status, dateFrom, dateTo)
            status != null ->
                deliveryRepository.findByCourierIdAndStatusWithDetails(currentUser.id, status)
            dateFrom != null && dateTo != null ->
                deliveryRepository.findByCourierIdAndDeliveryDateBetweenWithDetails(currentUser.id, dateFrom, dateTo)
            else ->
                deliveryRepository.findByCourierIdWithDetails(currentUser.id)
        }

        val deliveryPointsWithProducts = if (deliveries.isNotEmpty()) {
            deliveryRepository.loadDeliveryPoint(deliveries).groupBy { it.delivery.id }
        } else {
            emptyMap()
        }
        
        return deliveries.map { delivery ->
            val pointsToUse = deliveryPointsWithProducts[delivery.id] ?: emptyList()
            
            // Calculate totals from all delivery point products
            val allProducts = if (pointsToUse.isNotEmpty()) {
                deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(pointsToUse)
            } else {
                emptyList()
            }
            val totalWeight = allProducts.sumOf { 
                it.product.weight * BigDecimal(it.quantity) 
            }
            val totalProductsCount = allProducts.sumOf { it.quantity }
            
            CourierDeliveryResponse(
                id = delivery.id,
                deliveryNumber = "DEL-${delivery.deliveryDate.year}-${delivery.id.toString().padStart(3, '0')}",
                deliveryDate = delivery.deliveryDate,
                timeStart = delivery.timeStart,
                timeEnd = delivery.timeEnd,
                status = delivery.status,
                vehicle = VehicleInfo(
                    brand = delivery.vehicle?.brand ?: "Не назначена",
                    licensePlate = delivery.vehicle?.licensePlate ?: ""
                ),
                pointsCount = pointsToUse.size,
                productsCount = totalProductsCount,
                totalWeight = totalWeight
            )
        }
    }
    
    override fun getCourierDeliveryById(id: Long): DeliveryDto {
        val currentUser = authService.getCurrentUser()
            ?: throw IllegalStateException("Пользователь не авторизован")
        
        var delivery = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")
        
        // Check if delivery belongs to current courier
        if (delivery.courier?.id != currentUser.id) {
            throw IllegalArgumentException("Доступ запрещен - это не ваша доставка")
        }

        var deliveryPoints = deliveryRepository.loadDeliveryPoint(listOf(delivery))

        if (deliveryPoints.isNotEmpty()) {
            val deliveryPointsProduct = deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(deliveryPoints)
                .groupBy { it.deliveryPoint.id }
            deliveryPoints = deliveryPoints.map {
                it.copy(
                    deliveryPointProducts = deliveryPointsProduct[it.id] ?: emptyList()
                )
            }
        }

        delivery = delivery.copy(
            deliveryPoints = deliveryPoints
        )
        
        return DeliveryDto.from(delivery)
    }
}