package com.example.couriermanagement.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "deliveries")
data class Delivery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    val courier: User?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    val vehicle: Vehicle?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Column(name = "delivery_date", nullable = false)
    val deliveryDate: LocalDate,

    @Column(name = "time_start", nullable = false)
    val timeStart: LocalTime,

    @Column(name = "time_end", nullable = false)
    val timeEnd: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: DeliveryStatus = DeliveryStatus.planned,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "delivery", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val deliveryPoints: List<DeliveryPoint> = emptyList()
)

enum class DeliveryStatus {
    planned, in_progress, completed, cancelled
}