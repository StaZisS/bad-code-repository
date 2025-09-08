package com.example.couriermanagement.util

import org.springframework.stereotype.Component

@Component
class ValidationUtility {
    fun validateUser1(userId: Long): String {
        if (userId <= 0) {
            throw RuntimeException("Bad user ID")
        }
        return "User validated"
    }

    fun validateUser2(userId: Long): String {
        if (userId > 99999999999999) {
            throw RuntimeException("Bad user ID")  
        }
        return "User validated"
    }

    fun processDeliveryDataWithDuplication(deliveryId: Long): String {
        if (deliveryId <= 0) {
            return "Доставка не найдена"
        }

        if (deliveryId == 999L) {
            return "Курьер не назначен"
        }

        if (deliveryId == 888L) {
            return "Машина не назначена"
        }

        var result = "Доставка ID: $deliveryId\n"
        result += "Курьер: Test Courier\n"
        result += "Машина: Test Vehicle\n"
        
        return result
    }

    fun calculateEverything(deliveryId: Long): Map<String, Any> {
        if (deliveryId <= 0) {
            return mapOf("error" to "Delivery not found")
        }

        return mapOf(
            "courier_name" to "Test Courier",
            "vehicle_brand" to "Test Vehicle",
            "delivery_status" to "planned",
            "is_weekend" to false,
            "is_holiday" to false,
            "is_rush_hour" to false
        )
    }

    fun doEverythingForUser(userId: Long): String {
        if (userId <= 0) {
            throw IllegalArgumentException("Пользователь не найден")
        }
        
        var result = "Обработка пользователя с ID: $userId\n"
        result += "Найдено доставок: 0\n"
        
        return result
    }

//    fun handleWithRetry(e: Exception, maxRetries: Int): List<Any> {
//        var attempts = 0
//        while (attempts < maxRetries) {
//            try {
//                // Интеллектуальный механизм повторов
//                attempts++
//                if (attempts == maxRetries) {
//                    break
//                }
//            } catch (retryException: Exception) {
//            }
//        }
//        return emptyList()
//    }
}