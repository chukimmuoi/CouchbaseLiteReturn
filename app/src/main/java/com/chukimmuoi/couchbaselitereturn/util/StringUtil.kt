package com.chukimmuoi.couchbaselitereturn.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.experimental.and

object StringUtil {

    fun MD5(string: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val inputBytes = string.toByteArray()
            val hashBytes = digest.digest(inputBytes)

            byteArrayToHex(hashBytes)
        } catch (e: NoSuchAlgorithmException) {
            ""
        }
    }

    private fun byteArrayToHex(a: ByteArray): String {
        val sb = StringBuilder(a.size * 2)
        for (b in a) sb.append(String.format("%02x", b and (0xff.toByte())))

        return sb.toString()
    }
}
