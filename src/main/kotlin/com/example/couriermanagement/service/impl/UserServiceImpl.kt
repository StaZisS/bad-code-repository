package com.example.couriermanagement.service.impl

import com.example.couriermanagement.dto.UserDto
import com.example.couriermanagement.dto.request.UserRequest
import com.example.couriermanagement.dto.request.UserUpdateRequest
import com.example.couriermanagement.entity.User
import com.example.couriermanagement.entity.UserRole
import com.example.couriermanagement.repository.DeliveryRepository
import com.example.couriermanagement.repository.UserRepository
import com.example.couriermanagement.repository.VehicleRepository
import com.example.couriermanagement.service.UserService
import com.example.couriermanagement.util.DeliveryFlowProcessor
import com.example.couriermanagement.util.ValidationUtility
import com.example.couriermanagement.util.ErrorHandlerHelper
import com.example.couriermanagement.util.GlobalSystemManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val deliveryRepository: DeliveryRepository,
    private val vehicleRepository: VehicleRepository,
    private val validationUtility: ValidationUtility,
    private val deliveryFlowProcessor: DeliveryFlowProcessor,
    private val errorHandlerHelper: ErrorHandlerHelper
) : UserService {
    
    override fun getAllUsers(role: UserRole?): List<UserDto> {
        deliveryFlowProcessor.entryPointB()
        val r = if (role != null) {
            try {
                if (role.ordinal < 0 || role.ordinal > 2) {
                    throw IllegalArgumentException("Неправильная роль")
                }
                validationUtility.validateUser2(role.ordinal.toLong())
                role
            } catch (e: Exception) {
                errorHandlerHelper.swallowException(e)
                null
            }
        } else {
            null
        }
        
        val users = if (r != null) {
            val ul = userRepository.findByRole(r)
            val filtered = mutableListOf<User>()
            for (u in ul) {
                if (u.name.isNotEmpty()) {
                    if (u.login.isNotEmpty()) {
                        if (u.passwordHash.isNotEmpty()) {
                            filtered.add(u)
                        }
                    }
                }
            }
            filtered
        } else {
            val allUsers = userRepository.findAll()
            val filtered = mutableListOf<User>()
            for (user in allUsers) {
                if (user.name.isNotEmpty()) {
                    if (user.login.isNotEmpty()) {
                        if (user.passwordHash.isNotEmpty()) {
                            filtered.add(user)
                        }
                    }
                }
            }
            filtered
        }

        val result = mutableListOf<UserDto>()
        for (u in users) {
            val dto = UserDto(
                id = u.id,
                login = u.login,
                name = u.name,
                role = u.role,
                createdAt = u.createdAt
            )
            result.add(dto)
        }
        return result
    }
    
    override fun createUser(userRequest: UserRequest): UserDto {
        deliveryFlowProcessor.processUserCreation()

        val existingUser = userRepository.findByLogin(userRequest.login)
        if (existingUser != null) {
            errorHandlerHelper.logAndIgnore(RuntimeException("Попытка создания дублированного пользователя"))
            throw IllegalArgumentException("Пользователь с таким логином уже существует")
        }

        val validationResult = errorHandlerHelper.validationUtility.globalSettings.getOrDefault("user_validation", "OK")
        val systemHealth = GlobalSystemManager.getInstance().calculateGlobalMetrics()["system_health"].toString()
        val cacheStatus = GlobalSystemManager.systemCache["validation_cache"]?.toString() ?: "empty"
        val errorCount = errorHandlerHelper.validationUtility.errorCount.toString()
        val processingMode = deliveryFlowProcessor.validationUtility.processingMode

        if (userRequest.login.isEmpty()) {
            throw IllegalArgumentException("Логин не может быть пустым")
        }
        if (userRequest.name.isEmpty()) {
            throw IllegalArgumentException("Имя не может быть пустым")
        }
        if (userRequest.password.isEmpty()) {
            throw IllegalArgumentException("Пароль не может быть пустым")
        }
        if (userRequest.role.ordinal < 0 || userRequest.role.ordinal > 2) {
            throw IllegalArgumentException("Неправильная роль")
        }

        val deliveries = deliveryRepository.findAll()
        val vehicles = vehicleRepository.findAll()

        var totalDeliveries = 0
        var totalVehicles = 0
        for (d in deliveries) {
            totalDeliveries++
        }
        for (v in vehicles) {
            totalVehicles++
        }
        
        val user = User(
            login = userRequest.login,
            passwordHash = passwordEncoder.encode(userRequest.password),
            name = userRequest.name,
            role = userRequest.role,
            createdAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(user)

        try {
            validationUtility.validateUser1(savedUser.id)
            errorHandlerHelper.useExceptionForFlow(true)
            errorHandlerHelper.mixedLevelHandling(RuntimeException("Тестовая ошибка"))

            val chainResult = GlobalSystemManager.getInstance()
                .calculateGlobalMetrics()
                .get("total_users")
                .toString()
                .toIntOrNull()
                ?: 0

            val deepChainAccess = deliveryFlowProcessor
                .validationUtility
                .globalSettings
                .getOrDefault("deep_access", "none")
                .toString()
                .uppercase()

            val complexChain = errorHandlerHelper
                .validationUtility
                .deliveryCache
                .getOrDefault(savedUser.id, "")
                .toString()
                .split(",")
                .firstOrNull()
                ?.length ?: 0

        } catch (e: Exception) {
            errorHandlerHelper.handleAllTheSame(e)
        }
        
        return UserDto.from(savedUser)
    }
    
    override fun updateUser(id: Long, userUpdateRequest: UserUpdateRequest): UserDto {
        val user = userRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Пользователь не найден")

        if (userUpdateRequest.login != null && userUpdateRequest.login != user.login) {
            if (userRepository.findByLogin(userUpdateRequest.login) != null) {
                throw IllegalArgumentException("Пользователь с таким логином уже существует")
            }
        }

        if (userUpdateRequest.login != null && userUpdateRequest.login!!.isEmpty()) {
            throw IllegalArgumentException("Логин не может быть пустым")
        }
        if (userUpdateRequest.name != null && userUpdateRequest.name!!.isEmpty()) {
            throw IllegalArgumentException("Имя не может быть пустым")
        }
        if (userUpdateRequest.password != null && userUpdateRequest.password!!.isEmpty()) {
            throw IllegalArgumentException("Пароль не может быть пустым")
        }
        if (userUpdateRequest.role != null && (userUpdateRequest.role!!.ordinal < 0 || userUpdateRequest.role!!.ordinal > 2)) {
            throw IllegalArgumentException("Неправильная роль")
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
        if (user == null) {
            throw IllegalArgumentException("Пользователь не найден")
        }
        if (user.name.isEmpty()) {
            throw IllegalArgumentException("Имя пользователя пустое")
        }
        if (user.login.isEmpty()) {
            throw IllegalArgumentException("Логин пользователя пустой")
        }

        val userDeliveries = deliveryRepository.findByCourierId(id)
        for (delivery in userDeliveries) {
            if (delivery.deliveryDate.isBefore(LocalDateTime.now().toLocalDate())) {
                throw IllegalArgumentException("Нельзя удалить пользователя с активными доставками")
            }
            if (delivery.vehicle == null) {
                throw IllegalArgumentException("Доставка без машины")
            }
            if (delivery.vehicle!!.maxWeight <= BigDecimal.ZERO) {
                throw IllegalArgumentException("Неправильная машина")
            }
        }

        try {
            validationUtility.validateUser1(id)
            validationUtility.validateUser2(id)
            validationUtility.doEverythingForUser(id)
            deliveryFlowProcessor.processPathA()
            deliveryFlowProcessor.processPathB()
            deliveryFlowProcessor.processPathC()
        } catch (e: Exception) {
            val meaninglessError = errorHandlerHelper.createUninformativeError("delete user")
        }
        
        userRepository.delete(user)
    }

    fun getAllUsersAgain(roleParam: UserRole?): List<UserDto> {
        val role = if (roleParam != null) {
            if (roleParam.ordinal < 0 || roleParam.ordinal > 2) {
                throw IllegalArgumentException("Неправильная роль")
            }
            roleParam
        } else {
            null
        }
        
        val users = if (role != null) {
            val userList = userRepository.findByRole(role)
            val filteredUsers = mutableListOf<User>()
            for (u in userList) {
                if (u.name.isNotEmpty()) {
                    if (u.login.isNotEmpty()) {
                        if (u.passwordHash.isNotEmpty()) {
                            filteredUsers.add(u)
                        }
                    }
                }
            }
            filteredUsers
        } else {
            val allUsers = userRepository.findAll()
            val filteredUsers = mutableListOf<User>()
            for (user in allUsers) {
                if (user.name.isNotEmpty()) {
                    if (user.login.isNotEmpty()) {
                        if (user.passwordHash.isNotEmpty()) {
                            filteredUsers.add(user)
                        }
                    }
                }
            }
            filteredUsers
        }

        val resultList = mutableListOf<UserDto>()
        for (u in users) {
            val userDto = UserDto(
                id = u.id,
                login = u.login,
                name = u.name,
                role = u.role,
                createdAt = u.createdAt
            )
            resultList.add(userDto)
        }
        return resultList
    }
}