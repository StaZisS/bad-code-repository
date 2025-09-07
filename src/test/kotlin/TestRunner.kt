import com.example.couriermanagement.*
import com.example.couriermanagement.dto.request.LoginRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.lang.System.exit

fun main() {
    // Set encoding properties
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("console.encoding", "UTF-8")
    System.setProperty("java.awt.headless", "true")
    
    println("🚀 Starting Test Runner...")
    
    System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
    System.setProperty("spring.jpa.hibernate.ddl-auto", "validate")
    System.setProperty("spring.liquibase.enabled", "true")
    System.setProperty("spring.profiles.active", "test")
    
    val context: ConfigurableApplicationContext = SpringApplication.run(CourierManagementSystemApplication::class.java)
    
    try {
        runTests(context)
        println("\n✅ All tests passed!")
        exit(0)
    } catch (e: Exception) {
        println("\n❌ Tests failed: ${e.message}")
        e.printStackTrace()
        exit(1)
    } finally {
        context.close()
    }
}

fun runTests(context: ConfigurableApplicationContext) {
    // Cast the context to WebApplicationContext 
    val webContext = context as org.springframework.web.context.WebApplicationContext
    
    val mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build()
    val objectMapper = context.getBean(ObjectMapper::class.java)
    
    println("\n🧪 Running Authentication Tests...")
    
    // Test 1: Valid admin login
    testValidAdminLogin(mockMvc, objectMapper)
    
    // Test 2: Invalid login
    testInvalidLogin(mockMvc, objectMapper)
    
    // Test 3: Empty credentials
    testEmptyCredentials(mockMvc, objectMapper)
    
    println("📊 All authentication tests passed!")
    
    println("\n🧪 Running Courier Endpoint Tests...")
    testCourierEndpoints(context, mockMvc, objectMapper)
    println("📊 All courier endpoint tests completed!")
}

fun testValidAdminLogin(mockMvc: MockMvc, objectMapper: ObjectMapper) {
    println("  ✅ Testing valid admin login...")
    
    val loginRequest = LoginRequest(login = "admin", password = "admin123")
    
    val result = mockMvc.perform(
        post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest))
    )
    
    val response = result.andReturn().response
    println("    Response status: ${response.status}")
    println("    Response content: ${response.contentAsString}")
    
    if (response.status == 200) {
        result
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.login").value("admin"))
            .andExpect(jsonPath("$.user.role").value("admin"))
        println("    ✅ Admin login successful")
    } else {
        println("    ❌ Admin login failed with status ${response.status}")
        throw RuntimeException("Admin login failed: ${response.contentAsString}")
    }
}

fun testInvalidLogin(mockMvc: MockMvc, objectMapper: ObjectMapper) {
    println("  ✅ Testing invalid login...")
    
    val loginRequest = LoginRequest(login = "admin", password = "wrongpassword")
    
    val result = mockMvc.perform(
        post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest))
    )
    
    result.andExpect(status().isBadRequest())
    println("    ✅ Invalid login correctly rejected")
}

fun testEmptyCredentials(mockMvc: MockMvc, objectMapper: ObjectMapper) {
    println("  ✅ Testing empty credentials...")
    
    val loginRequest = LoginRequest(login = "", password = "admin123")
    
    val result = mockMvc.perform(
        post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest))
    )
    
    result.andExpect(status().isBadRequest())
    println("    ✅ Empty login correctly rejected")
}

fun testCourierEndpoints(context: ConfigurableApplicationContext, mockMvcOriginal: MockMvc, objectMapper: ObjectMapper) {
    println("  ✅ Testing courier endpoints...")
    
    // Use the original MockMvc to ensure proper configuration
    val mockMvc = mockMvcOriginal
    
    try {
        // First login as admin to get token
        val adminLoginRequest = LoginRequest(login = "admin", password = "admin123")
        val adminResult = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLoginRequest))
        )
        
        val adminResponseJson = adminResult.andReturn().response.contentAsString
        val adminData = objectMapper.readTree(adminResponseJson as String)
        val adminToken = adminData.get("token").asText()
        
        println("    Admin login successful")
        
        // Create a test courier user using JPA repositories
        val userRepository = context.getBean(com.example.couriermanagement.repository.UserRepository::class.java)
        val passwordEncoder = context.getBean(org.springframework.security.crypto.password.PasswordEncoder::class.java)
        
        val courierUser = userRepository.save(
            com.example.couriermanagement.entity.User(
                login = "testcourier",
                passwordHash = passwordEncoder.encode("password"),
                name = "Test Courier",
                role = com.example.couriermanagement.entity.UserRole.courier,
                createdAt = java.time.LocalDateTime.now()
            )
        )
        
        // Login as courier to get courier token
        val courierLoginRequest = LoginRequest(login = "testcourier", password = "password")
        val courierResult = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courierLoginRequest))
        )
        
        val courierResponseJson = courierResult.andReturn().response.contentAsString
        val courierData = objectMapper.readTree(courierResponseJson as String)
        val courierToken = courierData.get("token").asText()
        
        println("    Courier login successful")
        
        // Test the courier deliveries endpoint
        println("    Testing /courier/deliveries endpoint...")
        
        val result = mockMvc.perform(
            get("/courier/deliveries")
                .header("Authorization", "Bearer $courierToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
        
        val response = result.andReturn().response
        println("    Response status: ${response.status}")
        println("    Response content: ${response.contentAsString}")
        
        if (response.status >= 500) {
            println("    ❌ 500 Error detected!")
            throw RuntimeException("500 error on /courier/deliveries: ${response.contentAsString}")
        } else {
            println("    ✅ Request completed with status: ${response.status}")
        }
        
    } catch (e: Exception) {
        println("    ❌ Exception during courier endpoint test: ${e.message}")
        e.printStackTrace()
        throw e
    }
}