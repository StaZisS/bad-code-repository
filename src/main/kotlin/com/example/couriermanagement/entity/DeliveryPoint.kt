package com.example.couriermanagement.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "delivery_points",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["delivery_id", "sequence"])
    ]
)
data class DeliveryPoint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    val delivery: Delivery,

    @Column(name = "sequence", nullable = false)
    val sequence: Int,

    @Column(name = "latitude", precision = 10, scale = 8, nullable = false)
    val latitude: BigDecimal,

    @Column(name = "longitude", precision = 11, scale = 8, nullable = false)
    val longitude: BigDecimal,

    @OneToMany(mappedBy = "deliveryPoint", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val deliveryPointProducts: List<DeliveryPointProduct> = emptyList()
)