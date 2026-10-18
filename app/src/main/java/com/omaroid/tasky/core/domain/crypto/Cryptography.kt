package com.omaroid.tasky.core.domain.crypto

interface Cryptography {
    suspend fun encrypt(plainText: String): String
    suspend fun decrypt(cipherText: String): String
}