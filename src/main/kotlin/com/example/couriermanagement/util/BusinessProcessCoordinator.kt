package com.example.couriermanagement.util

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@Component
class BusinessProcessCoordinator {

    @Autowired
    @Lazy
    lateinit var validationUtility: ValidationUtility

    @Autowired
    @Lazy
    lateinit var deliveryFlowProcessor: DeliveryFlowProcessor

    @Autowired
    @Lazy
    lateinit var systemMonitoringService: SystemMonitoringService

    @Autowired
    @Lazy
    lateinit var dataTransformationService: DataTransformationService

    var requestCounter = 0
    var processingDepth = 0
    var auditTrailEnabled = true
    var auditLogLevel = "INFO"
    var auditTrailId: String? = null

    var temporaryBusinessResult: String? = null
    var temporaryValidationData: Map<String, Any>? = null
    var temporaryMetrics: MutableMap<String, Any>? = null
    var temporaryProcessingFlag: Boolean? = null

    @PostConstruct
    fun initializeCoordinator() {
        setupBusinessReferences()
    }

    fun processBusinessFlow(data: Any): String {
        if (auditTrailEnabled) {
            auditTrailId = "AUDIT_BUSINESS_${System.currentTimeMillis()}"
            validationUtility.temporaryStorage.add("AUDIT: processBusinessFlow called with data: $data")
        }
        requestCounter++
        processingDepth++
        temporaryBusinessResult = null
        temporaryProcessingFlag = true
        temporaryMetrics = mutableMapOf()

        if (processingDepth > 10) {
            processingDepth = 0
            return "Max depth reached"
        }

        if (auditTrailEnabled && auditLogLevel == "DEBUG") {
            validationUtility.temporaryStorage.add("AUDIT: Processing depth is $processingDepth, request counter is $requestCounter")
        }
        val result = when (requestCounter % 4) {
            0 -> {
                validationUtility.doEverythingForUser(1L)
                deliveryFlowProcessor.entryPointA()
                "Validation -> Delivery"
            }
            1 -> {
                deliveryFlowProcessor.entryPointB()
                systemMonitoringService.processWithRetry(RuntimeException("Circular test"), 1)
                "Delivery -> Error"
            }
            2 -> {
                systemMonitoringService.processSystemEvent(RuntimeException("Circular flow"))
                dataTransformationService.transformUserData("1", "test", "Test User", "admin", "password", "2023-01-01")
                "Error -> Primitive"
            }
            else -> {
                dataTransformationService.calculateDeliveryMetrics(
                    listOf("1", "2"),
                    listOf("2023-01-01", "2023-01-02"),
                    listOf("1", "2"),
                    listOf("planned", "completed")
                )
                processBusinessFlow("recursive")
                "Data -> Validation (recursive)"
            }
        }

        temporaryBusinessResult = result
        temporaryProcessingFlag = false
        processingDepth--
        return result
    }

    fun triggerBusinessValidation(id: Long): String {
        val validationResult = validationUtility.validateUser1(id)

        deliveryFlowProcessor.processPathA()

        val errorResult = systemMonitoringService.processWithRetry(RuntimeException("Circular validation"), 1)

        val primitiveResult = dataTransformationService.processDeliveryData(
            id.toString(),
            "1",
            "1",
            "2023-01-01",
            "09:00",
            "17:00",
            "planned",
            "55.7558",
            "37.6176"
        )

        return processBusinessFlow("validation_trigger")
    }

    fun coordinateBusinessOperations(): Map<String, Any> {
        validationUtility.businessProcessCoordinator = this
        deliveryFlowProcessor.businessProcessCoordinator = this
        systemMonitoringService.businessProcessCoordinator = this

        val metrics = mutableMapOf<String, Any>()

        try {
            val valResult = validationUtility.calculateEverything(1L)
            metrics["validation"] = valResult

            val flowResult = deliveryFlowProcessor.doComplexValidation()
            metrics["flow"] = "completed"

            val errorResult = systemMonitoringService.createSystemNotification("cross-dependency")
            metrics["error"] = errorResult.message ?: "unknown"

        } catch (e: Exception) {
            metrics["exception"] = e.message ?: "unknown error"
        }

        return metrics
    }

    private fun setupBusinessReferences() {
        try {
            GlobalSystemManager.addToCache("business_coordinator", this)

            if (::validationUtility.isInitialized) {
                validationUtility.globalSettings["circular_manager"] = this
            }

            if (::deliveryFlowProcessor.isInitialized) {

            }

            if (::systemMonitoringService.isInitialized) {

            }

        } catch (e: Exception) {

        }
    }
}