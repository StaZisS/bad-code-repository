package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.request.DeliveryRequest
import com.example.couriermanagement.dto.request.GenerateDeliveriesRequest
import com.example.couriermanagement.dto.response.GenerateDeliveriesResponse
import com.example.couriermanagement.dto.response.GenerationResultByDate
import com.example.couriermanagement.entity.*
import com.example.couriermanagement.repository.*
import com.example.couriermanagement.service.AuthService
import com.example.couriermanagement.service.DeliveryService
import com.example.couriermanagement.service.OpenStreetMapService
import com.example.couriermanagement.service.RouteService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import jakarta.persistence.EntityManager

@Service
@Transactional
class DeliveryServiceImpl(
    private val deliveryRepository: DeliveryRepository,
    private val deliveryPointRepository: DeliveryPointRepository,
    private val deliveryPointProductRepository: DeliveryPointProductRepository,
    private val userRepository: UserRepository,
    private val vehicleRepository: VehicleRepository,
    private val productRepository: ProductRepository,
    private val authService: AuthService,
    private val routeService: RouteService,
    private val openStreetMapService: OpenStreetMapService,
    private val entityManager: EntityManager,
) : DeliveryService {
    
    override fun getAllDeliveries(date: LocalDate?, courierId: Long?, status: DeliveryStatus?): List<DeliveryDto> {
        var deliveries = when {
            date != null && courierId != null && status != null -> 
                deliveryRepository.findByDeliveryDateAndCourierIdAndStatus(date, courierId, status)
            date != null && courierId != null -> 
                deliveryRepository.findByDeliveryDateAndCourierId(date, courierId)
            date != null && status != null -> 
                deliveryRepository.findByDeliveryDateAndStatus(date, status)
            date != null -> 
                deliveryRepository.findByDeliveryDate(date)
            courierId != null && status != null -> 
                deliveryRepository.findByCourierIdAndStatus(courierId, status)
            courierId != null -> 
                deliveryRepository.findByCourierId(courierId)
            status != null -> 
                deliveryRepository.findByStatus(status)
            else -> 
                deliveryRepository.findAll()
        }

        val deliveryPoints = deliveryRepository.loadDeliveryPoint(deliveries).groupBy { it.delivery.id }

        if (deliveryPoints.isNotEmpty()) {
            val deliveryPointsProduct = deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(deliveryPoints.values.flatten())
                .groupBy { it.deliveryPoint.id }
            deliveries = deliveries.map { delivery ->
                val points = deliveryPoints[delivery.id].orEmpty()
                delivery.copy(
                    deliveryPoints = points.map { deliveryPoint ->
                        deliveryPoint.copy(
                            deliveryPointProducts = deliveryPointsProduct[deliveryPoint.id].orEmpty(),
                        )
                    },
                )
            }
        }
        
        return deliveries.map {
            DeliveryDto.from(
                it
            )
        }
    }
    
    override fun getDeliveryById(id: Long): DeliveryDto {
        var delivery = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")

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
    
    override fun createDelivery(deliveryRequest: DeliveryRequest): DeliveryDto {
        validateDeliveryRequest(deliveryRequest)
        
        val currentUser = authService.getCurrentUser()
            ?: throw IllegalStateException("Пользователь не авторизован")
        
        val createdBy = userRepository.findByLogin(currentUser.login)
            ?: throw IllegalStateException("Пользователь не найден")
        
        val courier = userRepository.findByIdOrNull(deliveryRequest.courierId)
            ?: throw IllegalArgumentException("Курьер не найден")
        
        val vehicle = vehicleRepository.findByIdOrNull(deliveryRequest.vehicleId)
            ?: throw IllegalArgumentException("Машина не найдена")
        
        // Validate courier role
        if (courier.role != UserRole.courier) {
            throw IllegalArgumentException("Пользователь не является курьером")
        }
        
        val delivery = Delivery(
            courier = courier,
            vehicle = vehicle,
            createdBy = createdBy,
            deliveryDate = deliveryRequest.deliveryDate,
            timeStart = deliveryRequest.timeStart,
            timeEnd = deliveryRequest.timeEnd,
            status = DeliveryStatus.planned,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val savedDelivery = deliveryRepository.save(delivery)
        
        // Create delivery points with products
        createDeliveryPointsWithProducts(savedDelivery, deliveryRequest)
        
        return getDeliveryById(savedDelivery.id)
    }
    
    override fun updateDelivery(id: Long, deliveryRequest: DeliveryRequest): DeliveryDto {
        val delivery = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")
        
        // Check if delivery can be edited (more than 3 days before delivery date)
        val daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), delivery.deliveryDate)
        if (daysBefore < 3) {
            throw IllegalArgumentException("Нельзя редактировать доставку менее чем за 3 дня до даты доставки")
        }
        
        validateDeliveryRequest(deliveryRequest)
        
        val courier = userRepository.findByIdOrNull(deliveryRequest.courierId)
            ?: throw IllegalArgumentException("Курьер не найден")
        
        val vehicle = vehicleRepository.findByIdOrNull(deliveryRequest.vehicleId)
            ?: throw IllegalArgumentException("Машина не найдена")
        
        if (courier.role != UserRole.courier) {
            throw IllegalArgumentException("Пользователь не является курьером")
        }
        
        val updatedDelivery = delivery.copy(
            courier = courier,
            vehicle = vehicle,
            deliveryDate = deliveryRequest.deliveryDate,
            timeStart = deliveryRequest.timeStart,
            timeEnd = deliveryRequest.timeEnd,
            updatedAt = LocalDateTime.now()
        )
        
        val savedDelivery = deliveryRepository.save(updatedDelivery)
        
        // Remove old points and products, create new ones
        deliveryPointRepository.findByDeliveryId(delivery.id).forEach { point ->
            deliveryPointProductRepository.deleteByDeliveryPointId(point.id)
        }
        deliveryPointRepository.deleteByDeliveryId(delivery.id)
        
        // Flush deletions to avoid unique constraint violations
        entityManager.flush()
        
        createDeliveryPointsWithProducts(savedDelivery, deliveryRequest)
        
        return getDeliveryById(savedDelivery.id)
    }
    
    override fun deleteDelivery(id: Long) {
        val delivery = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")
        
        // Check if delivery can be deleted (more than 3 days before delivery date)
        val daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), delivery.deliveryDate)
        if (daysBefore < 3) {
            throw IllegalArgumentException("Нельзя удалить доставку менее чем за 3 дня до даты доставки")
        }
        
        deliveryRepository.delete(delivery)
    }
    
    override fun generateDeliveries(generateRequest: GenerateDeliveriesRequest): GenerateDeliveriesResponse {
        // Simple stub implementation for delivery generation
        val currentUser = authService.getCurrentUser()
            ?: throw IllegalStateException("Пользователь не авторизован")
        
        val createdBy = userRepository.findByLogin(currentUser.login)
            ?: throw IllegalStateException("Пользователь не найден")
        
        val resultByDate = mutableMapOf<LocalDate, GenerationResultByDate>()
        var totalGenerated = 0
        
        for ((date, routes) in generateRequest.deliveryData) {
            val generatedDeliveries = mutableListOf<DeliveryDto>()
            val warnings = mutableListOf<String>()
            
            // Get available couriers and vehicles
            val availableCouriers = userRepository.findByRole(UserRole.courier)
            val availableVehicles = vehicleRepository.findAll()
            
            if (availableCouriers.isEmpty()) {
                warnings.add("Нет доступных курьеров")
            }
            if (availableVehicles.isEmpty()) {
                warnings.add("Нет доступных машин")
            }
            
            routes.forEachIndexed { index, route ->
                if (index < availableCouriers.size && index < availableVehicles.size) {
                    try {
                        val courier = availableCouriers[index % availableCouriers.size]
                        val vehicle = availableVehicles[index % availableVehicles.size]
                        
                        // Create a temporary delivery request to validate vehicle capacity
                        val tempDeliveryRequest = com.example.couriermanagement.dto.request.DeliveryRequest(
                            courierId = courier.id,
                            vehicleId = vehicle.id,
                            deliveryDate = date,
                            timeStart = java.time.LocalTime.of(9, 0).plusHours(index.toLong()),
                            timeEnd = java.time.LocalTime.of(18, 0),
                            points = route.route.map { pointRequest ->
                                com.example.couriermanagement.dto.request.DeliveryPointRequest(
                                    sequence = null,
                                    latitude = pointRequest.latitude,
                                    longitude = pointRequest.longitude,
                                    products = route.products.map { productRequest ->
                                        com.example.couriermanagement.dto.request.DeliveryProductRequest(
                                            productId = productRequest.productId,
                                            quantity = productRequest.quantity
                                        )
                                    }
                                )
                            }
                        )
                        
                        // Validate vehicle capacity before creating delivery
                        validateVehicleCapacity(tempDeliveryRequest)
                        
                        val delivery = Delivery(
                            courier = courier,
                            vehicle = vehicle,
                            createdBy = createdBy,
                            deliveryDate = date,
                            timeStart = java.time.LocalTime.of(9, 0).plusHours(index.toLong()),
                            timeEnd = java.time.LocalTime.of(18, 0),
                            status = DeliveryStatus.planned,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                        
                        val savedDelivery = deliveryRepository.save(delivery)
                        
                        // Create points with products
                        route.route.forEachIndexed { pointIndex, pointRequest ->
                            val deliveryPoint = DeliveryPoint(
                                delivery = savedDelivery,
                                sequence = pointIndex + 1,
                                latitude = pointRequest.latitude,
                                longitude = pointRequest.longitude
                            )
                            
                            val savedPoint = deliveryPointRepository.save(deliveryPoint)
                            
                            // Add products to this point
                            route.products.forEach { productRequest ->
                                val product = productRepository.findByIdOrNull(productRequest.productId)
                                if (product != null) {
                                    val deliveryPointProduct = DeliveryPointProduct(
                                        deliveryPoint = savedPoint,
                                        product = product,
                                        quantity = productRequest.quantity
                                    )
                                    deliveryPointProductRepository.save(deliveryPointProduct)
                                }
                            }
                        }
                        
                        generatedDeliveries.add(DeliveryDto.from(getDeliveryById(savedDelivery.id).let {
                            deliveryRepository.findByIdOrNull(savedDelivery.id)!!
                        }))
                        totalGenerated++
                    } catch (e: IllegalArgumentException) {
                        warnings.add("Доставка пропущена из-за ограничений машины: ${e.message}")
                    } catch (e: Exception) {
                        warnings.add("Ошибка при создании доставки: ${e.message}")
                    }
                } else {
                    warnings.add("Недостаточно ресурсов для создания всех доставок")
                }
            }
            
            resultByDate[date] = GenerationResultByDate(
                generatedCount = generatedDeliveries.size,
                deliveries = generatedDeliveries,
                warnings = warnings.ifEmpty { null }
            )
        }
        
        return GenerateDeliveriesResponse(
            totalGenerated = totalGenerated,
            byDate = resultByDate
        )
    }
    
    private fun validateDeliveryRequest(deliveryRequest: DeliveryRequest) {
        if (deliveryRequest.timeStart >= deliveryRequest.timeEnd) {
            throw IllegalArgumentException("Время начала должно быть раньше времени окончания")
        }
        
        if (deliveryRequest.deliveryDate.isBefore(LocalDate.now())) {
            throw IllegalArgumentException("Дата доставки не может быть в прошлом")
        }
        
        // Vehicle capacity validation - check if vehicle can handle the load with existing deliveries
        validateVehicleCapacity(deliveryRequest)
        
        // Route time validation - check if the provided time window is sufficient for the route
        if (deliveryRequest.points.size >= 2) {
            validateRouteTime(deliveryRequest)
        }
    }
    
    private fun validateVehicleCapacity(deliveryRequest: DeliveryRequest) {
        val vehicle = vehicleRepository.findByIdOrNull(deliveryRequest.vehicleId)
            ?: throw IllegalArgumentException("Машина не найдена")
        
        // Calculate total weight and volume for the new delivery
        var totalWeight = BigDecimal.ZERO
        var totalVolume = BigDecimal.ZERO
        
        deliveryRequest.points.forEach { point ->
            point.products.forEach { productRequest ->
                val product = productRepository.findByIdOrNull(productRequest.productId)
                    ?: throw IllegalArgumentException("Товар с ID ${productRequest.productId} не найден")
                
                val quantity = BigDecimal(productRequest.quantity)
                totalWeight = totalWeight.add(product.weight.multiply(quantity))
                totalVolume = totalVolume.add(product.getVolume().multiply(quantity))
            }
        }
        
        // Find existing deliveries for this vehicle on the same date with overlapping time periods
        // (excluding cancelled and completed deliveries)
        val existingDeliveries = deliveryRepository.findByDateVehicleAndOverlappingTime(
            deliveryRequest.deliveryDate, 
            deliveryRequest.vehicleId,
            deliveryRequest.timeStart,
            deliveryRequest.timeEnd
        )
        
        // Calculate weight and volume from existing deliveries
        var existingWeight = BigDecimal.ZERO
        var existingVolume = BigDecimal.ZERO
        
        if (existingDeliveries.isNotEmpty()) {
            val deliveryPoints = deliveryRepository.loadDeliveryPoint(existingDeliveries)
            if (deliveryPoints.isNotEmpty()) {
                val products = deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(deliveryPoints)
                products.forEach { dpp ->
                    val quantity = BigDecimal(dpp.quantity)
                    existingWeight = existingWeight.add(dpp.product.weight.multiply(quantity))
                    existingVolume = existingVolume.add(dpp.product.getVolume().multiply(quantity))
                }
            }
        }
        
        // Check total capacity
        val totalRequiredWeight = existingWeight.add(totalWeight)
        val totalRequiredVolume = existingVolume.add(totalVolume)
        
        if (totalRequiredWeight > vehicle.maxWeight) {
            throw IllegalArgumentException(
                "Превышена грузоподъемность машины в период ${deliveryRequest.timeStart}-${deliveryRequest.timeEnd}. " +
                "Максимум: ${vehicle.maxWeight} кг, " +
                "требуется: ${totalRequiredWeight} кг " +
                "(пересекающиеся доставки: ${existingWeight} кг, новые: ${totalWeight} кг)"
            )
        }
        
        if (totalRequiredVolume > vehicle.maxVolume) {
            throw IllegalArgumentException(
                "Превышен объем машины в период ${deliveryRequest.timeStart}-${deliveryRequest.timeEnd}. " +
                "Максимум: ${vehicle.maxVolume} м³, " +
                "требуется: ${totalRequiredVolume} м³ " +
                "(пересекающиеся доставки: ${existingVolume} м³, новые: ${totalVolume} м³)"
            )
        }
    }
    
    private fun validateRouteTime(deliveryRequest: DeliveryRequest) {
        val firstPoint = deliveryRequest.points.first()
        val lastPoint = deliveryRequest.points.last()
        
        // Calculate distance between first and last point using OpenStreetMap
        val distanceKm = openStreetMapService.calculateDistance(
            startLatitude = firstPoint.latitude,
            startLongitude = firstPoint.longitude,
            endLatitude = lastPoint.latitude,
            endLongitude = lastPoint.longitude
        )
        
        // Calculate required time for courier at 60 km/h speed
        val speedKmPerHour = BigDecimal("60")
        val requiredHours = distanceKm.divide(speedKmPerHour, 4, java.math.RoundingMode.HALF_UP)
        
        // Add buffer time for stops and handling deliveries (30 minutes per delivery point)
        val bufferMinutesPerPoint = 30
        val totalBufferMinutes = deliveryRequest.points.size * bufferMinutesPerPoint
        val totalRequiredMinutes = (requiredHours.toDouble() * 60).toLong() + totalBufferMinutes
        
        // Calculate available time window
        val timeStart = deliveryRequest.timeStart
        val timeEnd = deliveryRequest.timeEnd
        val availableMinutes = java.time.Duration.between(timeStart, timeEnd).toMinutes()
        
        if (totalRequiredMinutes > availableMinutes) {
            throw IllegalArgumentException(
                "Недостаточно времени для выполнения маршрута. " +
                "Требуется: ${totalRequiredMinutes} мин (${String.format("%.1f", totalRequiredMinutes/60.0)} ч), " +
                "доступно: ${availableMinutes} мин (${String.format("%.1f", availableMinutes/60.0)} ч). " +
                "Расстояние: ${distanceKm} км"
            )
        }
    }
    
    private fun createDeliveryPointsWithProducts(delivery: Delivery, deliveryRequest: DeliveryRequest) {
        deliveryRequest.points.forEachIndexed { index, pointRequest ->
            val deliveryPoint = DeliveryPoint(
                delivery = delivery,
                sequence = pointRequest.sequence ?: (index + 1),
                latitude = pointRequest.latitude,
                longitude = pointRequest.longitude
            )
            
            val savedPoint = deliveryPointRepository.save(deliveryPoint)
            
            // Create products for this point
            pointRequest.products.forEach { productRequest ->
                val product = productRepository.findByIdOrNull(productRequest.productId)
                    ?: throw IllegalArgumentException("Товар с ID ${productRequest.productId} не найден")
                
                val deliveryPointProduct = DeliveryPointProduct(
                    deliveryPoint = savedPoint,
                    product = product,
                    quantity = productRequest.quantity
                )
                
                deliveryPointProductRepository.save(deliveryPointProduct)
            }
        }
    }
}