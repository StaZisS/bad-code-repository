package com.example.couriermanagement.service

import com.example.couriermanagement.BaseIntegrationTest
import com.example.couriermanagement.dto.request.*
import com.example.couriermanagement.entity.Product
import com.example.couriermanagement.entity.Vehicle
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

class VehicleCapacityValidationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var deliveryService: com.example.couriermanagement.service.DeliveryService

    private fun createHeavyProduct(): Product {
        return productRepository.save(
            Product(
                name = "Тяжелый товар",
                weight = BigDecimal("600.0"), // 600 кг
                length = BigDecimal("100.0"), // 100 см
                width = BigDecimal("100.0"),  // 100 см
                height = BigDecimal("100.0")  // 100 см = 1 м³
            )
        )
    }

    private fun createBulkyProduct(): Product {
        return productRepository.save(
            Product(
                name = "Объемный товар",
                weight = BigDecimal("10.0"),   // 10 кг (легкий)
                length = BigDecimal("200.0"),  // 200 см
                width = BigDecimal("200.0"),   // 200 см  
                height = BigDecimal("200.0")   // 200 см = 8 м³
            )
        )
    }

    private fun createSmallVehicle(): Vehicle {
        return vehicleRepository.save(
            Vehicle(
                brand = "Маленький грузовик",
                licensePlate = "SMALL123",
                maxWeight = BigDecimal("1000.0"), // 1 тонна
                maxVolume = BigDecimal("10.0")     // 10 м³
            )
        )
    }

    @Test
    fun `delivery should succeed when vehicle has sufficient capacity`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val lightProduct = createProduct() // 1.5 кг, малый объем

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = lightProduct.id,
                            quantity = 10 // 10 * 1.5кг = 15кг - вполне в пределах лимита
                        )
                    )
                )
            )
        )

        try {
            // Установить контекст авторизации для manager-а
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val result = deliveryService.createDelivery(deliveryRequest)
            println("✅ Delivery created successfully with sufficient capacity: ${result.id}")
        } catch (e: Exception) {
            throw AssertionError("Expected delivery creation to succeed with sufficient capacity, but got: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `delivery should fail when exceeding vehicle weight capacity`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val heavyProduct = createHeavyProduct() // 500 кг каждый

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 3 // 3 * 500кг = 1500кг > 1000кг (превышение веса)
                        )
                    )
                )
            )
        )

        try {
            // Установить контекст авторизации для manager-а
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            deliveryService.createDelivery(deliveryRequest)
            throw AssertionError("Expected delivery creation to fail due to weight capacity exceeded")
        } catch (e: IllegalArgumentException) {
            println("✅ Delivery correctly rejected for weight exceeded: ${e.message}")
            assert(e.message?.contains("Превышена грузоподъемность") == true) {
                "Expected weight capacity error, but got: ${e.message}"
            }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `delivery should fail when exceeding vehicle volume capacity`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val bulkyProduct = createBulkyProduct() // 10 кг, 8 м³ каждый

        val deliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = LocalDate.now().plusDays(5),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(18, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = bulkyProduct.id,
                            quantity = 2 // 2 * 8м³ = 16м³ > 10м³ (превышение объема)
                        )
                    )
                )
            )
        )

        try {
            // Установить контекст авторизации для manager-а
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            deliveryService.createDelivery(deliveryRequest)
            throw AssertionError("Expected delivery creation to fail due to volume capacity exceeded")
        } catch (e: IllegalArgumentException) {
            println("✅ Delivery correctly rejected for volume exceeded: ${e.message}")
            assert(e.message?.contains("Превышен объем") == true) {
                "Expected volume capacity error, but got: ${e.message}"
            }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `delivery should fail when combined with existing deliveries exceeding capacity`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val heavyProduct = createHeavyProduct() // 500 кг каждый
        val date = LocalDate.now().plusDays(5)

        // Create first delivery with 600кг (within limit)
        val firstDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date,
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(12, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // 1 * 500кг = 500кг
                        )
                    )
                )
            )
        )

        // Create first delivery successfully
        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val firstDelivery = deliveryService.createDelivery(firstDeliveryRequest)
            println("✅ First delivery created: ${firstDelivery.id} with 500кг")
        } catch (e: Exception) {
            throw AssertionError("First delivery should have succeeded: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }

        // Try to create second delivery with additional 600кг (total = 1100кг > 1000кг)
        val secondDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date, // Same date
            timeStart = LocalTime.of(13, 0),
            timeEnd = LocalTime.of(16, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7600"),
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // Another 500кг, total = 1000кг - should still fit
                        )
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("55.7700"),
                    longitude = BigDecimal("37.6300"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // Another 500кг, total = 1500кг - should fail
                        )
                    )
                )
            )
        )

        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth

            deliveryService.createDelivery(secondDeliveryRequest)
            throw AssertionError("Expected second delivery to fail due to combined capacity exceeded")
        } catch (e: IllegalArgumentException) {
            println("✅ Second delivery correctly rejected for combined capacity exceeded: ${e.message}")
            assert(e.message?.contains("Превышена грузоподъемность") == true) {
                "Expected weight capacity error with existing deliveries, but got: ${e.message}"
            }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `delivery should succeed when time periods do not overlap`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val heavyProduct = createHeavyProduct() // 500 кг каждый
        val date = LocalDate.now().plusDays(5)

        // Create first delivery with 500кг from 9:00-12:00
        val firstDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date,
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(12, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // 1 * 500кг = 500кг
                        )
                    )
                )
            )
        )

        // Create first delivery successfully
        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val firstDelivery = deliveryService.createDelivery(firstDeliveryRequest)
            println("✅ First delivery created: ${firstDelivery.id} from 9:00-12:00 with 500кг")
        } catch (e: Exception) {
            throw AssertionError("First delivery should have succeeded: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }

        // Create second delivery with 500кг from 13:00-16:00 (no time overlap)
        val secondDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date, // Same date
            timeStart = LocalTime.of(13, 0), // Different time - no overlap
            timeEnd = LocalTime.of(16, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7600"),
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // Another 500кг - should succeed since no time overlap
                        )
                    )
                )
            )
        )

        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val secondDelivery = deliveryService.createDelivery(secondDeliveryRequest)
            println("✅ Second delivery created successfully: ${secondDelivery.id} from 13:00-16:00 with 500кг (no time overlap)")
        } catch (e: Exception) {
            throw AssertionError("Second delivery should have succeeded with non-overlapping time, but got: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `delivery should fail when time periods overlap and exceed capacity`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val heavyProduct = createHeavyProduct() // 500 кг каждый
        val date = LocalDate.now().plusDays(5)

        // Create first delivery with 600кг from 9:00-13:00
        val firstDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date,
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(13, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // 500кг
                        )
                    )
                )
            )
        )

        // Create first delivery successfully
        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val firstDelivery = deliveryService.createDelivery(firstDeliveryRequest)
            println("✅ First delivery created: ${firstDelivery.id} from 9:00-13:00 with 500кг")
        } catch (e: Exception) {
            throw AssertionError("First delivery should have succeeded: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }

        // Try to create overlapping delivery with 600кг from 12:00-16:00 (overlaps 12:00-13:00)
        val secondDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date, // Same date
            timeStart = LocalTime.of(12, 0), // Overlaps with first delivery (12:00-13:00)
            timeEnd = LocalTime.of(16, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7600"),
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // 500кг + existing 500кг = 1000кг - exactly at limit, should work
                        )
                    )
                ),
                DeliveryPointRequest(
                    sequence = 2,
                    latitude = BigDecimal("55.7700"),
                    longitude = BigDecimal("37.6300"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // Another 500кг: total = 1500кг > 1000кг limit - should fail
                        )
                    )
                )
            )
        )

        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            deliveryService.createDelivery(secondDeliveryRequest)
            throw AssertionError("Expected second delivery to fail due to overlapping time capacity exceeded")
        } catch (e: IllegalArgumentException) {
            println("✅ Second delivery correctly rejected for overlapping time capacity exceeded: ${e.message}")
            assert(e.message?.contains("Превышена грузоподъемность машины в период") == true) {
                "Expected time-specific capacity error, but got: ${e.message}"
            }
            assert(e.message?.contains("пересекающиеся доставки:") == true) {
                "Expected error message to mention overlapping deliveries, but got: ${e.message}"
            }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `completed deliveries should not affect capacity validation`() {
        val vehicle = createSmallVehicle() // 1000 кг, 10 м³
        val heavyProduct = createHeavyProduct() // 500 кг каждый
        val date = LocalDate.now().plusDays(5)

        // Create first delivery and mark it as completed
        val firstDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date,
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(13, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7558"),
                    longitude = BigDecimal("37.6176"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // 500кг
                        )
                    )
                )
            )
        )

        var firstDeliveryId: Long
        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val firstDelivery = deliveryService.createDelivery(firstDeliveryRequest)
            firstDeliveryId = firstDelivery.id
            println("✅ First delivery created: ${firstDelivery.id} from 9:00-13:00 with 500кг")
        } catch (e: Exception) {
            throw AssertionError("First delivery should have succeeded: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }

        // Mark first delivery as completed (simulate via direct repository access)
        val existingDelivery = deliveryRepository.findById(firstDeliveryId).get()
        val completedDelivery = existingDelivery.copy(
            status = com.example.couriermanagement.entity.DeliveryStatus.completed,
            updatedAt = java.time.LocalDateTime.now()
        )
        deliveryRepository.save(completedDelivery)
        println("✅ Marked first delivery as completed")

        // Create overlapping delivery with 900кг from 12:00-16:00
        // Should succeed because completed delivery doesn't count toward capacity
        val secondDeliveryRequest = DeliveryRequest(
            courierId = courierUser.id,
            vehicleId = vehicle.id,
            deliveryDate = date,
            timeStart = LocalTime.of(12, 0), // Overlaps with completed delivery
            timeEnd = LocalTime.of(16, 0),
            points = listOf(
                DeliveryPointRequest(
                    sequence = 1,
                    latitude = BigDecimal("55.7600"),
                    longitude = BigDecimal("37.6200"),
                    products = listOf(
                        DeliveryProductRequest(
                            productId = heavyProduct.id,
                            quantity = 1 // 500кг - should succeed since completed delivery doesn't count
                        )
                    )
                )
            )
        )

        try {
            val auth = UsernamePasswordAuthenticationToken(managerUser.login, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
            
            val secondDelivery = deliveryService.createDelivery(secondDeliveryRequest)
            println("✅ Second delivery created successfully: ${secondDelivery.id} - completed deliveries ignored")
        } catch (e: Exception) {
            throw AssertionError("Second delivery should have succeeded since completed deliveries don't count, but got: ${e.message}")
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}