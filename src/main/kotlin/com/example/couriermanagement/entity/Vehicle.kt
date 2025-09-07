package com.example.couriermanagement.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "vehicles")
data class Vehicle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "brand", nullable = false, length = 100)
    val brand: String,

    @Column(name = "license_plate", unique = true, nullable = false, length = 20)
    val licensePlate: String,

    @Column(name = "max_weight", nullable = false, precision = 8, scale = 2)
    val maxWeight: BigDecimal,

    @Column(name = "max_volume", nullable = false, precision = 8, scale = 3)
    val maxVolume: BigDecimal
)