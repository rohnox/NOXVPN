package com.v2ray.ang.ui.protocols

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.v2ray.ang.handler.MmkvManager
import com.wireguard.config.Config
import de.blinkt.openvpn.core.ConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One MMKV value commits the complete downloaded generation, so failed updates leave the old profiles intact. */
internal object ProtocolSubscriptionRepository {
    val mutation = Mutex()
    private const val KEY = "protocol_subscriptions_v1"
    private val gson = Gson()
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS).build()
    fun read(): List<ProtocolSubscription> = MmkvManager.decodeSettingsString(KEY)?.let {
        gson.fromJson<List<ProtocolSubscription>>(it, object : TypeToken<List<ProtocolSubscription>>() {}.type)
    }.orEmpty()
    fun save(records: List<ProtocolSubscription>) {
        require(records.size <= 8)
        val encoded = gson.toJson(records)
        require(encoded.toByteArray().size <= 32 * 1024 * 1024)
        check(MmkvManager.encodeSettings(KEY, encoded))
    }
    fun readProfileBytes(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size() + count <= OpenVpnImportPolicy.MAX_BYTES); output.write(buffer, 0, count) }
        return output.toByteArray()
    }
    suspend fun fetch(context: Context, id: String, title: String, link: String, fetchBytes: suspend (HttpUrl) -> ByteArray = ::download): ProtocolSubscription {
        val source = EylanSubscription.source(link)
        val manifest = withContext(Dispatchers.Default) { EylanSubscription.parse(id, source, fetchBytes(EylanSubscription.manifestUrl(source)).toString(Charsets.UTF_8)) }
        val accountBytes = fetchBytes(EylanSubscription.userDataUrl(source))
        val info = withContext(Dispatchers.Default) { parseProtocolSubscriptionInfo(accountBytes.toString(Charsets.UTF_8), source.pathSegments[2]) }
        val profiles = manifest.cisco.toMutableList()
        var total = 0
        for (item in manifest.downloads) {
            val bytes = fetchBytes(item.url)
            total += bytes.size; require(total <= 24 * 1024 * 1024)
            val config = withContext(Dispatchers.Default) {
                if (item.protocol == ProfileProtocol.OpenVPN) {
                    val text = OpenVpnImportPolicy.decode("profile.ovpn", bytes)
                    val parser = ConfigParser(); parser.parseConfig(StringReader(text))
                    require(parser.convertProfile().checkProfile(context) == de.blinkt.openvpn.R.string.no_error_found)
                    text
                } else Config.parse(bytes.inputStream()).toWgQuickString()
            }
            profiles += SubscriptionProfile(item.id, item.name, item.protocol, config)
        }
        return ProtocolSubscription(id, title.trim().take(100).ifBlank { info.username }, source.toString(), System.currentTimeMillis(), profiles, info)
    }

    suspend fun replace(context: Context, records: List<ProtocolSubscription>, id: String, title: String, link: String,
                        fetchBytes: suspend (HttpUrl) -> ByteArray = ::download,
                        commit: suspend (ProtocolSubscription?, List<ProtocolSubscription>) -> Unit): ProtocolSubscription {
        val normalized = EylanSubscription.source(link).toString()
        require(records.none { it.id != id && it.url == normalized })
        val old = records.firstOrNull { it.id == id }
        require(old != null || records.size < 8)
        val replacement = fetch(context, id, title, normalized, fetchBytes)
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        commit(old, records.filterNot { it.id == id } + replacement)
        return replacement
    }
    private suspend fun download(url: HttpUrl): ByteArray = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (!continuation.isCancelled) continuation.resumeWithException(IOException("Download unavailable")) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes = response.use {
                        require(it.isSuccessful)
                        val body = it.body ?: error("Missing response")
                        require(body.contentLength() <= OpenVpnImportPolicy.MAX_BYTES)
                        body.byteStream().use { input ->
                            val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                            while (true) {
                                if (continuation.isCancelled) throw IOException("Cancelled")
                                val count = input.read(buffer); if (count < 0) break
                                require(output.size() + count <= OpenVpnImportPolicy.MAX_BYTES)
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                    }
                    if (!continuation.isCancelled) continuation.resume(bytes)
                } catch (e: Exception) { if (!continuation.isCancelled) continuation.resumeWithException(IOException("Download rejected")) }
            }
        })
    }
}
