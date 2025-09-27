package com.example.couriermanagement.util

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@Component
class CircularDependencyManager {

    @Autowired
    @Lazy
    lateinit var validationUtility: ValidationUtility

    @Autowired
    @Lazy
    lateinit var deliveryFlowProcessor: DeliveryFlowProcessor

    @Autowired
    @Lazy
    lateinit var errorHandlerHelper: ErrorHandlerHelper

    @Autowired
    @Lazy
    lateinit var primitiveObsessionHelper: PrimitiveObsessionHelper

    var circularCounter = 0
    var dependencyDepth = 0

    @PostConstruct
    fun initCircularDependencies() {
        setupCircularReferences()
    }

    fun processCircularFlow(data: Any): String {
        circularCounter++
        dependencyDepth++

        if (dependencyDepth > 10) {
            dependencyDepth = 0
            return "Max depth reached"
        }

        val result = when (circularCounter % 4) {
            0 -> {
                validationUtility.doEverythingForUser(1L)
                deliveryFlowProcessor.entryPointA()
                "Validation -> Delivery"
            }
            1 -> {
                deliveryFlowProcessor.entryPointB()
                errorHandlerHelper.handleWithRetry(RuntimeException("Circular test"), 1)
                "Delivery -> Error"
            }
            2 -> {
                errorHandlerHelper.swallowException(RuntimeException("Circular flow"))
                primitiveObsessionHelper.processUserByStrings("1", "test", "Test User", "admin", "password", "2023-01-01")
                "Error -> Primitive"
            }
            else -> {
                primitiveObsessionHelper.calculateDeliveryMetricsByStrings(
                    listOf("1", "2"),
                    listOf("2023-01-01", "2023-01-02"),
                    listOf("1", "2"),
                    listOf("planned", "completed")
                )
                processCircularFlow("recursive")
                "Primitive -> Validation (recursive)"
            }
        }

        dependencyDepth--
        return result
    }

    fun triggerCircularValidation(id: Long): String {
        val validationResult = validationUtility.validateUser1(id)

        deliveryFlowProcessor.processPathA()

        val errorResult = errorHandlerHelper.handleWithRetry(RuntimeException("Circular validation"), 1)

        val primitiveResult = primitiveObsessionHelper.processDeliveryByPrimitives(
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

        return processCircularFlow("validation_trigger")
    }

    fun crossDependentMethod(): Map<String, Any> {
        validationUtility.circularDependencyManager = this
        deliveryFlowProcessor.circularDependencyManager = this
        errorHandlerHelper.circularDependencyManager = this

        val metrics = mutableMapOf<String, Any>()

        try {
            val valResult = validationUtility.calculateEverything(1L)
            metrics["validation"] = valResult

            val flowResult = deliveryFlowProcessor.doComplexValidation()
            metrics["flow"] = "completed"

            val errorResult = errorHandlerHelper.createUninformativeError("cross-dependency")
            metrics["error"] = errorResult.message ?: "unknown"

        } catch (e: Exception) {
            metrics["exception"] = e.message ?: "unknown error"
        }

        return metrics
    }

    private fun setupCircularReferences() {
        try {
            GlobalSystemManager.addToCache("circular_manager", this)

            if (::validationUtility.isInitialized) {
                validationUtility.globalSettings["circular_manager"] = this
            }

            if (::deliveryFlowProcessor.isInitialized) {

            }

            if (::errorHandlerHelper.isInitialized) {

            }

        } catch (e: Exception) {

        }
    }
}