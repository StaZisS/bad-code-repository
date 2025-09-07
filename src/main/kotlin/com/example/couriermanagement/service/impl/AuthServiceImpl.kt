package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.UserDto
import com.example.couriermanagement.dto.request.LoginRequest
import com.example.couriermanagement.dto.response.LoginResponse
import com.example.couriermanagement.repository.UserRepository
import com.example.couriermanagement.security.JwtUtil
import com.example.couriermanagement.service.AuthService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) : AuthService {
    
    override fun login(loginRequest: LoginRequest): LoginResponse {
        val user = userRepository.findByLogin(loginRequest.login)
            ?: throw IllegalArgumentException("Invalid login or password")
        
        if (!passwordEncoder.matches(loginRequest.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid login or password")
        }
        
        val token = jwtUtil.generateToken(user.login, user.role.name)
        
        return LoginResponse(
            token = token,
            user = UserDto.from(user)
        )
    }
    
    override fun getCurrentUser(): UserDto? {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return null
        
        val username = authentication.name
        val user = userRepository.findByLogin(username)
            ?: return null
        
        return UserDto.from(user)
    }
}