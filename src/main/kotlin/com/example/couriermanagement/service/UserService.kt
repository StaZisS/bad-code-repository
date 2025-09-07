package com.example.couriermanagement.service

import com.example.couriermanagement.dto.UserDto
import com.example.couriermanagement.dto.request.UserRequest
import com.example.couriermanagement.dto.request.UserUpdateRequest
import com.example.couriermanagement.entity.UserRole

interface UserService {
    fun getAllUsers(role: UserRole?): List<UserDto>
    fun createUser(userRequest: UserRequest): UserDto
    fun updateUser(id: Long, userUpdateRequest: UserUpdateRequest): UserDto
    fun deleteUser(id: Long)
}