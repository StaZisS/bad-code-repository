import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication(scanBasePackages = ["com.example.couriermanagement"])
class TestRunner

fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext = SpringApplication.run(TestRunner::class.java, *args)
    
    println("Application started successfully!")
    println("Available beans:")
    context.beanDefinitionNames.forEach { name ->
        if (name.contains("controller", ignoreCase = true) || 
            name.contains("service", ignoreCase = true) ||
            name.contains("jwt", ignoreCase = true)) {
            println("  - $name")
        }
    }

    try {
        val jwtUtil = context.getBean("jwtUtil")
        println("JWT util bean found: $jwtUtil")
    } catch (e: Exception) {
        println("JWT util not found: ${e.message}")
    }
    
    context.close()
}