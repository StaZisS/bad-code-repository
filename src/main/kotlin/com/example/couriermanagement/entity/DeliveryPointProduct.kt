package com.example.couriermanagement.entity

import jakarta.persistence.*

@Entity
@Table(name = "delivery_point_products")
data class DeliveryPointProduct(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_point_id", nullable = false)
    val deliveryPoint: DeliveryPoint,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "quantity", nullable = false)
    val quantity: Int
)