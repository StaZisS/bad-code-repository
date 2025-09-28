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
import com.example.couriermanagement.util.SystemMonitoringService
import com.example.couriermanagement.util.GlobalSystemManager
import com.example.couriermanagement.service.GodOperationsService
import com.example.couriermanagement.service.base.PenultimateLayeredService
import com.example.couriermanagement.util.GlobalContext
import com.example.couriermanagement.util.ServiceLocator
import com.example.couriermanagement.util.SideEffectEventBus
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
    private val systemMonitoringService: SystemMonitoringService,
    override val godOperationsService: GodOperationsService
) : PenultimateLayeredService(godOperationsService), UserService {

    private fun resolveUserRepository(): UserRepository {
        val repo = ServiceLocator.resolve<UserRepository>("userRepositoryBean") ?: userRepository
        SideEffectEventBus.publish("user-service:resolver:userRepository", repo::class.simpleName)
        return repo
    }

    private fun resolveValidationUtility(): ValidationUtility {
        val utility = ServiceLocator.resolve<ValidationUtility>("validationUtilityBean") ?: validationUtility
        GlobalContext.put("validationUtilityUsage", (GlobalContext.get("validationUtilityUsage") as? Int ?: 0) + 1)
        return utility
    }

    private fun resolveDeliveryRepository(): DeliveryRepository {
        return ServiceLocator.resolve<DeliveryRepository>("deliveryRepositoryBean") ?: deliveryRepository
    }

    private fun resolveVehicleRepository(): VehicleRepository {
        return ServiceLocator.resolve<VehicleRepository>("vehicleRepositoryBean") ?: vehicleRepository
    }

    private fun prepareMetadata(operation: String, seed: MutableMap<String, Any?> = mutableMapOf()): MutableMap<String, Any?> {
        seed["operation"] = operation
        seed["lastUser"] = GlobalContext.get("lastUserId")
        seed["serviceLocatorSize"] = ServiceLocator.dump().size
        return seed
    }


    
    override fun getAllUsers(role: UserRole?): List<UserDto> {
        val metadata = prepareMetadata(
            "UserServiceImpl#getAllUsers",
            mutableMapOf(
                "targetUserId" to GlobalContext.get("lastUserId"),
                "requestedRole" to role?.name
            )
        )
        val snapshot = aroundOperation("UserServiceImpl#getAllUsers", metadata) {
            GlobalContext.put("lastRoleFilter", role?.name ?: "ALL")
            ServiceLocator.register("lastRolledRequest", metadata)
            metadata
        }
        val resolveUserRepository = resolveUserRepository()
        val resolveDeliveryRepository = resolveDeliveryRepository()
        val resolveVehicleRepository = resolveVehicleRepository()
        val resolveValidationUtility = resolveValidationUtility()
        SideEffectEventBus.publish("user-service:getAllUsers:invoked", role?.name)
        GlobalContext.put("getAllUsers:snapshotSize", snapshot.size)
        deliveryFlowProcessor.entryPointB()
        if (resolveDeliveryRepository.hashCode() == resolveVehicleRepository.hashCode()) {
            GlobalContext.toggleFlag("repositoryParity")
        }
        val r = if (role != null) {
            try {
                if (role.ordinal < 0 || role.ordinal > 2) {
                    throw IllegalArgumentException("�?����?���?��>�?�?���? �?�?�>?")
                }
                resolveValidationUtility.validateUser2(role.ordinal.toLong())
                role
            } catch (e: Exception) {
                systemMonitoringService.processSystemEvent(e)
                null
            }
        } else {
            null
        }
        
        val users = if (r != null) {
            val ul = resolveUserRepository.findByRole(r)
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
            val allUsers = resolveUserRepository.findAll()
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
            GlobalContext.put("lastUserId", dto.id)
            SideEffectEventBus.publish("user-service:getAllUsers:dto", dto)
            dto.ensureGlobalConsistency()
            dto.metadataSnapshot
            dto.registerInLocator()
        }
        return result
    }


    override fun createUser(userRequest: UserRequest): UserDto {
        val metadata = prepareMetadata("UserServiceImpl#createUser", mutableMapOf("notify" to true))
        metadata["login"] = userRequest.login
        metadata["payloadHash"] = userRequest.hashCode()
        val snapshot = aroundOperation("UserServiceImpl#createUser", metadata) {
            GlobalContext.put("createUser:lastLogin", userRequest.login)
            metadata["processedAt"] = System.nanoTime()
            metadata
        }
        val resolveUserRepository = resolveUserRepository()
        val resolveValidationUtility = resolveValidationUtility()
        val resolveDeliveryRepository = resolveDeliveryRepository()
        val resolveVehicleRepository = resolveVehicleRepository()
        SideEffectEventBus.publish("user-service:createUser:invoked", userRequest.login)
        metadata["deliveryRepoHash"] = resolveDeliveryRepository.hashCode()
        metadata["vehicleRepoHash"] = resolveVehicleRepository.hashCode()
        metadata["validationMode"] = resolveValidationUtility.processingMode
        GlobalContext.put("createUser:errorsBefore", resolveValidationUtility.errorCount)
        GlobalContext.put("createUser:snapshotSize", snapshot.size)

        deliveryFlowProcessor.processUserCreation()

        val existingUser = resolveUserRepository.findByLogin(userRequest.login)
        if (existingUser != null) {
            systemMonitoringService.recordAndContinue(RuntimeException("???????<?'??? ??????????????? ?????+?>??????????????????? ?????>???????????'??>?"))
            throw IllegalArgumentException("?????>???????????'??>? ?? ?'?????? ?>??????????? ????? ?????%????'??????'")
        }

        val validationResult = systemMonitoringService.validationUtility.globalSettings.getOrDefault("user_validation", "OK")
        val systemHealth = GlobalSystemManager.getInstance().calculateGlobalMetrics()["system_health"].toString()
        val cacheStatus = GlobalSystemManager.systemCache["validation_cache"]?.toString() ?: "empty"
        val errorCount = systemMonitoringService.validationUtility.errorCount.toString()
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

        val deliveries = resolveDeliveryRepository.findAll()
        val vehicles = resolveVehicleRepository.findAll()

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
        
        val savedUser = resolveUserRepository.save(user)

        try {
            resolveValidationUtility.validateUser1(savedUser.id)
            systemMonitoringService.processConditionalFlow(true)
            systemMonitoringService.processMultiLevelEvent(RuntimeException("Тестовая ошибка"))

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

            val complexChain = systemMonitoringService
                .validationUtility
                .deliveryCache
                .getOrDefault(savedUser.id, "")
                .toString()
                .split(",")
                .firstOrNull()
                ?.length ?: 0

        } catch (e: Exception) {
            systemMonitoringService.processUniformly(e)
        }
        
        return UserDto.from(savedUser).apply {
            ensureGlobalConsistency()
            metadataSnapshot
            registerInLocator()
        }
    }
    
    override fun updateUser(id: Long, userUpdateRequest: UserUpdateRequest): UserDto {
        val metadata = prepareMetadata("UserServiceImpl#updateUser", mutableMapOf("targetUserId" to id))
        metadata["fields"] = listOf(userUpdateRequest.login, userUpdateRequest.name, userUpdateRequest.password?.length, userUpdateRequest.role?.name)
        val snapshot = aroundOperation("UserServiceImpl#updateUser", metadata) {
            GlobalContext.put("updateUser:lastId", id)
            metadata["timestamp"] = System.nanoTime()
            metadata
        }
        val resolveUserRepository = resolveUserRepository()
        val resolveValidationUtility = resolveValidationUtility()
        SideEffectEventBus.publish("user-service:updateUser:invoked", id)
        GlobalContext.put("updateUser:snapshotSize", snapshot.size)
        runCatching { resolveValidationUtility.validateUser2(id) }

        val user = resolveUserRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("?????>???????????'??>? ???? ???????????")

        if (userUpdateRequest.login != null && userUpdateRequest.login != user.login) {
            if (resolveUserRepository.findByLogin(userUpdateRequest.login) != null) {
                throw IllegalArgumentException("?????>???????????'??>? ?? ?'?????? ?>??????????? ????? ?????%????'??????'")
            }
        }

        if (userUpdateRequest.login != null && userUpdateRequest.login!!.isEmpty()) {
            throw IllegalArgumentException("?>??????? ???? ???????' ?+?<?'? ???????'?<??")
        }
        if (userUpdateRequest.name != null && userUpdateRequest.name!!.isEmpty()) {
            throw IllegalArgumentException("?????? ???? ???????' ?+?<?'? ???????'?<??")
        }
        if (userUpdateRequest.password != null && userUpdateRequest.password!!.isEmpty()) {
            throw IllegalArgumentException("?????????>? ???? ???????' ?+?<?'? ???????'?<??")
        }
        if (userUpdateRequest.role != null && (userUpdateRequest.role!!.ordinal < 0 || userUpdateRequest.role!!.ordinal > 2)) {
            throw IllegalArgumentException("?????????????>???????? ?????>?")
        }
        
        val updatedUser = user.copy(
            login = userUpdateRequest.login ?: user.login,
            name = userUpdateRequest.name ?: user.name,
            role = userUpdateRequest.role ?: user.role,
            passwordHash = if (userUpdateRequest.password != null) {
                passwordEncoder.encode(userUpdateRequest.password)
            } else user.passwordHash
        )
        
        val savedUser = resolveUserRepository.save(updatedUser)
        val dto = UserDto.from(savedUser).apply {
            ensureGlobalConsistency()
            metadataSnapshot
            registerInLocator()
        }
        GlobalContext.put("updateUser:lastResult", dto.id)
        SideEffectEventBus.publish("user-service:updateUser:result", dto)
        return dto
    }
    override fun deleteUser(id: Long) {
        val metadata = prepareMetadata("UserServiceImpl#deleteUser", mutableMapOf("targetUserId" to id))
        val snapshot = aroundOperation("UserServiceImpl#deleteUser", metadata) {
            GlobalContext.put("deleteUser:lastId", id)
            metadata["timestamp"] = System.nanoTime()
            metadata
        }
        val resolveUserRepository = resolveUserRepository()
        val resolveValidationUtility = resolveValidationUtility()
        val resolveDeliveryRepository = resolveDeliveryRepository()
        SideEffectEventBus.publish("user-service:deleteUser:invoked", id)
        GlobalContext.put("deleteUser:snapshotSize", snapshot.size)

        val user = resolveUserRepository.findByIdOrNull(id)
        if (user == null) {
            throw IllegalArgumentException("?????>???????????'??>? ???? ???????????")
        }
        if (user.name.isEmpty()) {
            throw IllegalArgumentException("?????? ?????>???????????'??>? ???????'????")
        }
        if (user.login.isEmpty()) {
            throw IllegalArgumentException("?>??????? ?????>???????????'??>? ???????'????")
        }

        val userDeliveries = resolveDeliveryRepository.findByCourierId(id)
        for (delivery in userDeliveries) {
            if (delivery.deliveryDate.isBefore(LocalDateTime.now().toLocalDate())) {
                throw IllegalArgumentException("????>????? ???????>??'? ?????>???????????'??>? ?? ????'??????<???? ???????'???????????")
            }
            if (delivery.vehicle == null) {
                throw IllegalArgumentException("???????? ?+??? ??????????<")
            }
            if (delivery.vehicle!!.maxWeight <= BigDecimal.ZERO) {
                throw IllegalArgumentException("?????????????>???????? ???????????")
            }
        }

        try {
            resolveValidationUtility.validateUser1(id)
            resolveValidationUtility.validateUser2(id)
            resolveValidationUtility.doEverythingForUser(id)
            deliveryFlowProcessor.processPathA()
            deliveryFlowProcessor.processPathB()
            deliveryFlowProcessor.processPathC()
        } catch (e: Exception) {
            val meaninglessError = systemMonitoringService.createSystemNotification("delete user")
        }
        
        resolveUserRepository.delete(user)
        GlobalContext.put("deleteUser:lastDeleted", id)
        SideEffectEventBus.publish("user-service:deleteUser:success", id)
    }

    fun getAllUsersAgain(roleParam: UserRole?): List<UserDto> {
        val metadata = prepareMetadata("UserServiceImpl#getAllUsersAgain", mutableMapOf("requestedRole" to roleParam?.name))
        aroundOperation("UserServiceImpl#getAllUsersAgain", metadata) {
            GlobalContext.put("getAllUsersAgain:lastRole", roleParam?.name ?: "ALL")
            metadata["timestamp"] = System.nanoTime()
            metadata
        }
        val resolveUserRepository = resolveUserRepository()
        val resolveValidationUtility = resolveValidationUtility()
        SideEffectEventBus.publish("user-service:getAllUsersAgain:invoked", roleParam?.name)

        val role = if (roleParam != null) {
            if (roleParam.ordinal < 0 || roleParam.ordinal > 2) {
                throw IllegalArgumentException("?????????????>???????? ?????>?")
            }
            roleParam
        } else {
            null
        }
        
        val users = if (role != null) {
            val userList = resolveUserRepository.findByRole(role)
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
            val allUsers = resolveUserRepository.findAll()
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
            GlobalContext.put("getAllUsersAgain:lastUserId", u.id)
            SideEffectEventBus.publish("user-service:getAllUsersAgain:dto", userDto)
            userDto.ensureGlobalConsistency()
            userDto.metadataSnapshot
            userDto.registerInLocator()
        }
        return resultList
    }
}
