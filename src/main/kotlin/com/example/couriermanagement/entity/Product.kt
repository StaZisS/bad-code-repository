package com.example.couriermanagement.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "weight", nullable = false, precision = 8, scale = 3)
    val weight: BigDecimal,

    @Column(name = "length", nullable = false, precision = 6, scale = 2)
    val length: BigDecimal,

    @Column(name = "width", nullable = false, precision = 6, scale = 2)
    val width: BigDecimal,

    @Column(name = "height", nullable = false, precision = 6, scale = 2)
    val height: BigDecimal
) {
    fun getVolume(): BigDecimal {
        return length * width * height / BigDecimal("1000000") // convert cm³ to m³
    }
}