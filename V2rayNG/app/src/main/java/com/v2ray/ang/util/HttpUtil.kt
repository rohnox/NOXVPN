package com.v2ray.ang.util

import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.UrlContentRequest
import okhttp3.Credentials
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

object HttpUtil {

    /**
     * Converts the domain part of a URL string to its IDN (Punycode, ASCII Compatible Encoding) format.
     *
     * For example, a URL like "https://例子.中国/path" will be converted to "https://xn--fsqu00a.xn--fiqs8s/path".
     *
     * @param str The URL string to convert (can contain non-ASCII characters in the domain).
     * @return The URL string with the domain part converted to ASCII-compatible (Punycode) format.
     */
    fun toIdnUrl(str: String): String {
        val url = URL(str)
        val host = url.host
        val asciiHost = IDN.toASCII(url.host, IDN.ALLOW_UNASSIGNED)
        if (host != asciiHost) {
            return str.replace(host, asciiHost)
        } else {
            return str
        }
    }

    /**
     * Converts a Unicode domain name to its IDN (Punycode, ASCII Compatible Encoding) format.
     * If the input is an IP address or already an ASCII domain, returns the original string.
     *
     * @param domain The domain string to convert (can include non-ASCII internationalized characters).
     * @return The domain in ASCII-compatible (Punycode) format, or the original string if input is an IP or already ASCII.
     */
    fun toIdnDomain(domain: String): String {
        // Return as is if it's a pure IP address (IPv4 or IPv6)
        if (Utils.isPureIpAddress(domain)) {
            return domain
        }

        // Return as is if already ASCII (English domain or already punycode)
        if (domain.all { it.code < 128 }) {
            return domain
        }

        // Otherwise, convert to ASCII using IDN
        return IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED)
    }

    /**
     * Resolves a hostname to an IP address, returns original input if it's already an IP
     *
     * @param host The hostname or IP address to resolve
     * @param ipv6Preferred Whether to prefer IPv6 addresses, defaults to false
     * @return The resolved IP address or the original input (if it's already an IP or resolution fails)
     */
    fun resolveHostToIP(host: String, ipv6Preferred: Boolean = false): List<String>? {
        try {
            // If it's already an IP address, return it as a list
            if (Utils.isPureIpAddress(host)) {
                return null
            }

            // Get all IP addresses
            val addresses = InetAddress.getAllByName(host)
            if (addresses.isEmpty()) {
                return null
            }

            // Sort addresses based on preference
            val sortedAddresses = if (ipv6Preferred) {
                addresses.sortedWith(compareByDescending { it is Inet6Address })
            } else {
                addresses.sortedWith(compareBy { it is Inet6Address })
            }

            val ipList = sortedAddresses.mapNotNull { it.hostAddress }

            LogUtil.i(AppConfig.TAG, "Resolved IPs for $host: ${ipList.joinToString()}")

            return ipList
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve host to IP", e)
            return null
        }
    }


    /**
     * Retrieves the content of a URL as a string.
     *
     * @param url The URL to fetch content from.
     * @param timeout The timeout value in milliseconds.
     * @param httpPort The HTTP port to use.
     * @return The content of the URL as a string.
     */
    fun getUrlContent(request: UrlContentRequest): String? {
        val url = request.url ?: return null
        val client = buildOkHttpClient(request.timeout, request.httpPort, request.proxyUsername, request.proxyPassword, followRedirects = true)
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Connection", "close")
        if (request.httpPort != 0 && !request.proxyUsername.isNullOrBlank() && !request.proxyPassword.isNullOrBlank()) {
            requestBuilder.header("Proxy-Authorization", Credentials.basic(request.proxyUsername, request.proxyPassword))
        }
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    LogUtil.w(AppConfig.TAG, "Failed to get URL content, code=${response.code}")
                    return null
                }
                return response.body?.string()
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to get URL content", e)
        }
        return null
    }

    /**
     * Retrieves the content of a URL as a string with a custom User-Agent header.
     *
     * @param url The URL to fetch content from.
     * @param timeout The timeout value in milliseconds.
     * @param httpPort The HTTP port to use.
     * @param onResponseHeaders Receives the headers of the successful response that produced the returned content.
     * @return The content of the URL as a string.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun getUrlContentWithUserAgent(
        request: UrlContentRequest,
        onResponseHeaders: ((Headers) -> Unit)? = null
    ): String {
        var currentUrl = request.url
        var redirects = 0
        val maxRedirects = 3

        while (redirects++ < maxRedirects) {
            if (currentUrl == null) continue
            val client = buildOkHttpClient(request.timeout, request.httpPort, request.proxyUsername, request.proxyPassword, followRedirects = false)
            val finalUserAgent = if (request.userAgent.isNullOrBlank()) {
                "v2rayNG/${BuildConfig.VERSION_NAME}"
            } else {
                request.userAgent
            }
            val requestBuilder = Request.Builder()
                .url(currentUrl)
                .get()
                .header("User-agent", finalUserAgent)
                .header("Connection", "close")

            applyEmbeddedBasicAuthHeader(currentUrl, requestBuilder)


            val headersMap = JsonUtil.parseHeadersToMap(request.requestHeaders)
            for ((key, value) in headersMap) {
                LogUtil.d(AppConfig.TAG, "Adding configured subscription request header")
                try {
                    requestBuilder.header(key, value)
                } catch (_: IllegalArgumentException) {
                }
            }

            if (request.httpPort != 0 && !request.proxyUsername.isNullOrBlank() && !request.proxyPassword.isNullOrBlank()) {
                requestBuilder.header("Proxy-Authorization", Credentials.basic(request.proxyUsername, request.proxyPassword))
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                when {
                    response.isRedirect -> {
                        val location = response.header("Location")
                        if (location.isNullOrEmpty()) {
                            throw IOException("Redirect location not found")
                        }
                        currentUrl = resolveLocation(currentUrl, location)
                        if (currentUrl.isNullOrEmpty()) {
                            throw IOException("Failed to resolve redirect location")
                        }
                        continue
                    }

                    response.isSuccessful -> {
                        onResponseHeaders?.invoke(response.headers)
                        return response.body?.string() ?: ""
                    }

                    else -> {
                        throw IOException("Request failed with status code ${response.code}")
                    }
                }
            }
        }
        throw IOException("Too many redirects")
    }

    /** Bounded JSON metadata request. No redirects or URL/header values are logged. */
    fun getBoundedSubscriptionJson(request: UrlContentRequest): String? {
        val url = request.url ?: return null
        return try {
            val client = buildOkHttpClient(request.timeout, request.httpPort, request.proxyUsername, request.proxyPassword, false)
                .newBuilder().callTimeout(request.timeout.toLong(), TimeUnit.MILLISECONDS).build()
            val builder = Request.Builder().url(url).get().header("Accept", "application/json")
                .header("User-Agent", request.userAgent?.takeIf { it.isNotBlank() } ?: "v2rayNG/" + BuildConfig.VERSION_NAME)
            if (!request.requestHeaders.isNullOrBlank()) {
                for ((key, value) in com.google.gson.JsonParser.parseString(request.requestHeaders).asJsonObject.entrySet()) {
                    if (!value.isJsonNull) builder.header(key, if (value.isJsonPrimitive) value.asString else value.toString())
                }
            }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful || !response.header("Content-Type").orEmpty().contains("json", ignoreCase = true)) return null
                val body = response.body
                if (body.contentLength() > 1_048_576) return null
                body.byteStream().use { input ->
                    val bytes = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > 1_048_576) return null
                        bytes.write(buffer, 0, count)
                    }
                    bytes.toString(Charsets.UTF_8.name())
                }
            }
        } catch (error: Exception) {
            val safe = IOException(error.javaClass.simpleName).apply { stackTrace = error.stackTrace }
            LogUtil.e(AppConfig.TAG, "Subscription history operation=fetch-json", safe)
            null
        }
    }

    private fun applyEmbeddedBasicAuthHeader(rawUrl: String, requestBuilder: Request.Builder) {
        val parsed = runCatching { URL(rawUrl) }.getOrNull() ?: return
        parsed.userInfo?.let { userInfo ->
            val colon = userInfo.indexOf(':')
            val user = runCatching {
                Utils.decodeURIComponent(if (colon >= 0) userInfo.substring(0, colon) else userInfo)
            }.getOrDefault(if (colon >= 0) userInfo.substring(0, colon) else userInfo)
            val pass = runCatching {
                Utils.decodeURIComponent(if (colon >= 0) userInfo.substring(colon + 1) else "")
            }.getOrDefault(if (colon >= 0) userInfo.substring(colon + 1) else "")
            requestBuilder.header("Authorization", Credentials.basic(user, pass))
        }
    }

    /** Strictly uses the daemon's local HTTP proxy. Never retries over the device's direct network. */
    suspend fun getExitIpViaProxy(httpPort: Int, username: String?, password: String?): String? =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            if (httpPort !in 1..65535) {
                continuation.resumeWith(Result.success(null))
                return@suspendCancellableCoroutine
            }
            val call = exitIpClient(httpPort, username, password).newCall(Request.Builder()
                .url("https://api64.ipify.org?format=json").header("Accept", "application/json").get().build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, error: IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.success(null))
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val address = try {
                        response.use {
                            if (!it.isSuccessful || !it.header("Content-Type").orEmpty().contains("json", true)) null
                            else it.body.byteStream().use { input ->
                                val buffer = ByteArray(257)
                                var size = 0
                                while (size < buffer.size) {
                                    val count = input.read(buffer, size, buffer.size - size)
                                    if (count < 0) break
                                    size += count
                                }
                                if (size > 256) null else parseExitIpResponse(String(buffer, 0, size, Charsets.UTF_8))
                            }
                        }
                    } catch (_: Exception) { null }
                    if (continuation.isActive) continuation.resumeWith(Result.success(address))
                }
            })
        }

    internal fun exitIpClient(httpPort: Int, username: String?, password: String?): OkHttpClient {
        require(httpPort in 1..65535)
        return buildOkHttpClient(6000, httpPort, username, password, false).newBuilder()
            .callTimeout(6000, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
    }

    internal fun parseExitIpResponse(text: String): String? {
        if (text.length > 256) return null
        return try {
            val json = com.google.gson.JsonParser.parseString(text).takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val value = json.get("ip")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.trim() ?: return null
            value.takeIf { it.length <= 45 && it.none { c -> c == '%' || c == '[' || c == ']' } && Utils.isPureIpAddress(it) }
        } catch (_: Exception) { null }
    }

    private fun buildOkHttpClient(
        timeout: Int,
        httpPort: Int,
        proxyUsername: String?,
        proxyPassword: String?,
        followRedirects: Boolean
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)

        if (httpPort != 0) {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(LOOPBACK, httpPort)))
            if (!proxyUsername.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
                builder.proxyAuthenticator { _, response ->
                    if (response.request.header("Proxy-Authorization") != null) {
                        null
                    } else {
                        response.request.newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(proxyUsername, proxyPassword))
                            .build()
                    }
                }
            }
        }

        return builder.build()
    }

    private fun resolveLocation(baseUrl: String, raw: String): String? {
        return try {
            val locUri = URI(raw)
            val baseUri = URI(baseUrl)
            val resolved = if (locUri.isAbsolute) locUri else baseUri.resolve(locUri)
            resolved.toURL().toString()
        } catch (_: Exception) {
            try {
                URL(raw).toString()
            } catch (_: MalformedURLException) {
                try {
                    URL(URL(baseUrl), raw).toString()
                } catch (_: MalformedURLException) {
                    null
                }
            }
        }
    }

    fun downloadToFile(
        request: UrlContentRequest,
        targetFile: File
    ): Boolean {
        val url = request.url ?: return false
        val client = buildOkHttpClient(request.timeout, request.httpPort, request.proxyUsername, request.proxyPassword, followRedirects = true)
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Connection", "close")
        if (request.httpPort != 0 && !request.proxyUsername.isNullOrBlank() && !request.proxyPassword.isNullOrBlank()) {
            requestBuilder.header("Proxy-Authorization", Credentials.basic(request.proxyUsername, request.proxyPassword))
        }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    LogUtil.w(AppConfig.TAG, "Failed to download file, code=${response.code}, url=$url")
                    return false
                }
                val body = response.body ?: return false
                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to download file: $url", e)
            false
        }
    }
}
