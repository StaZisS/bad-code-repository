package com.example.couriermanagement.controller

import com.example.couriermanagement.dto.ProductDto
import com.example.couriermanagement.dto.request.ProductRequest
import com.example.couriermanagement.service.ProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Управление товарами (админ)")
@SecurityRequirement(name = "bearerAuth")
class ProductController(
    private val productService: ProductService
) {

    @GetMapping
    @Operation(
        summary = "Получить список всех товаров",
        description = "Получение списка всех товаров в системе"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Список товаров"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    fun getAllProducts(): ResponseEntity<List<ProductDto>> {
        val products = productService.getAllProducts()
        return ResponseEntity.ok(products)
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(
        summary = "Создать новый товар",
        description = "Создание нового товара. Доступно только для админа"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Товар создан"),
            ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    fun createProduct(@Valid @RequestBody productRequest: ProductRequest): ResponseEntity<ProductDto> {
        val product = productService.createProduct(productRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(product)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(
        summary = "Обновить данные товара",
        description = "Обновление данных товара. Доступно только для админа"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Товар обновлен"),
            ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            ApiResponse(responseCode = "404", description = "Товар не найден")
        ]
    )
    fun updateProduct(
        @Parameter(description = "ID товара", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody productRequest: ProductRequest
    ): ResponseEntity<ProductDto> {
        val product = productService.updateProduct(id, productRequest)
        return ResponseEntity.ok(product)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(
        summary = "Удалить товар",
        description = "Удаление товара из системы. Доступно только для админа"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Товар удален"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            ApiResponse(responseCode = "404", description = "Товар не найден")
        ]
    )
    fun deleteProduct(
        @Parameter(description = "ID товара", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}