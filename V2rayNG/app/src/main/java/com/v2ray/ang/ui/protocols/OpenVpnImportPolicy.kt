package com.v2ray.ang.ui.protocols

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** SAF imports are self-contained profiles, never executable scripts or arbitrary local files. */
internal object OpenVpnImportPolicy {
    const val MAX_BYTES = 2 * 1024 * 1024
    fun decode(name: String, bytes: ByteArray): String {
        require(name.endsWith(".ovpn", ignoreCase = true))
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        require('\u0000' !in text)
        var inline: String? = null
        text.lineSequence().forEach { line ->
            val value = line.trim()
            if (inline != null) {
                if (value.equals("</$inline>", ignoreCase = true)) inline = null
            } else if (value.startsWith("<") && !value.startsWith("</")) {
                val tag = value.removePrefix("<").removeSuffix(">").lowercase()
                // <connection> contains real configuration directives, unlike certificate data.
                if (tag != "connection") {
                    require(tag in setOf("ca", "cert", "key", "pkcs12", "tls-auth", "tls-crypt", "tls-crypt-v2", "auth-user-pass", "extra-certs", "crl-verify"))
                    inline = tag
                }
            } else if (value.equals("</connection>", ignoreCase = true)) {
                // End of a validated connection directive block.
            } else if (value.isNotEmpty() && !value.startsWith("#") && !value.startsWith(";")) {
                val parts = value.split(Regex("\\s+"), limit = 2)
                val option = parts[0].removePrefix("--").lowercase()
                // Quoted/escaped directive names must not bypass this file/control policy.
                require(option.matches(Regex("[a-z0-9_-]+")))
                require(option !in setOf("up", "down", "plugin", "route-up", "route-pre-down", "ipchange",
                    "tls-verify", "auth-user-pass-verify", "client-connect", "client-disconnect",
                    "management", "management-client", "log", "log-append", "writepid", "chroot", "daemon",
                    "config", "cd", "askpass", "secret", "dh", "auth-gen-token-secret", "http-proxy-user-pass"))
                if (option in setOf("ca", "cert", "key", "pkcs12", "tls-auth", "tls-crypt", "tls-crypt-v2", "auth-user-pass", "extra-certs", "crl-verify")) {
                    require(parts.size == 1 || parts[1].startsWith("[inline]"))
                }
            }
        }
        require(inline == null)
        return text
    }
}
