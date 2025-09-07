package com.example.couriermanagement.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil {

    @Value("\${jwt.secret:mySecretKey123456789012345678901234567890}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400000}") // 24 hours in milliseconds
    private var expiration: Long = 86400000

    private fun getSigningKey(): SecretKey {
        val keyBytes = if (secret.length >= 32) {
            secret.toByteArray()
        } else {
            secret.padEnd(32, '0').toByteArray()
        }
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(username: String, role: String): String {
        val claims = HashMap<String, Any>()
        claims["role"] = role
        return createToken(claims, username)
    }

    private fun createToken(claims: Map<String, Any>, subject: String): String {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact()
    }

    fun validateToken(token: String, username: String): Boolean {
        val extractedUsername = extractUsername(token)
        return extractedUsername == username && !isTokenExpired(token)
    }

    fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    fun extractRole(token: String): String = extractClaim(token) { it["role"] as String }

    fun extractExpiration(token: String): Date = extractClaim(token, Claims::getExpiration)

    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun isTokenExpired(token: String): Boolean = extractExpiration(token).before(Date())
}