package com.example.couriermanagement.repository

import com.example.couriermanagement.entity.User
import com.example.couriermanagement.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    fun findByLogin(login: String): User?
    
    fun findByRole(role: UserRole): List<User>
    
    @Query("SELECT u FROM User u WHERE u.role = 'courier'")
    fun findAllCouriers(): List<User>
    
    @Query("SELECT u FROM User u WHERE u.role = 'manager'")
    fun findAllManagers(): List<User>
    
    fun existsByLogin(login: String): Boolean
}