package com.example.couriermanagement

import com.example.couriermanagement.entity.*
import com.example.couriermanagement.repository.*
import com.example.couriermanagement.security.JwtUtil
import com.example.couriermanagement.service.OpenStreetMapService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.transaction.annotation.Transactional
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.ArgumentMatchers.any
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml"
    ]
)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = true)
@Transactional
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var webApplicationContext: org.springframework.web.context.WebApplicationContext

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var jwtUtil: JwtUtil

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var vehicleRepository: VehicleRepository

    @Autowired
    protected lateinit var productRepository: ProductRepository

    @Autowired
    protected lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    protected lateinit var deliveryPointRepository: DeliveryPointRepository

    @Autowired
    protected lateinit var deliveryPointProductRepository: DeliveryPointProductRepository

    @MockBean
    protected lateinit var openStreetMapService: OpenStreetMapService

    protected lateinit var adminUser: User
    protected lateinit var managerUser: User
    protected lateinit var courierUser: User
    protected lateinit var adminToken: String
    protected lateinit var managerToken: String
    protected lateinit var courierToken: String

    @BeforeEach
    fun setUp() {
        // MockMvc теперь автоматически конфигурируется с @AutoConfigureMockMvc
        setupUsers()
        setupTokens()
        setupOpenStreetMapMocks()
    }

    private fun setupUsers() {
        // Get admin user from Liquibase setup
        adminUser = userRepository.findByLogin("admin") ?: throw RuntimeException("Admin user not found")
        
        // Create test manager and courier users
        managerUser = userRepository.save(
            User(
                login = "manager",
                passwordHash = passwordEncoder.encode("password"),
                name = "Менеджер",
                role = UserRole.manager,
                createdAt = LocalDateTime.now()
            )
        )

        courierUser = userRepository.save(
            User(
                login = "courier",
                passwordHash = passwordEncoder.encode("password"),
                name = "Курьер",
                role = UserRole.courier,
                createdAt = LocalDateTime.now()
            )
        )
    }

    private fun setupTokens() {
        adminToken = jwtUtil.generateToken(adminUser.login, adminUser.role.name)
        managerToken = jwtUtil.generateToken(managerUser.login, managerUser.role.name)
        courierToken = jwtUtil.generateToken(courierUser.login, courierUser.role.name)
    }

    private fun setupOpenStreetMapMocks() {
        // Mock long distance route (Moscow to St. Petersburg - ~635 km)
        `when`(openStreetMapService.calculateDistance(
            BigDecimal("55.7558"),
            BigDecimal("37.6176"),
            BigDecimal("59.9311"),
            BigDecimal("30.3609")
        )).thenReturn(BigDecimal("635.0"))
        
        // Mock short distance route (within Moscow - ~2.5 km)
        `when`(openStreetMapService.calculateDistance(
            BigDecimal("55.7558"),
            BigDecimal("37.6176"),
            BigDecimal("55.7600"),
            BigDecimal("37.6200")
        )).thenReturn(BigDecimal("2.5"))
        
        // Mock default distance for createDelivery() method coordinates
        `when`(openStreetMapService.calculateDistance(
            BigDecimal("55.7558"),
            BigDecimal("37.6176"),
            BigDecimal("55.7558"),
            BigDecimal("37.6176")
        )).thenReturn(BigDecimal("0.1")) // Very short distance for same coordinates

        `when`(openStreetMapService.calculateDistance(
            BigDecimal("55.7600"),
            BigDecimal("37.6200"),
            BigDecimal("55.7700"),
            BigDecimal("37.6300")
        )).thenReturn(BigDecimal("0.1")) // Very short distance for same coordinates
    }

    protected fun createVehicle(): Vehicle {
        return vehicleRepository.save(
            Vehicle(
                brand = "Ford Transit",
                licensePlate = "А123БВ",
                maxWeight = BigDecimal("1000.0"),
                maxVolume = BigDecimal("15.0")
            )
        )
    }

    protected fun createProduct(): Product {
        return productRepository.save(
            Product(
                name = "Тестовый товар",
                weight = BigDecimal("1.5"),
                length = BigDecimal("10.0"),
                width = BigDecimal("10.0"),
                height = BigDecimal("10.0")
            )
        )
    }

    protected fun createDelivery(courier: User = courierUser, vehicle: Vehicle = createVehicle()): Delivery {
        val delivery = deliveryRepository.save(
            Delivery(
                courier = courier,
                vehicle = vehicle,
                createdBy = managerUser,
                deliveryDate = LocalDate.now().plusDays(5),
                timeStart = LocalTime.of(9, 0),
                timeEnd = LocalTime.of(18, 0),
                status = DeliveryStatus.planned,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        // Create delivery point with product
        val product = createProduct()
        val deliveryPoint = deliveryPointRepository.save(
            DeliveryPoint(
                delivery = delivery,
                sequence = 1,
                latitude = BigDecimal("55.7558"),
                longitude = BigDecimal("37.6176")
            )
        )

        deliveryPointProductRepository.save(
            DeliveryPointProduct(
                deliveryPoint = deliveryPoint,
                product = product,
                quantity = 2
            )
        )

        return delivery
    }

    protected fun ResultActions.expectSuccess() = this.andExpect { result ->
        assert(result.response.status in 200..299) { 
            "Expected success status, but got ${result.response.status}: ${result.response.contentAsString}" 
        }
    }

    protected fun ResultActions.expectBadRequest() = this.andExpect { result ->
        assert(result.response.status == 400) { 
            "Expected 400, but got ${result.response.status}: ${result.response.contentAsString}" 
        }
    }

    protected fun ResultActions.expectUnauthorized() = this.andExpect { result ->
        assert(result.response.status == 401) { 
            "Expected 401, but got ${result.response.status}: ${result.response.contentAsString}" 
        }
    }

    protected fun ResultActions.expectForbidden() = this.andExpect { result ->
        assert(result.response.status == 403) { 
            "Expected 403, but got ${result.response.status}: ${result.response.contentAsString}" 
        }
    }

    protected fun ResultActions.expectNotFound() = this.andExpect { result ->
        assert(result.response.status == 404) { 
            "Expected 404, but got ${result.response.status}: ${result.response.contentAsString}" 
        }
    }

    protected fun postJson(url: String, body: Any, token: String? = null) =
        (if (token != null) {
            post(url).header("Authorization", "Bearer $token")
        } else {
            post(url)
        })
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .let { mockMvc.perform(it) }

    protected fun putJson(url: String, body: Any, token: String) =
        put(url)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .let { mockMvc.perform(it) }

    protected fun getWithAuth(url: String, token: String) =
        get(url)
            .header("Authorization", "Bearer $token")
            .let { mockMvc.perform(it) }

    protected fun deleteWithAuth(url: String, token: String) =
        delete(url)
            .header("Authorization", "Bearer $token")
            .let { mockMvc.perform(it) }
}