package com.example.couriermanagement.repository

import com.example.couriermanagement.entity.Delivery
import com.example.couriermanagement.entity.DeliveryPoint
import com.example.couriermanagement.entity.DeliveryPointProduct
import com.example.couriermanagement.entity.DeliveryStatus
import com.example.couriermanagement.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DeliveryRepository : JpaRepository<Delivery, Long> {
    
    fun findByDeliveryDate(deliveryDate: LocalDate): List<Delivery>
    
    fun findByCourier(courier: User): List<Delivery>
    
    fun findByCourierId(courierId: Long): List<Delivery>
    
    fun findByCourierAndDeliveryDate(courier: User, deliveryDate: LocalDate): List<Delivery>
    
    fun findByDeliveryDateAndCourierId(deliveryDate: LocalDate, courierId: Long): List<Delivery>
    
    fun findByDeliveryDateAndCourierIdAndStatus(deliveryDate: LocalDate, courierId: Long, status: DeliveryStatus): List<Delivery>
    
    fun findByDeliveryDateAndStatus(deliveryDate: LocalDate, status: DeliveryStatus): List<Delivery>
    
    fun findByCourierIdAndStatus(courierId: Long, status: DeliveryStatus): List<Delivery>
    
    fun findByCourierIdAndDeliveryDateBetween(courierId: Long, dateFrom: LocalDate, dateTo: LocalDate): List<Delivery>
    
    fun findByCourierIdAndStatusAndDeliveryDateBetween(courierId: Long, status: DeliveryStatus, dateFrom: LocalDate, dateTo: LocalDate): List<Delivery>
    
    fun findByStatus(status: DeliveryStatus): List<Delivery>

    fun findByVehicleId(vehicleId: Long): List<Delivery>
    
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.courier.id = :courierId 
        AND d.deliveryDate = :date 
        AND d.status IN ('planned', 'in_progress')
    """)
    fun findActiveByCourierAndDate(
        @Param("courierId") courierId: Long,
        @Param("date") date: LocalDate
    ): List<Delivery>
    
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.deliveryDate = :date 
        AND d.status != 'cancelled'
        ORDER BY d.timeStart
    """)
    fun findByDateOrderByTime(@Param("date") date: LocalDate): List<Delivery>
    
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.deliveryDate BETWEEN :startDate AND :endDate
        AND (:courierId IS NULL OR d.courier.id = :courierId)
        AND (:status IS NULL OR d.status = :status)
        ORDER BY d.deliveryDate, d.timeStart
    """)
    fun findByDateRangeAndFilters(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("courierId") courierId: Long?,
        @Param("status") status: DeliveryStatus?
    ): List<Delivery>
    
    @Query("""
        SELECT COUNT(d) > 0 FROM Delivery d 
        WHERE d.courier.id = :courierId 
        AND d.deliveryDate = :date
        AND d.status != 'cancelled'
        AND (
            (d.timeStart <= :timeStart AND d.timeEnd > :timeStart) OR
            (d.timeStart < :timeEnd AND d.timeEnd >= :timeEnd) OR
            (d.timeStart >= :timeStart AND d.timeEnd <= :timeEnd)
        )
    """)
    fun existsCourierTimeConflict(
        @Param("courierId") courierId: Long,
        @Param("date") date: LocalDate,
        @Param("timeStart") timeStart: java.time.LocalTime,
        @Param("timeEnd") timeEnd: java.time.LocalTime
    ): Boolean
    
    @Query("""
        SELECT DISTINCT d FROM Delivery d 
        LEFT JOIN FETCH d.deliveryPoints
        LEFT JOIN FETCH d.vehicle
        WHERE d.courier.id = :courierId
    """)
    fun findByCourierIdWithDetails(@Param("courierId") courierId: Long): List<Delivery>
    
    @Query("""
        SELECT DISTINCT d FROM Delivery d 
        LEFT JOIN FETCH d.deliveryPoints
        LEFT JOIN FETCH d.vehicle
        WHERE d.courier.id = :courierId 
        AND d.deliveryDate = :date
    """)
    fun findByDeliveryDateAndCourierIdWithDetails(
        @Param("date") date: LocalDate, 
        @Param("courierId") courierId: Long
    ): List<Delivery>
    
    @Query("""
        SELECT DISTINCT d FROM Delivery d 
        LEFT JOIN FETCH d.deliveryPoints
        LEFT JOIN FETCH d.vehicle
        WHERE d.courier.id = :courierId 
        AND d.status = :status
    """)
    fun findByCourierIdAndStatusWithDetails(
        @Param("courierId") courierId: Long,
        @Param("status") status: DeliveryStatus
    ): List<Delivery>
    
    @Query("""
        SELECT DISTINCT d FROM Delivery d 
        LEFT JOIN FETCH d.deliveryPoints
        LEFT JOIN FETCH d.vehicle
        WHERE d.courier.id = :courierId 
        AND d.deliveryDate = :date 
        AND d.status = :status
    """)
    fun findByDeliveryDateAndCourierIdAndStatusWithDetails(
        @Param("date") date: LocalDate,
        @Param("courierId") courierId: Long,
        @Param("status") status: DeliveryStatus
    ): List<Delivery>
    
    @Query("""
        SELECT DISTINCT d FROM Delivery d 
        LEFT JOIN FETCH d.deliveryPoints
        LEFT JOIN FETCH d.vehicle
        WHERE d.courier.id = :courierId 
        AND d.deliveryDate BETWEEN :dateFrom AND :dateTo
    """)
    fun findByCourierIdAndDeliveryDateBetweenWithDetails(
        @Param("courierId") courierId: Long,
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate
    ): List<Delivery>
    
    @Query("""
        SELECT DISTINCT d FROM Delivery d 
        LEFT JOIN FETCH d.deliveryPoints
        LEFT JOIN FETCH d.vehicle
        WHERE d.courier.id = :courierId 
        AND d.status = :status
        AND d.deliveryDate BETWEEN :dateFrom AND :dateTo
    """)
    fun findByCourierIdAndStatusAndDeliveryDateBetweenWithDetails(
        @Param("courierId") courierId: Long,
        @Param("status") status: DeliveryStatus,
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate
    ): List<Delivery>
    
    @Query("""
        SELECT DISTINCT dp FROM DeliveryPoint dp
        LEFT JOIN FETCH dp.deliveryPointProducts dpp
        LEFT JOIN FETCH dpp.product p
        WHERE dp.delivery IN :deliveries
        ORDER BY dp.sequence
    """)
    fun loadDeliveryPoint(@Param("deliveries") deliveries: List<Delivery>): List<DeliveryPoint>

    @Query("""
        SELECT DISTINCT dpp FROM DeliveryPointProduct dpp
        LEFT JOIN FETCH dpp.product
        LEFT JOIN FETCH dpp.product p
        WHERE dpp.deliveryPoint IN :deliveryPoints
    """)
    fun loadDeliveryPointsProductsByDeliveryPoint(@Param("deliveryPoints") deliveryPoints: List<DeliveryPoint>): List<DeliveryPointProduct>
    
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.deliveryDate = :date 
        AND d.vehicle.id = :vehicleId
        AND d.status NOT IN ('cancelled', 'completed')
        AND (
            (d.timeStart <= :timeStart AND d.timeEnd > :timeStart) OR
            (d.timeStart < :timeEnd AND d.timeEnd >= :timeEnd) OR
            (d.timeStart >= :timeStart AND d.timeEnd <= :timeEnd)
        )
    """)
    fun findByDateVehicleAndOverlappingTime(
        @Param("date") date: LocalDate, 
        @Param("vehicleId") vehicleId: Long,
        @Param("timeStart") timeStart: java.time.LocalTime,
        @Param("timeEnd") timeEnd: java.time.LocalTime
    ): List<Delivery>

    // нужно по id product получить delivery
    @Query("""
        SELECT d FROM Delivery d 
        JOIN d.deliveryPoints dp 
        JOIN dp.deliveryPointProducts dpp 
        WHERE dpp.product.id = :productId
    """)
    fun findByProductId(@Param("productId") productId: Long): List<Delivery>

}