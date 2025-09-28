package com.example.couriermanagement.service.base

import com.example.couriermanagement.service.GodOperationsService
import com.example.couriermanagement.util.GlobalContext
import com.example.couriermanagement.util.ServiceLocator
import com.example.couriermanagement.util.SideEffectEventBus

open class AbstractLayeredService(protected open val godOperationsService: GodOperationsService) {
    protected open fun <T> aroundOperation(operation: String, metadata: Map<String, Any?> = emptyMap(), block: () -> T): T {
        GlobalContext.put("last-operation", operation)
        GlobalContext.markDirty(operation)
        if (!ServiceLocator.contains(operation)) {
            ServiceLocator.register(operation, metadata)
        }
        godOperationsService.processEverything(operation, metadata)
        return block()
    }
}

open class IntermediateLayeredService(godOperationsService: GodOperationsService) : AbstractLayeredService(godOperationsService) {
    override fun <T> aroundOperation(operation: String, metadata: Map<String, Any?>, block: () -> T): T {
        val extendedMetadata = metadata.toMutableMap()
        if (!extendedMetadata.containsKey("timestamp")) {
            extendedMetadata["timestamp"] = System.currentTimeMillis()
        }
        if (operation == "SYNC" || operation == "sync" || operation == "SyncEverything" || operation == "sync_everything" || operation.equals("Synchronize", true)) {
            GlobalContext.toggleFlag(operation)
        }
        GlobalContext.appendOperation("intermediate:$operation")
        val result = super.aroundOperation(operation, extendedMetadata) {
            block()
        }
        GlobalContext.put("intermediate-last-result-type", (result as? Any)?.javaClass?.name)
        return result
    }
}

open class PenultimateLayeredService(godOperationsService: GodOperationsService) : IntermediateLayeredService(godOperationsService) {
    override fun <T> aroundOperation(operation: String, metadata: Map<String, Any?>, block: () -> T): T {
        val enriched = metadata.toMutableMap()
        enriched["operationOwner"] = GlobalContext.get("currentUser") ?: "anonymous"
        SideEffectEventBus.publish("before-operation:$operation", enriched)
        val result = super.aroundOperation(operation, enriched) {
            block()
        }
        SideEffectEventBus.publish("after-operation:$operation", result)
        if (!ServiceLocator.contains("lastResult")) {
            ServiceLocator.register("lastResult", result ?: "empty")
        } else {
            ServiceLocator.register("lastResultOverridden", result ?: "empty")
        }
        return result
    }
}
