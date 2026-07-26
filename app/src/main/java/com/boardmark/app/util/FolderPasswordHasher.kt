package com.boardmark.app.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * フォルダのパスワードをSHA-256+ソルトでハッシュ化する。元のパスワードは保存せず、
 * ロック解除時の一致判定にのみ使う(復元は行わない)。
 */
object FolderPasswordHasher {

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun matches(password: String, salt: String, expectedHash: String): Boolean =
        hash(password, salt) == expectedHash
}
