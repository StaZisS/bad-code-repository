package com.example.couriermanagement.config

import com.example.couriermanagement.dto.response.ErrorResponse
import com.example.couriermanagement.dto.response.ErrorInfo
import com.example.couriermanagement.dto.response.ValidationErrorResponse
import com.example.couriermanagement.dto.response.ValidationErrorInfo
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            error = ErrorInfo(
                code = "BAD_REQUEST",
                message = ex.message ?: "Bad request"
            )
        )
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            error = ErrorInfo(
                code = "INTERNAL_ERROR",
                message = ex.message ?: "Internal error"
            )
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            error = ErrorInfo(
                code = "FORBIDDEN",
                message = "Access denied"
            )
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val details = ex.bindingResult.allErrors.associate { error ->
            val fieldName = if (error is FieldError) error.field else error.objectName
            fieldName to (error.defaultMessage ?: "Validation error")
        }

        val error = ValidationErrorResponse(
            error = ValidationErrorInfo(
                code = "VALIDATION_FAILED",
                message = "Validation failed",
                details = details
            )
        )
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ResponseEntity<ValidationErrorResponse> {
        val details = ex.bindingResult.allErrors.associate { error ->
            val fieldName = if (error is FieldError) error.field else error.objectName
            fieldName to (error.defaultMessage ?: "Validation error")
        }

        val error = ValidationErrorResponse(
            error = ValidationErrorInfo(
                code = "VALIDATION_FAILED",
                message = "Validation failed",
                details = details
            )
        )
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            error = ErrorInfo(
                code = "INTERNAL_ERROR",
                message = "Internal server error"
            )
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}