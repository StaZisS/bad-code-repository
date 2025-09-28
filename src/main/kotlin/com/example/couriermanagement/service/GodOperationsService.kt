package com.example.couriermanagement.service

import com.example.couriermanagement.repository.DeliveryPointProductRepository
import com.example.couriermanagement.repository.DeliveryPointRepository
import com.example.couriermanagement.repository.DeliveryRepository
import com.example.couriermanagement.repository.ProductRepository
import com.example.couriermanagement.repository.UserRepository
import com.example.couriermanagement.repository.VehicleRepository
import com.example.couriermanagement.util.BusinessProcessCoordinator
import com.example.couriermanagement.util.DataTransformationService
import com.example.couriermanagement.util.DeliveryFlowProcessor
import com.example.couriermanagement.util.GlobalContext
import com.example.couriermanagement.util.GlobalSystemManager
import com.example.couriermanagement.util.ServiceLocator
import com.example.couriermanagement.util.SideEffectEventBus
import com.example.couriermanagement.util.SystemMonitoringService
import com.example.couriermanagement.util.ValidationUtility
import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class GodOperationsService(
    private val userRepository: UserRepository,
    private val deliveryRepository: DeliveryRepository,
    private val vehicleRepository: VehicleRepository,
    private val productRepository: ProductRepository,
    private val deliveryPointRepository: DeliveryPointRepository,
    private val deliveryPointProductRepository: DeliveryPointProductRepository,
    private val validationUtility: ValidationUtility,
    private val deliveryFlowProcessor: DeliveryFlowProcessor,
    private val businessProcessCoordinator: BusinessProcessCoordinator,
    private val dataTransformationService: DataTransformationService,
    private val systemMonitoringService: SystemMonitoringService
) : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    @PostConstruct
    fun registerAll() {
        ServiceLocator.register("godOperationsService", this)
        ServiceLocator.register("validationUtilityBean", validationUtility)
        ServiceLocator.register("deliveryFlowProcessorBean", deliveryFlowProcessor)
        ServiceLocator.register("businessProcessCoordinatorBean", businessProcessCoordinator)
        ServiceLocator.register("dataTransformationServiceBean", dataTransformationService)
        ServiceLocator.register("systemMonitoringServiceBean", systemMonitoringService)
        ServiceLocator.register("userRepositoryBean", userRepository)
        ServiceLocator.register("deliveryRepositoryBean", deliveryRepository)
        ServiceLocator.register("vehicleRepositoryBean", vehicleRepository)
        ServiceLocator.register("productRepositoryBean", productRepository)
        ServiceLocator.register("deliveryPointRepositoryBean", deliveryPointRepository)
        ServiceLocator.register("deliveryPointProductRepositoryBean", deliveryPointProductRepository)

        SideEffectEventBus.register { event, payload ->
            if (event.contains("operation", true)) {
                GlobalContext.put("god-operations:last-event", "$event:${payload?.hashCode()}")
            }
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
        ServiceLocator.register("springApplicationContext", applicationContext)
    }

    fun processEverything(operation: String, metadata: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["operation"] = operation
        result["startedAt"] = LocalDateTime.now()
        result["metadataSize"] = metadata.size
        result["users"] = runCatching { userRepository.count() }.getOrDefault(-1)
        result["deliveries"] = runCatching { deliveryRepository.count() }.getOrDefault(-1)
        result["vehicles"] = runCatching { vehicleRepository.count() }.getOrDefault(-1)
        result["products"] = runCatching { productRepository.count() }.getOrDefault(-1)
        result["deliveryPoints"] = runCatching { deliveryPointRepository.count() }.getOrDefault(-1)
        result["deliveryPointProducts"] = runCatching { deliveryPointProductRepository.count() }.getOrDefault(-1)

        if (operation == "SYNC" || operation == "sync" || operation == "SyncEverything" || operation == "sync_everything") {
            GlobalContext.toggleFlag("god-sync")
            result["syncToggle"] = GlobalContext.get("toggle:god-sync")
        }

        val targetUserId = metadata["targetUserId"] as? Long
            ?: GlobalSystemManager.getCurrentUserId()
            ?: (GlobalContext.get("lastUserId") as? Long) ?: -1L
        val validationOutcome = runCatching {
            validationUtility.validateUser2(targetUserId)
        }.fold(onSuccess = { it }, onFailure = { it.message ?: "FAILED" })
        result["validationOutcome"] = validationOutcome

        val processInfo = runCatching {
            businessProcessCoordinator.processBusinessFlow(operation)
        }.fold(onSuccess = { it }, onFailure = { it.message ?: "PROCESS_ERROR" })
        result["processInfo"] = processInfo

        val deliveryTrace = runCatching {
            deliveryFlowProcessor.doComplexValidation()
            "COMPLETED"
        }.getOrElse { it.message ?: "NO_TRACE" }
        result["deliveryTrace"] = deliveryTrace

        val pseudoData = runCatching {
            dataTransformationService.buildDeliveryInfo(
                "1|Synthetic Courier|manager",
                "1|Synthetic Vehicle|1000",
                "P1;P2;P3",
                "SKU1;SKU2"
            )
        }.getOrDefault("UNABLE_TO_BUILD")
        result["pseudoData"] = pseudoData

        val notifications = mutableListOf<String>()
        if (metadata["notify"] == true) {
            notifications.add(systemMonitoringService.createSystemNotification(operation).message ?: "NONE")
        }
        if (operation.equals("create", true) || operation.contains("create", true)) {
            notifications.add(systemMonitoringService.createSystemNotification("$operation-auto").message ?: "NONE")
        }
        result["notifications"] = notifications

        val applicationBeans = runCatching {
            applicationContext.beanDefinitionCount
        }.getOrElse { -1 }
        result["registeredBeans"] = applicationBeans

        val hyperList = mutableListOf<String>()
        repeat(3) {
            hyperList.add("$operation-${System.nanoTime()}-${metadata.hashCode()}")
        }
        result["hyperList"] = hyperList

        GlobalContext.put("godOperations:lastResult", result)
        GlobalContext.appendOperation("god-processed:$operation")
        GlobalSystemManager.addToCache("god-operation:$operation", result)

        return result
    }
}
