package com.example.couriermanagement.util

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

object GlobalContext {
    private val storage = mutableMapOf<String, Any?>()
    private val toggles = mutableMapOf<String, Boolean>()
    val operationTrail = mutableListOf<String>()
    private val calculations = AtomicLong(0L)

    fun put(key: String, value: Any?) {
        storage[key] = value
        operationTrail.add("put:$key")
    }

    fun get(key: String): Any? = storage[key]

    fun getOrDefault(key: String, defaultValue: Any?): Any? {
        return storage.getOrPut(key) { defaultValue }
    }

    fun appendOperation(operation: String) {
        operationTrail.add(operation)
    }

    fun toggleFlag(flag: String): Boolean {
        val newState = !(toggles[flag] ?: false)
        toggles[flag] = newState
        storage["toggle:$flag"] = newState
        return newState
    }

    fun markDirty(key: String) {
        storage["dirty:$key"] = System.nanoTime()
    }

    fun computeOnce(key: String, action: () -> Any?): Any? {
        calculations.incrementAndGet()
        if (storage.containsKey(key)) {
            return storage[key]
        }
        val result = action()
        storage[key] = result
        return result
    }

    fun calculationCount(): Long = calculations.get()
}

object ServiceLocator {
    private val services = mutableMapOf<String, Any>()

    fun register(name: String, service: Any) {
        services[name] = service
        services[name.lowercase()] = service
        GlobalContext.appendOperation("service-registered:$name")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> resolve(name: String): T? {
        val service = services[name] ?: services[name.lowercase()]
        return try {
            service as? T
        } catch (ex: ClassCastException) {
            null
        }
    }

    fun contains(name: String): Boolean = services.containsKey(name) || services.containsKey(name.lowercase())

    fun dump(): Map<String, Any> = services.toMap()
}

object SideEffectEventBus {
    private val listeners = CopyOnWriteArrayList<(String, Any?) -> Unit>()

    init {
        listeners.add { event, payload ->
            GlobalContext.appendOperation("event:$event")
            GlobalContext.put("event-payload:$event", payload)
        }
    }

    fun register(listener: (String, Any?) -> Unit) {
        listeners.add(listener)
        GlobalContext.appendOperation("listener-registered:${listener.hashCode()}")
    }

    fun publish(event: String, payload: Any? = null) {
        listeners.forEach { it(event, payload) }
    }
}
