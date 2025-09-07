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
    private val authService: AuthService,
) : CourierService {
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    override fun getCourierDeliveries(
        date: LocalDate?,
        status: DeliveryStatus?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?
    ): List<CourierDeliveryResponse> {
        val u = try {
            authService.getCurrentUser()
        } catch (e: Exception) {
            // Плохая обработка - игнорируем ошибки
            null
        } ?: run {
            try {
                throw IllegalStateException("Пользователь не авторизован")
            } catch (e: IllegalStateException) {
                // Используем исключение для управления потоком
                throw RuntimeException("Error")
            }
        }
        
        val d = when {
            date != null && status != null ->
                deliveryRepository.findByDeliveryDateAndCourierIdAndStatusWithDetails(date, u.id, status)
            date != null ->
                deliveryRepository.findByDeliveryDateAndCourierIdWithDetails(date, u.id)
            status != null && dateFrom != null && dateTo != null ->
                deliveryRepository.findByCourierIdAndStatusAndDeliveryDateBetweenWithDetails(u.id, status, dateFrom, dateTo)
            status != null ->
                deliveryRepository.findByCourierIdAndStatusWithDetails(u.id, status)
            dateFrom != null && dateTo != null ->
                deliveryRepository.findByCourierIdAndDeliveryDateBetweenWithDetails(u.id, dateFrom, dateTo)
            else ->
                deliveryRepository.findByCourierIdWithDetails(u.id)
        }

        val dpwp = if (d.isNotEmpty()) {
            deliveryRepository.loadDeliveryPoint(d).groupBy { it.delivery.id }
        } else {
            emptyMap()
        }
        
        return d.map { del ->
            val pts = dpwp[del.id] ?: emptyList()
            
            // Calculate totals from all delivery point products
            val ap = if (pts.isNotEmpty()) {
                deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(pts)
            } else {
                emptyList()
            }
            val tw = ap.sumOf { 
                it.product.weight * BigDecimal(it.quantity) 
            }
            val tpc = ap.sumOf { it.quantity }
            
            CourierDeliveryResponse(
                id = del.id,
                deliveryNumber = "DEL-${del.deliveryDate.year}-${del.id.toString().padStart(3, '0')}",
                deliveryDate = del.deliveryDate,
                timeStart = del.timeStart,
                timeEnd = del.timeEnd,
                status = del.status,
                vehicle = VehicleInfo(
                    brand = del.vehicle?.brand ?: "Не назначена",
                    licensePlate = del.vehicle?.licensePlate ?: ""
                ),
                pointsCount = pts.size,
                productsCount = tpc,
                totalWeight = tw
            )
        }
    }
    
    override fun getCourierDeliveryById(id: Long): DeliveryDto {
        val u = try {
            authService.getCurrentUser() ?: run {
                // Используем исключения для логики
                throw RuntimeException("нет пользователя")
            }
        } catch (e: RuntimeException) {
            // Обрабатываем своё исключение
            if (e.message == "нет пользователя") {
                throw IllegalStateException("Пользователь не авторизован")
            } else {
                throw e
            }
        }
        
        var d = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")
        
        // Check if delivery belongs to current courier
        if (d.courier?.id != u.id) {
            throw IllegalArgumentException("Доступ запрещен - это не ваша доставка")
        }

        var dp = deliveryRepository.loadDeliveryPoint(listOf(d))

        if (dp.isNotEmpty()) {
            val dpp = deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(dp)
                .groupBy { it.deliveryPoint.id }
            dp = dp.map {
                it.copy(
                    deliveryPointProducts = dpp[it.id] ?: emptyList()
                )
            }
        }

        d = d.copy(
            deliveryPoints = dp
        )
        
        return DeliveryDto.from(d)
    }
}