package com.example.data.security

import java.net.InetAddress
import java.net.URI

/**
 * Validates user-supplied and external stream URLs to block unsafe schemes
 * (file://, javascript:, content://) and private/loopback/metadata endpoints.
 */
object UrlSecurityValidator {

    private val blockedHosts = setOf(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "169.254.169.254",
        "::1"
    )

    fun validateStreamUrl(rawUrl: String): Result<String> {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Stream URL cannot be empty."))
        }

        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("Malformed stream URL."))

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return Result.failure(
                IllegalArgumentException("Only http:// and https:// stream URLs are permitted.")
            )
        }

        val host = uri.host?.lowercase()?.trim()
            ?: return Result.failure(IllegalArgumentException("Stream URL must include a valid host."))

        if (host in blockedHosts || host.endsWith(".local")) {
            return Result.failure(IllegalArgumentException("Local or restricted hosts are not permitted."))
        }

        // Guard against direct private IP literal injection when numeric
        if (isPrivateIpLiteral(host)) {
            return Result.failure(IllegalArgumentException("Private network addresses are not permitted."))
        }

        return Result.success(trimmed)
    }

    private fun isPrivateIpLiteral(host: String): Boolean {
        val isNumericIpv4 = host.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))
        if (!isNumericIpv4 && !host.contains(":")) return false
        return runCatching {
            val addr = InetAddress.getByName(host)
            addr.isLoopbackAddress || addr.isLinkLocalAddress || addr.isSiteLocalAddress
        }.getOrDefault(false)
    }
}
