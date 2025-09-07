package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.UserDto
import com.example.couriermanagement.dto.request.UserRequest
import com.example.couriermanagement.dto.request.UserUpdateRequest
import com.example.couriermanagement.entity.User
import com.example.couriermanagement.entity.UserRole
import com.example.couriermanagement.repository.UserRepository
import com.example.couriermanagement.service.UserService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {
    
    override fun getAllUsers(role: UserRole?): List<UserDto> {
        val users = if (role != null) {
            userRepository.findByRole(role)
        } else {
            userRepository.findAll()
        }
        return users.map { UserDto.from(it) }
    }
    
    override fun createUser(userRequest: UserRequest): UserDto {
        // Check if login already exists
        if (userRepository.findByLogin(userRequest.login) != null) {
            throw IllegalArgumentException("Пользователь с таким логином уже существует")
        }
        
        val user = User(
            login = userRequest.login,
            passwordHash = passwordEncoder.encode(userRequest.password),
            name = userRequest.name,
            role = userRequest.role,
            createdAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(user)
        return UserDto.from(savedUser)
    }
    
    override fun updateUser(id: Long, userUpdateRequest: UserUpdateRequest): UserDto {
        val user = userRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Пользователь не найден")
        
        // Check if new login already exists (if login is being changed)
        if (userUpdateRequest.login != null && userUpdateRequest.login != user.login) {
            if (userRepository.findByLogin(userUpdateRequest.login) != null) {
                throw IllegalArgumentException("Пользователь с таким логином уже существует")
            }
        }
        
        val updatedUser = user.copy(
            login = userUpdateRequest.login ?: user.login,
            name = userUpdateRequest.name ?: user.name,
            role = userUpdateRequest.role ?: user.role,
            passwordHash = if (userUpdateRequest.password != null) {
                passwordEncoder.encode(userUpdateRequest.password)
            } else user.passwordHash
        )
        
        val savedUser = userRepository.save(updatedUser)
        return UserDto.from(savedUser)
    }
    
    override fun deleteUser(id: Long) {
        val user = userRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Пользователь не найден")
        
        // Check if user has active deliveries
        // TODO: Add check for active deliveries when delivery service is implemented
        
        userRepository.delete(user)
    }
}