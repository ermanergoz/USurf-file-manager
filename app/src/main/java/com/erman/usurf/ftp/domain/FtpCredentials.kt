package com.erman.usurf.ftp.domain

import java.util.Random

const val DEFAULT_FTP_USERNAME: String = "usurf"
const val ANONYMOUS_FTP_USERNAME: String = "anonymous"
const val GENERATED_PASSWORD_LENGTH: Int = 12

// Letters and digits without the look-alikes 0, O, 1, l and I, so the password is easy to type on another device.
private const val PASSWORD_ALPHABET: String = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"

data class FtpCredentials(val username: String, val password: String)

/** Decides the credentials an install starts with; pass a `SecureRandom` outside tests. */
class FtpCredentialsInitializer(private val random: Random) {
    fun generatePassword(): String = buildString(GENERATED_PASSWORD_LENGTH) {
        repeat(GENERATED_PASSWORD_LENGTH) { append(PASSWORD_ALPHABET[random.nextInt(PASSWORD_ALPHABET.length)]) }
    }

    /**
     * Keeps what the user set, except a login that could not be protected or used: the anonymous name, which the server
     * never accepts, becomes the default user, and a missing password or one [FtpSettingsRules] refuses (blank, or with
     * a space at either end, which no client could log in with) is replaced by a generated one.
     */
    fun initialCredentials(storedUsername: String?, storedPassword: String?): FtpCredentials {
        val username: String =
            storedUsername?.takeUnless { it.isBlank() || it == ANONYMOUS_FTP_USERNAME } ?: DEFAULT_FTP_USERNAME
        val password: String = storedPassword?.takeUnless { FtpSettingsRules.passwordError(it) != null } ?: generatePassword()
        return FtpCredentials(username = username, password = password)
    }
}
