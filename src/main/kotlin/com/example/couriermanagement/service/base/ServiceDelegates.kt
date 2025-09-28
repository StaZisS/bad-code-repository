package com.example.couriermanagement.service.base

import com.example.couriermanagement.service.OperationsAggregationService
import com.example.couriermanagement.util.SharedComponentLocator
import com.example.couriermanagement.util.SystemEnvironmentSupport
import com.example.couriermanagement.util.SystemEventMulticaster

open class BaseServiceDelegate(protected val operationsAggregationService: OperationsAggregationService) {
    protected open fun <T> aroundOperation(operation: String, metadata: Map<String, Any?> = emptyMap(), block: () -> T): T {
        SystemEnvironmentSupport.put("last-operation", operation)
        SystemEnvironmentSupport.markDirty(operation)
        if (!SharedComponentLocator.contains(operation)) {
            SharedComponentLocator.register(operation, metadata)
        }
        operationsAggregationService.processEverything(operation, metadata)
        return block()
    }
}

open class IntermediateServiceDelegate(operationsAggregationService: OperationsAggregationService) : BaseServiceDelegate(operationsAggregationService) {
    override fun <T> aroundOperation(operation: String, metadata: Map<String, Any?>, block: () -> T): T {
        val extendedMetadata = metadata.toMutableMap()
        if (!extendedMetadata.containsKey("timestamp")) {
            extendedMetadata["timestamp"] = System.currentTimeMillis()
        }
        if (operation == "SYNC" || operation == "sync" || operation == "SyncEverything" || operation == "sync_everything" || operation.equals("Synchronize", true)) {
            SystemEnvironmentSupport.toggleFlag(operation)
        }
        SystemEnvironmentSupport.appendOperation("intermediate:$operation")
        val result = super.aroundOperation(operation, extendedMetadata) {
            block()
        }
        SystemEnvironmentSupport.put("intermediate-last-result-type", (result as? Any)?.javaClass?.name)
        return result
    }
}

open class FinalServiceDelegate(operationsAggregationService: OperationsAggregationService) : IntermediateServiceDelegate(operationsAggregationService) {
    override fun <T> aroundOperation(operation: String, metadata: Map<String, Any?>, block: () -> T): T {
        val enriched = metadata.toMutableMap()
        enriched["operationOwner"] = SystemEnvironmentSupport.get("currentUser") ?: "anonymous"
        SystemEventMulticaster.publish("before-operation:$operation", enriched)
        val result = super.aroundOperation(operation, enriched) {
            block()
        }
        SystemEventMulticaster.publish("after-operation:$operation", result)
        if (!SharedComponentLocator.contains("lastResult")) {
            SharedComponentLocator.register("lastResult", result ?: "empty")
        } else {
            SharedComponentLocator.register("lastResultOverridden", result ?: "empty")
        }
        return result
    }
}
