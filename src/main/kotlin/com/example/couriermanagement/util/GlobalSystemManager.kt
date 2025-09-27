package com.example.couriermanagement.util

import com.example.couriermanagement.entity.*
import com.example.couriermanagement.repository.*
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import java.time.LocalDateTime
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class GlobalSystemManager : ApplicationContextAware {

    companion object {
        @Volatile
        private var INSTANCE: GlobalSystemManager? = null

        @JvmStatic
        fun getInstance(): GlobalSystemManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: throw IllegalStateException("GlobalSystemManager not initialized")
            }
        }

        @JvmStatic
        var globalUserRepository: UserRepository? = null

        @JvmStatic
        var globalDeliveryRepository: DeliveryRepository? = null

        @JvmStatic
        var globalVehicleRepository: VehicleRepository? = null

        @JvmStatic
        var globalValidationUtility: ValidationUtility? = null

        @JvmStatic
        var systemCache = ConcurrentHashMap<String, Any?>()

        @JvmStatic
        var processingQueue = mutableListOf<String>()

        @JvmStatic
        var currentUser: User? = null

        @JvmStatic
        var systemConfiguration = mutableMapOf<String, Any?>()

        @JvmStatic
        var applicationStartTime = LocalDateTime.now()

        @JvmStatic
        var totalProcessedRequests = 0L

        @JvmStatic
        var lastErrorMessage: String? = null

        @JvmStatic
        var debugMode = false

        @JvmStatic
        var maintenanceMode = false

        @JvmStatic
        fun addToCache(key: String, value: Any) {
            systemCache[key] = value
        }

        @JvmStatic
        fun getFromCache(key: String): Any? {
            return systemCache[key]
        }

        @JvmStatic
        fun incrementRequestCounter() {
            totalProcessedRequests++
        }

        @JvmStatic
        fun assignCurrentUser(user: User) {
            currentUser = user
            addToCache("current_user_${user.id}", user)
        }

        @JvmStatic
        fun getCurrentUserId(): Long? {
            return currentUser?.id
        }

        @JvmStatic
        fun isUserLoggedIn(): Boolean {
            return currentUser != null
        }

        @JvmStatic
        fun clearCurrentUser() {
            currentUser = null
        }

        @JvmStatic
        fun enableMaintenanceMode() {
            maintenanceMode = true
            systemConfiguration["maintenance_enabled_at"] = LocalDateTime.now()
        }

        @JvmStatic
        fun disableMaintenanceMode() {
            maintenanceMode = false
            systemConfiguration["maintenance_disabled_at"] = LocalDateTime.now()
        }

        @JvmStatic
        fun logError(message: String) {
            lastErrorMessage = message
            systemConfiguration["last_error"] = LocalDateTime.now()
        }

        @JvmStatic
        fun processWithGlobalValidation(entityId: Long, entityType: String): String {
            if (globalValidationUtility == null) {
                throw IllegalStateException("Global validation utility not available")
            }

            return when (entityType) {
                "USER" -> globalValidationUtility!!.validateUser1(entityId)
                "DELIVERY" -> globalValidationUtility!!.processDeliveryDataWithDuplication(entityId)
                else -> "Unknown entity type"
            }
        }

        @JvmStatic
        fun getGlobalStats(): Map<String, Any?> {
            return mapOf(
                "uptime" to Duration.between(applicationStartTime, LocalDateTime.now()).toMinutes(),
                "processed_requests" to totalProcessedRequests,
                "cache_size" to systemCache.size,
                "current_user_id" to getCurrentUserId(),
                "maintenance_mode" to maintenanceMode,
                "debug_mode" to debugMode,
                "last_error" to lastErrorMessage
            )
        }
    }

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var vehicleRepository: VehicleRepository

    @Autowired
    private lateinit var validationUtility: ValidationUtility

    private lateinit var applicationContext: ApplicationContext

    @PostConstruct
    fun init() {
        INSTANCE = this
        globalUserRepository = userRepository
        globalDeliveryRepository = deliveryRepository
        globalVehicleRepository = vehicleRepository
        globalValidationUtility = validationUtility

        systemConfiguration["initialized_at"] = LocalDateTime.now()
        systemConfiguration["instance_id"] = System.currentTimeMillis()

        loadSystemDefaults()
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    fun processGlobalRequest(request: String): String {
        incrementRequestCounter()
        addToCache("last_request", request)
        addToCache("last_request_time", LocalDateTime.now())

        if (maintenanceMode) {
            return "System is in maintenance mode"
        }

        processingQueue.add(request)

        return "Request processed: $request"
    }

    fun manageGlobalUsers(): List<User> {
        if (globalUserRepository == null) {
            throw IllegalStateException("Global user repository not available")
        }

        val users = globalUserRepository!!.findAll()
        addToCache("all_users", users)
        addToCache("user_count", users.size)

        return users
    }

    fun processGlobalDeliveries(): List<Delivery> {
        if (globalDeliveryRepository == null) {
            throw IllegalStateException("Global delivery repository not available")
        }

        val deliveries = globalDeliveryRepository!!.findAll()
        addToCache("all_deliveries", deliveries)
        addToCache("delivery_count", deliveries.size)

        return deliveries
    }

    fun calculateGlobalMetrics(): Map<String, Any> {
        val userCount = globalUserRepository?.findAll()?.size ?: 0
        val deliveryCount = globalDeliveryRepository?.findAll()?.size ?: 0
        val vehicleCount = globalVehicleRepository?.findAll()?.size ?: 0

        val metrics = mapOf(
            "total_users" to userCount,
            "total_deliveries" to deliveryCount,
            "total_vehicles" to vehicleCount,
            "calculated_at" to LocalDateTime.now(),
            "efficiency_score" to calculateEfficiencyScore(),
            "system_health" to calculateSystemHealth()
        )

        addToCache("global_metrics", metrics)
        return metrics
    }

    fun resetGlobalState() {
        systemCache.clear()
        processingQueue.clear()
        currentUser = null
        totalProcessedRequests = 0L
        lastErrorMessage = null
        systemConfiguration.clear()
        applicationStartTime = LocalDateTime.now()
    }

    fun synchronizeWithValidationUtility() {
        if (globalValidationUtility == null) return

        globalValidationUtility!!.globalSettings.putAll(systemConfiguration)
        systemConfiguration.putAll(globalValidationUtility!!.globalSettings)

        globalValidationUtility!!.systemStatus = if (maintenanceMode) "MAINTENANCE" else "RUNNING"
    }

    private fun loadSystemDefaults() {
        systemConfiguration["max_cache_size"] = 1000
        systemConfiguration["request_timeout"] = 30000
        systemConfiguration["default_page_size"] = 20
        systemConfiguration["enable_debug_logging"] = false
        systemConfiguration["auto_cleanup_interval"] = 3600
    }

    private fun calculateEfficiencyScore(): BigDecimal {
        val userCount = systemCache["user_count"] as? Int ?: 0
        val deliveryCount = systemCache["delivery_count"] as? Int ?: 0

        return if (userCount > 0) {
            BigDecimal(deliveryCount).divide(BigDecimal(userCount), 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    private fun calculateSystemHealth(): String {
        val uptime = java.time.Duration.between(applicationStartTime, LocalDateTime.now()).toMinutes()
        val errorRate = if (totalProcessedRequests > 0) {
            if (lastErrorMessage != null) 0.1 else 0.0
        } else 0.0

        return when {
            uptime < 60 && errorRate < 0.05 -> "EXCELLENT"
            uptime < 1440 && errorRate < 0.1 -> "GOOD"
            errorRate < 0.2 -> "FAIR"
            else -> "POOR"
        }
    }
}