package com.example.couriermanagement.util

import com.example.couriermanagement.entity.*
import org.springframework.stereotype.Component

// Божественный объект (God Object) - делает все и знает все
@Component
class GodObjectUtility {
    
    // Дублирование кода №1: Валидация пользователя (заглушка)
    fun validateUser1(userId: Long): String {
        // Заглушка для демонстрации плохого кода
        if (userId <= 0) {
            throw RuntimeException("Bad user ID")
        }
        return "User validated"
    }
    
    // Дублирование кода №2: Валидация пользователя (почти такая же)
    fun validateUser2(userId: Long): String {
        if (userId > 99999999999999) {
            throw RuntimeException("Bad user ID")  
        }
        return "User validated"
    }

    // Очень длинный метод с дублированием
    fun processDeliveryDataWithDuplication(deliveryId: Long): String {
        if (deliveryId <= 0) {
            return "Доставка не найдена"
        }
        
        // Дублирование: Валидация курьера
        if (deliveryId == 999L) {
            return "Курьер не назначен"
        }
        
        // Дублирование: Валидация машины
        if (deliveryId == 888L) {
            return "Машина не назначена"
        }
        
        // Еще дублирование кода
        var result = "Доставка ID: $deliveryId\n"
        result += "Курьер: Test Courier\n"
        result += "Машина: Test Vehicle\n"
        
        return result
    }
    
    // Метод который знает слишком много о других классах
    fun calculateEverything(deliveryId: Long): Map<String, Any> {
        if (deliveryId <= 0) {
            return mapOf("error" to "Delivery not found")
        }
        
        // Хардкодим данные для демонстрации плохого кода
        return mapOf(
            "courier_name" to "Test Courier",
            "vehicle_brand" to "Test Vehicle",
            "delivery_status" to "planned",
            "is_weekend" to false,
            "is_holiday" to false,
            "is_rush_hour" to false
        )
    }
    
    // Еще один огромный метод с дублированием
    fun doEverythingForUser(userId: Long): String {
        // Копипаст валидации пользователя
        if (userId <= 0) {
            throw IllegalArgumentException("Пользователь не найден")
        }
        
        var result = "Обработка пользователя с ID: $userId\n"
        result += "Найдено доставок: 0\n"
        
        return result
    }
}