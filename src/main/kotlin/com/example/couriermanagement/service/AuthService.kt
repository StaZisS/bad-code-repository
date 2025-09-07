package com.example.couriermanagement.service

import com.example.couriermanagement.dto.UserDto
import com.example.couriermanagement.dto.request.LoginRequest
import com.example.couriermanagement.dto.response.LoginResponse

interface AuthService {
    fun login(loginRequest: LoginRequest): LoginResponse
    fun getCurrentUser(): UserDto?
}