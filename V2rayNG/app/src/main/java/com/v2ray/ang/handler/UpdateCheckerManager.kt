package com.v2ray.ang.handler

import android.os.Build
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.CheckUpdateResult
import com.v2ray.ang.dto.GitHubRelease
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.extension.concatUrl
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateCheckerManager {
    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult = withContext(Dispatchers.IO) {
        val url = if (includePreRelease) {
            AppConfig.APP_API_URL
        } else {
            AppConfig.APP_API_URL.concatUrl("latest")
        }

        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()

        var response = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 5000
            )
        )
        if (response.isNullOrEmpty()) {
            val httpPort = SettingsManager.getHttpPort()
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword
                )
            )
                ?: throw IllegalStateException("Failed to get response")
        }

        val latestRelease = if (includePreRelease) {
            JsonUtil.fromJsonSafe(response, Array<GitHubRelease>::class.java)
                ?.firstOrNull { !it.prerelease || includePreRelease }
                ?: throw IllegalStateException("No pre-release found")
        } else {
            JsonUtil.fromJsonSafe(response, GitHubRelease::class.java)
        }
        if (latestRelease == null) {
            return@withContext CheckUpdateResult(hasUpdate = false)
        }

        resolveRelease(latestRelease, BuildConfig.VERSION_NAME, Build.SUPPORTED_ABIS.firstOrNull().orEmpty(), includePreRelease)
    }

    /** Only our signed Release asset, never Debug or an upstream application's APK. */
    internal fun resolveRelease(release: GitHubRelease, currentVersion: String, abi: String, includePreRelease: Boolean = false): CheckUpdateResult {
        if (release.prerelease && !includePreRelease) return CheckUpdateResult(hasUpdate = false)
        val latest = release.tagName.removePrefix("v")
        fun parts(value: String): List<Int> {
            require(value.matches(Regex("[0-9]{1,9}(\\.[0-9]{1,9}){1,3}")))
            return value.split('.').map { it.toInt() }
        }
        val remote = parts(latest)
        val current = parts(currentVersion)
        val order = (0 until maxOf(remote.size, current.size)).asSequence().map { (remote.getOrElse(it) { 0 }).compareTo(current.getOrElse(it) { 0 }) }.firstOrNull { it != 0 } ?: 0
        if (order <= 0) return CheckUpdateResult(hasUpdate = false)
        val names = listOf("SpecialVPN-$latest-universal-release.apk", "SpecialVPN-$latest-$abi-release.apk")
        val asset = names.firstNotNullOfOrNull { name -> release.assets.firstOrNull { it.name == name } }
            ?: error("No compatible Special VPN Release APK")
        val uri = java.net.URI(asset.browserDownloadUrl)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null && uri.fragment == null && uri.query == null)
        require(uri.rawPath == "/sabialballa/iSpeedTopVPN/releases/download/" + release.tagName + "/" + asset.name)
        return CheckUpdateResult(hasUpdate = true, latestVersion = latest, releaseNotes = release.body,
            downloadUrl = asset.browserDownloadUrl, isPreRelease = release.prerelease)
    }
}
