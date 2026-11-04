package com.v2ray.ang.handler

import com.v2ray.ang.dto.GitHubRelease
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpecialUpdateTest {
    private fun release(version: String = "1.6.3", name: String = "SpecialVPN-$version-universal-release.apk", url: String = "https://github.com/sabialballa/iSpeedTopVPN/releases/download/v$version/$name", pre: Boolean = false) =
        GitHubRelease("v$version", "Release notes", listOf(GitHubRelease.Asset(name, url)), pre)
    @Test fun newerUniversalReleaseWorksForAllSupportedAbis() {
        listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64").forEach { abi ->
            val result = UpdateCheckerManager.resolveRelease(release(), "1.6.2", abi)
            assertTrue(result.hasUpdate)
            assertEquals("1.6.3", result.latestVersion)
            assertTrue(result.downloadUrl!!.endsWith("universal-release.apk"))
        }
    }
    @Test fun equalOlderAndPrereleaseDoNotTriggerStableUpdate() {
        assertFalse(UpdateCheckerManager.resolveRelease(release("1.6.2"), "1.6.2", "x86").hasUpdate)
        assertFalse(UpdateCheckerManager.resolveRelease(release("1.5.9"), "1.6.2", "x86").hasUpdate)
        assertFalse(UpdateCheckerManager.resolveRelease(release(pre = true), "1.6.2", "x86").hasUpdate)
        assertTrue(UpdateCheckerManager.resolveRelease(release("1.10.0"), "1.9.9", "x86").hasUpdate)
    }
    @Test fun debugUpstreamAndUntrustedDownloadsAreRejected() {
        assertThrows(IllegalStateException::class.java) { UpdateCheckerManager.resolveRelease(release(name = "SpecialVPN-1.6.3-universal-debug.apk"), "1.6.2", "x86") }
        for (url in listOf("http://github.com/sabialballa/iSpeedTopVPN/releases/download/v1.6.3/SpecialVPN-1.6.3-universal-release.apk", "https://example.invalid/app.apk", "https://github.com/2dust/v2rayNG/releases/download/v1.6.3/SpecialVPN-1.6.3-universal-release.apk")) {
            assertThrows(IllegalArgumentException::class.java) { UpdateCheckerManager.resolveRelease(release(url = url), "1.6.2", "x86") }
        }
        assertThrows(IllegalArgumentException::class.java) { UpdateCheckerManager.resolveRelease(release("not-a-version"), "1.6.2", "x86") }
    }
}
