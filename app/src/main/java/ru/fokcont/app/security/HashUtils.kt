package ru.fokcont.app.security

import java.security.MessageDigest

object HashUtils {

    fun hashPin(pin: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, hash: String): Boolean {
        return hashPin(pin) == hash
    }
}