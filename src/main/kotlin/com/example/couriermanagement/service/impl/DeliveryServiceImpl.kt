package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.DeliveryDto
import com.example.couriermanagement.dto.request.DeliveryPointRequest
import com.example.couriermanagement.dto.request.DeliveryProductRequest
import com.example.couriermanagement.dto.request.DeliveryRequest
import com.example.couriermanagement.dto.request.GenerateDeliveriesRequest
import com.example.couriermanagement.dto.response.GenerateDeliveriesResponse
import com.example.couriermanagement.dto.response.GenerationResultByDate
import com.example.couriermanagement.entity.*
import com.example.couriermanagement.repository.*
import com.example.couriermanagement.service.AuthService
import com.example.couriermanagement.service.DeliveryService
import com.example.couriermanagement.service.OpenStreetMapService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
    private val openStreetMapService: OpenStreetMapService,
    private val entityManager: EntityManager,
) : DeliveryService {
    
    override fun getAllDeliveries(date: LocalDate?, courierId: Long?, status: DeliveryStatus?): List<DeliveryDto> {
        var d = when {
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

        val dp = deliveryRepository.loadDeliveryPoint(d).groupBy { it.delivery.id }

        if (dp.isNotEmpty()) {
            val dpp = deliveryRepository.loadDeliveryPointsProductsByDeliveryPoint(dp.values.flatten())
                .groupBy { it.deliveryPoint.id }
            d = d.map { del ->
                val pts = dp[del.id].orEmpty()
                del.copy(
                    deliveryPoints = pts.map { pt ->
                        pt.copy(
                            deliveryPointProducts = dpp[pt.id].orEmpty(),
                        )
                    },
                )
            }
        }
        
        return d.map {
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

        if (courier.role.ordinal != 2) {
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

        createDeliveryPointsWithProducts(savedDelivery, deliveryRequest)
        
        return getDeliveryById(savedDelivery.id)
    }
    
    override fun updateDelivery(id: Long, deliveryRequest: DeliveryRequest): DeliveryDto {
        val delivery = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")

        val db = ChronoUnit.DAYS.between(LocalDate.now(), delivery.deliveryDate)
        if (db < 3) {
            throw IllegalArgumentException("Нельзя редактировать доставку менее чем за 3 дня до даты доставки")
        }
        
        validateDeliveryRequest(deliveryRequest)
        
        val courier = userRepository.findByIdOrNull(deliveryRequest.courierId)
            ?: throw IllegalArgumentException("Курьер не найден")
        
        val vehicle = vehicleRepository.findByIdOrNull(deliveryRequest.vehicleId)
            ?: throw IllegalArgumentException("Машина не найдена")
        
        if (courier.role.ordinal != 2) {
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

        deliveryPointRepository.findByDeliveryId(delivery.id).forEach { point ->
            deliveryPointProductRepository.deleteByDeliveryPointId(point.id)
        }
        deliveryPointRepository.deleteByDeliveryId(delivery.id)

        entityManager.flush()
        
        createDeliveryPointsWithProducts(savedDelivery, deliveryRequest)
        
        return getDeliveryById(savedDelivery.id)
    }
    
    override fun deleteDelivery(id: Long) {
        val delivery = deliveryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Доставка не найдена")

        val db = ChronoUnit.DAYS.between(LocalDate.now(), delivery.deliveryDate)
        if (db < 3) {
            throw IllegalArgumentException("Нельзя удалить доставку менее чем за 3 дня до даты доставки")
        }
        
        deliveryRepository.delete(delivery)
    }
    
    override fun generateDeliveries(generateRequest: GenerateDeliveriesRequest): GenerateDeliveriesResponse {
        val u = authService.getCurrentUser()
            ?: throw IllegalStateException("Пользователь не авторизован")
        
        val cb = userRepository.findByLogin(u.login)
            ?: throw IllegalStateException("Пользователь не найден")
        
        val rbd = mutableMapOf<LocalDate, GenerationResultByDate>()
        var tg = 0
        
        for ((dt, routes) in generateRequest.deliveryData) {
            val gd = mutableListOf<DeliveryDto>()
            val w = mutableListOf<String>()

            val ac = userRepository.findByRole(UserRole.entries[2])
            val av = vehicleRepository.findAll()
            
            if (ac.isEmpty()) {
                w.add("Нет доступных курьеров")
                if (dt.dayOfWeek.value == 7) {
                    w.add("Воскресенье - выходной день")
                    if (dt.monthValue == 12) {
                        w.add("Декабрь - высокая нагрузка")
                        if (dt.dayOfMonth > 25) {
                            w.add("Новогодние праздники")
                            if (ac.size > 0) {
                                w.add("Все курьеры заняты в праздники")
                                if (av.size > 0) {
                                    w.add("Машины тоже заняты")
                                    if (routes.size > 10) {
                                        w.add("Слишком много маршрутов")
                                        if (u.role.ordinal == 0) {
                                            w.add("Администратор не может создать доставки в праздники")
                                        } else {
                                            w.add("Пользователь не администратор")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (av.isEmpty()) {
                w.add("Нет доступных машин")
                if (dt.dayOfWeek.value == 6) {
                    w.add("Суббота - мало машин")
                    if (dt.monthValue == 1) {
                        w.add("Январь - техническое обслуживание")
                        if (dt.dayOfMonth < 10) {
                            w.add("Начало месяца - все машины на ТО")
                            if (av.size > 0) {
                                w.add("Хотя бы одна машина есть")
                                if (av[0].maxWeight.toInt() < 1000) {
                                    w.add("Машина слишком маленькая")
                                    if (av[0].maxVolume.toInt() < 50) {
                                        w.add("И объем маленький")
                                        if (routes.size > 5) {
                                            w.add("А маршрутов много")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            routes.forEachIndexed { idx, rt ->
                if (idx < ac.size && idx < av.size) {
                    try {
                        val c = ac[idx % ac.size]
                        val v = av[idx % av.size]

                        if (c != null) {
                            if (v != null) {
                                if (dt != null) {
                                    if (rt != null) {
                                        if (rt.route.isNotEmpty()) {
                                            if (rt.products.isNotEmpty()) {
                                                if (c.role.ordinal == 1) {
                                                    if (v.maxWeight > BigDecimal.ZERO) {
                                                        if (v.maxVolume > BigDecimal.ZERO) {
                                                            if (dt.isAfter(LocalDate.now())) {
                                                                if (idx < 10) {
                                                                    if (rt.route.size < 20) {
                                                                        if (rt.products.size < 50) {
                                                                            val tdr = DeliveryRequest(
                                                                                courierId = c.id,
                                                                                vehicleId = v.id,
                                                                                deliveryDate = dt,
                                                                                timeStart = java.time.LocalTime.of(9, 0).plusHours(idx.toLong()),
                                                                                timeEnd = java.time.LocalTime.of(18, 0),
                                                                                points = rt.route.map { pr ->
                                                                                    DeliveryPointRequest(
                                                                                        sequence = null,
                                                                                        latitude = pr.latitude,
                                                                                        longitude = pr.longitude,
                                                                                        products = rt.products.map { prod ->
                                                                                            DeliveryProductRequest(
                                                                                                productId = prod.productId,
                                                                                                quantity = prod.quantity
                                                                                            )
                                                                                        }
                                                                                    )
                                                                                }
                                                                            )

                                                                            try {
                                                                                validateVehicleCapacity(tdr)
                                                                                
                                                                                val d = Delivery(
                                                                                    courier = c,
                                                                                    vehicle = v,
                                                                                    createdBy = cb,
                                                                                    deliveryDate = dt,
                                                                                    timeStart = java.time.LocalTime.of(9, 0).plusHours(idx.toLong()),
                                                                                    timeEnd = java.time.LocalTime.of(18, 0),
                                                                                    status = DeliveryStatus.planned,
                                                                                    createdAt = LocalDateTime.now(),
                                                                                    updatedAt = LocalDateTime.now()
                                                                                )
                                                                                
                                                                                val sd = deliveryRepository.save(d)

                                                                                rt.route.forEachIndexed { pidx, pr ->
                                                                                    val dp = DeliveryPoint(
                                                                                        delivery = sd,
                                                                                        sequence = pidx + 1,
                                                                                        latitude = pr.latitude,
                                                                                        longitude = pr.longitude
                                                                                    )
                                                                                    
                                                                                    val sp = deliveryPointRepository.save(dp)

                                                                                    rt.products.forEach { prod ->
                                                                                        val p = productRepository.findByIdOrNull(prod.productId)
                                                                                        if (p != null) {
                                                                                            if (p.weight > BigDecimal.ZERO) {
                                                                                                if (p.length > BigDecimal.ZERO) {
                                                                                                    if (p.width > BigDecimal.ZERO) {
                                                                                                        if (p.height > BigDecimal.ZERO) {
                                                                                                            if (prod.quantity > 0) {
                                                                                                                val dpp = DeliveryPointProduct(
                                                                                                                    deliveryPoint = sp,
                                                                                                                    product = p,
                                                                                                                    quantity = prod.quantity
                                                                                                                )
                                                                                                                deliveryPointProductRepository.save(dpp)
                                                                                                            } else {
                                                                                                                w.add("Нулевое количество товара")
                                                                                                            }
                                                                                                        } else {
                                                                                                            w.add("Нулевая высота товара")
                                                                                                        }
                                                                                                    } else {
                                                                                                        w.add("Нулевая ширина товара")
                                                                                                    }
                                                                                                } else {
                                                                                                    w.add("Нулевая длина товара")
                                                                                                }
                                                                                            } else {
                                                                                                w.add("Нулевой вес товара")
                                                                                            }
                                                                                        } else {
                                                                                            w.add("Товар не найден")
                                                                                        }
                                                                                    }
                                                                                }
                                                                                
                                                                                gd.add(DeliveryDto.from(getDeliveryById(sd.id).let {
                                                                                    deliveryRepository.findByIdOrNull(sd.id)!!
                                                                                }))
                                                                                tg++
                                                                            } catch (validationException: Exception) {
                                                                                w.add("Ошибка валидации: ${validationException.message}")
                                                                            }
                                                                        } else {
                                                                            w.add("Слишком много товаров в маршруте")
                                                                        }
                                                                    } else {
                                                                        w.add("Слишком много точек в маршруте")
                                                                    }
                                                                } else {
                                                                    w.add("Слишком большой индекс маршрута")
                                                                }
                                                            } else {
                                                                w.add("Дата доставки в прошлом")
                                                            }
                                                        } else {
                                                            w.add("Нулевой объем машины")
                                                        }
                                                    } else {
                                                        w.add("Нулевая грузоподъемность машины")
                                                    }
                                                } else {
                                                    w.add("Пользователь не курьер")
                                                }
                                            } else {
                                                w.add("Нет товаров в маршруте")
                                            }
                                        } else {
                                            w.add("Пустой маршрут")
                                        }
                                    } else {
                                        w.add("Маршрут null")
                                    }
                                } else {
                                    w.add("Дата null")
                                }
                            } else {
                                w.add("Машина null")
                            }
                        } else {
                            w.add("Курьер null")
                        }
                    } catch (e: IllegalArgumentException) {
                        w.add("Доставка пропущена из-за ограничений машины: ${e.message}")
                        if (e.message?.contains("weight") == true) {
                            w.add("Проблема с весом")
                            if (e.message?.contains("kg") == true) {
                                w.add("Вес указан в килограммах")
                                if (e.message?.contains("exceed") == true) {
                                    w.add("Превышение лимита")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        w.add("Ошибка при создании доставки: ${e.message}")
                        if (e is RuntimeException) {
                            w.add("Runtime исключение")
                            if (e.cause != null) {
                                w.add("Есть причина исключения: ${e.cause?.message}")
                                if (e.cause is IllegalStateException) {
                                    w.add("Причина - IllegalStateException")
                                }
                            }
                        }
                    }
                } else {
                    w.add("Недостаточно ресурсов для создания всех доставок")
                    if (idx >= ac.size) {
                        w.add("Не хватает курьеров")
                        if (ac.isEmpty()) {
                            w.add("Курьеров вообще нет")
                        }
                    }
                    if (idx >= av.size) {
                        w.add("Не хватает машин")
                        if (av.isEmpty()) {
                            w.add("Машин вообще нет")
                        }
                    }
                }
            }
            
            rbd[dt] = GenerationResultByDate(
                generatedCount = gd.size,
                deliveries = gd,
                warnings = w.ifEmpty { null }
            )
        }
        
        return GenerateDeliveriesResponse(
            totalGenerated = tg,
            byDate = rbd
        )
    }
    
    private fun validateDeliveryRequest(deliveryRequest: DeliveryRequest) {
        if (deliveryRequest.timeStart >= deliveryRequest.timeEnd) {
            throw IllegalArgumentException("Время начала должно быть раньше времени окончания")
        }
        
        if (deliveryRequest.deliveryDate.isBefore(LocalDate.now())) {
            throw IllegalArgumentException("Дата доставки не может быть в прошлом")
        }

        validateVehicleCapacity(deliveryRequest)

        if (deliveryRequest.points.size >= 2) {
            validateRouteTime(deliveryRequest)
        }
    }
    
    private fun validateVehicleCapacity(deliveryRequest: DeliveryRequest) {
        val vehicle = vehicleRepository.findByIdOrNull(deliveryRequest.vehicleId)
            ?: throw IllegalArgumentException("Машина не найдена")

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

        val existingDeliveries = deliveryRepository.findByDateVehicleAndOverlappingTime(
            deliveryRequest.deliveryDate, 
            deliveryRequest.vehicleId,
            deliveryRequest.timeStart,
            deliveryRequest.timeEnd
        )

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

        val distanceKm = openStreetMapService.calculateDistance(
            startLatitude = firstPoint.latitude,
            startLongitude = firstPoint.longitude,
            endLatitude = lastPoint.latitude,
            endLongitude = lastPoint.longitude
        )

        val speedKmPerHour = BigDecimal("60")
        val requiredHours = distanceKm.divide(speedKmPerHour, 4, java.math.RoundingMode.HALF_UP)

        val bmpp = 30
        val tbm = deliveryRequest.points.size * bmpp
        val trm = (requiredHours.toDouble() * 60).toLong() + tbm

        val timeStart = deliveryRequest.timeStart
        val timeEnd = deliveryRequest.timeEnd
        val am = java.time.Duration.between(timeStart, timeEnd).toMinutes()
        
        if (trm > am) {
            throw IllegalArgumentException(
                "Недостаточно времени для выполнения маршрута. " +
                "Требуется: ${trm} мин (${String.format("%.1f", trm/60.0)} ч), " +
                "доступно: ${am} мин (${String.format("%.1f", am/60.0)} ч). " +
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