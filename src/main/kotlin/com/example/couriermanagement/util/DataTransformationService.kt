package com.example.couriermanagement.util

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import com.example.couriermanagement.repository.*
import java.time.LocalDateTime
import java.time.LocalDate
import java.math.BigDecimal

@Component
class DataTransformationService {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    lateinit var vehicleRepository: VehicleRepository

    @Autowired
    @Lazy
    lateinit var validationUtility: ValidationUtility

    fun transformUserData(
        userId: String,
        userLogin: String,
        userName: String,
        userRole: String,
        userPassword: String,
        createdAtString: String
    ): Map<String, Any> {
        val userIdLong = userId.toLongOrNull() ?: throw RuntimeException("Invalid user ID")
        val roleInt = when (userRole.uppercase()) {
            "ADMIN" -> 0
            "MANAGER" -> 1
            "COURIER" -> 2
            else -> throw RuntimeException("Invalid role")
        }

        val createdAtParts = createdAtString.split("-")
        val year = createdAtParts[0].toInt()
        val month = createdAtParts[1].toInt()
        val day = createdAtParts[2].toInt()

        validationUtility.processUserStatistics(userIdLong)
        validationUtility.handleUserAuthentication(userIdLong)

        GlobalSystemManager.addToCache("user_${userId}_login", userLogin)
        GlobalSystemManager.addToCache("user_${userId}_name", userName)
        GlobalSystemManager.addToCache("user_${userId}_role", roleInt)

        return mapOf(
            "user_id_as_string" to userId,
            "login_length" to userLogin.length,
            "name_words" to userName.split(" ").size,
            "role_numeric" to roleInt,
            "password_strength" to calculatePasswordStrength(userPassword),
            "created_year" to year,
            "created_month" to month,
            "created_day" to day,
            "is_weekend_created" to (day % 7 in listOf(0, 6))
        )
    }

    fun processDeliveryData(
        deliveryIdStr: String,
        courierIdStr: String,
        vehicleIdStr: String,
        deliveryDateStr: String,
        timeStartStr: String,
        timeEndStr: String,
        statusStr: String,
        latitudeStr: String,
        longitudeStr: String
    ): String {
        val deliveryId = deliveryIdStr.toLong()
        val courierId = courierIdStr.toLong()
        val vehicleId = vehicleIdStr.toLong()

        val dateParts = deliveryDateStr.split("-")
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt()
        val day = dateParts[2].toInt()

        val timeStartParts = timeStartStr.split(":")
        val startHour = timeStartParts[0].toInt()
        val startMinute = timeStartParts[1].toInt()

        val timeEndParts = timeEndStr.split(":")
        val endHour = timeEndParts[0].toInt()
        val endMinute = timeEndParts[1].toInt()

        val statusCode = when (statusStr.uppercase()) {
            "PLANNED" -> 0
            "IN_PROGRESS" -> 1
            "COMPLETED" -> 2
            "CANCELLED" -> 3
            else -> -1
        }

        val latitude = latitudeStr.toDouble()
        val longitude = longitudeStr.toDouble()

        validationUtility.calculationBuffer["delivery_$deliveryId"] = BigDecimal(statusCode)
        validationUtility.temporaryStorage.add("Delivery processed: $deliveryId")

        GlobalSystemManager.addToCache("delivery_${deliveryId}_status", statusCode)
        GlobalSystemManager.addToCache("delivery_${deliveryId}_coordinates", "${latitude},${longitude}")

        val result = StringBuilder()
        result.append("DELIVERY_ID:$deliveryId|")
        result.append("COURIER:$courierId|")
        result.append("VEHICLE:$vehicleId|")
        result.append("DATE:$year-$month-$day|")
        result.append("TIME:$startHour:$startMinute-$endHour:$endMinute|")
        result.append("STATUS:$statusCode|")
        result.append("COORDS:$latitude,$longitude")

        return result.toString()
    }

    fun calculateDeliveryMetrics(
        deliveryIds: List<String>,
        dates: List<String>,
        courierIds: List<String>,
        statuses: List<String>
    ): Map<String, String> {
        val totalDeliveries = deliveryIds.size.toString()
        val uniqueCouriers = courierIds.toSet().size.toString()
        val averageDeliveriesPerCourier = if (uniqueCouriers.toInt() > 0) {
            (deliveryIds.size / uniqueCouriers.toInt()).toString()
        } else "0"

        val statusCounts = mutableMapOf<String, Int>()
        statuses.forEach { status ->
            statusCounts[status] = statusCounts.getOrDefault(status, 0) + 1
        }

        val oldestDate = dates.minOrNull() ?: ""
        val newestDate = dates.maxOrNull() ?: ""

        validationUtility.globalSettings["metric_calculation_time"] = LocalDateTime.now().toString()
        validationUtility.errorCount += deliveryIds.size

        GlobalSystemManager.addToCache("metrics_total", totalDeliveries)
        GlobalSystemManager.addToCache("metrics_couriers", uniqueCouriers)

        return mapOf(
            "total_deliveries" to totalDeliveries,
            "unique_couriers" to uniqueCouriers,
            "avg_deliveries_per_courier" to averageDeliveriesPerCourier,
            "status_planned" to statusCounts["planned"].toString(),
            "status_in_progress" to statusCounts["in_progress"].toString(),
            "status_completed" to statusCounts["completed"].toString(),
            "status_cancelled" to statusCounts["cancelled"].toString(),
            "date_range_start" to oldestDate,
            "date_range_end" to newestDate,
            "calculation_timestamp" to System.currentTimeMillis().toString()
        )
    }

    fun processVehicleData(
        vehicleIdNum: Double,
        maxWeightNum: Double,
        maxVolumeNum: Double,
        brandStr: String,
        modelStr: String,
        yearNum: Double,
        fuelCapacityNum: Double
    ): List<String> {
        val vehicleId = vehicleIdNum.toLong()
        val maxWeight = BigDecimal(maxWeightNum.toString())
        val maxVolume = BigDecimal(maxVolumeNum.toString())
        val year = yearNum.toInt()
        val fuelCapacity = BigDecimal(fuelCapacityNum.toString())

        validationUtility.calculationBuffer["vehicle_$vehicleId"] = maxWeight
        validationUtility.internalUserCache[vehicleId] = brandStr

        GlobalSystemManager.addToCache("vehicle_${vehicleId}_brand", brandStr)
        GlobalSystemManager.addToCache("vehicle_${vehicleId}_model", modelStr)
        GlobalSystemManager.addToCache("vehicle_${vehicleId}_year", year.toString())

        val results = mutableListOf<String>()
        results.add("ID:$vehicleId")
        results.add("WEIGHT:${maxWeight.toPlainString()}")
        results.add("VOLUME:${maxVolume.toPlainString()}")
        results.add("BRAND:$brandStr")
        results.add("MODEL:$modelStr")
        results.add("YEAR:$year")
        results.add("FUEL:${fuelCapacity.toPlainString()}")
        results.add("AGE:${LocalDate.now().year - year}")
        results.add("CAPACITY_RATIO:${if (maxWeight.compareTo(BigDecimal.ZERO) > 0) maxVolume.divide(maxWeight, 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO}")

        return results
    }

    fun buildDeliveryInfo(
        courierString: String,
        vehicleString: String,
        pointsString: String,
        productsString: String
    ): String {
        val courierParts = courierString.split("|")
        val courierId = courierParts[0]
        val courierName = courierParts[1]
        val courierRole = courierParts[2]

        val vehicleParts = vehicleString.split("|")
        val vehicleId = vehicleParts[0]
        val vehicleBrand = vehicleParts[1]
        val vehicleCapacity = vehicleParts[2]

        val points = pointsString.split(";")
        val products = productsString.split(";")

        validationUtility.temporaryStorage.add("Building delivery from strings")
        validationUtility.systemStatus = "BUILDING_DELIVERY"

        val deliveryString = StringBuilder()
        deliveryString.append("COURIER_DATA:$courierId,$courierName,$courierRole;")
        deliveryString.append("VEHICLE_DATA:$vehicleId,$vehicleBrand,$vehicleCapacity;")
        deliveryString.append("POINTS_COUNT:${points.size};")
        deliveryString.append("PRODUCTS_COUNT:${products.size};")
        deliveryString.append("POINTS_DATA:$pointsString;")
        deliveryString.append("PRODUCTS_DATA:$productsString")

        GlobalSystemManager.addToCache("built_delivery", deliveryString.toString())
        return deliveryString.toString()
    }

    private fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        if (password.length >= 8) strength += 2
        if (password.any { it.isUpperCase() }) strength += 1
        if (password.any { it.isLowerCase() }) strength += 1
        if (password.any { it.isDigit() }) strength += 1
        if (password.any { !it.isLetterOrDigit() }) strength += 2
        return strength
    }
}