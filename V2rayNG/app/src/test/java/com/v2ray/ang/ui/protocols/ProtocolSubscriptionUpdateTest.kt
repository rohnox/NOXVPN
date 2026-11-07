package com.v2ray.ang.ui.protocols

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.IOException

class ProtocolSubscriptionUpdateTest {
    private val key = java.util.Base64.getEncoder().encodeToString(ByteArray(32) { 1 })
    private val config get() = "[Interface]\nPrivateKey=$key\nAddress=10.0.0.2/32\n[Peer]\nPublicKey=$key\nAllowedIPs=0.0.0.0/0\nEndpoint=vpn.example.invalid:51820\n"
    private fun manifest(two: Boolean = false) = """{"success":true,"username":"demo","wireguard":{"enabled":true,"servers":[{"name":"One","download_url":"/one"}${if (two) ",{\"name\":\"Two\",\"download_url\":\"/two\"}" else ""}]}}"""
    @Test fun completeDownloadCommitsExactlyOneGenerationAndPreservesOtherSubscriptions() = runTest {
        val other = ProtocolSubscription("other", "Manual", "", 0, emptyList())
        val old = ProtocolSubscription("id", "Old", "https://example.invalid/sub/a/demo", 1, emptyList())
        var commits = 0
        val result = ProtocolSubscriptionRepository.replace(mock<Application>(), listOf(other, old), "id", "Plan", old.url, fetchBytes = { url ->
            if (url.encodedPath.startsWith("/get_subpage")) manifest().toByteArray() else if (url.encodedPath.startsWith("/get_user_data")) """{"user":{"username":"demo"}}""".toByteArray() else config.toByteArray()
        }) { previous, next ->
            commits++
            assertEquals(old, previous)
            assertTrue(next.contains(other))
            assertEquals(2, next.size)
            assertTrue(next.single { it.id == "id" }.profiles.single().config.contains("[Interface]"))
        }
        assertEquals(1, commits)
        assertEquals(ProfileProtocol.WireGuard, result.profiles.single().protocol)
    }
    @Test fun partialDownloadsAndInvalidConfigsNeverCommit() = runTest {
        for (invalid in listOf(false, true)) {
            var commits = 0
            var downloads = 0
            val failure = runCatching {
                ProtocolSubscriptionRepository.replace(mock<Application>(), emptyList(), "id", "Plan", "https://example.invalid/sub/a/demo", fetchBytes = { url ->
                    if (url.encodedPath.startsWith("/get_user_data")) """{"user":{"username":"demo"}}""".toByteArray()
                    else if (url.encodedPath.startsWith("/get_subpage")) manifest(two = true).toByteArray()
                    else { downloads++; if (invalid) "invalid".toByteArray() else if (downloads == 1) config.toByteArray() else throw IOException("fixture") }
                }) { _, _ -> commits++ }
            }
            assertTrue(failure.isFailure)
            assertEquals(0, commits)
        }
    }
    @Test fun rejectsDuplicateSubscriptionsAndPropagatesAtomicStorageFailure() = runTest {
        val existing = ProtocolSubscription("old", "Old", "https://example.invalid/sub/a/demo", 1, emptyList())
        var requested = false
        val duplicate = runCatching {
            ProtocolSubscriptionRepository.replace(mock<Application>(), listOf(existing), "new", "Plan", existing.url, fetchBytes = { requested = true; byteArrayOf() }) { _, _ -> fail<Unit>("Must not commit") }
        }
        assertTrue(duplicate.isFailure); assertFalse(requested)
        val storage = runCatching {
            ProtocolSubscriptionRepository.replace(mock<Application>(), listOf(existing), "old", "Plan", existing.url, fetchBytes = { url -> if (url.encodedPath.startsWith("/get_subpage")) manifest().toByteArray() else if (url.encodedPath.startsWith("/get_user_data")) """{"user":{"username":"demo"}}""".toByteArray() else config.toByteArray() }) { _, _ -> throw IOException("storage") }
        }
        assertTrue(storage.isFailure)
        assertEquals("Old", existing.title)
        assertTrue(existing.profiles.isEmpty())
    }
    @Test fun failedOrForeignAccountMetadataKeepsPreviousGenerationUntouched() = runTest {
        val info = parseProtocolSubscriptionInfo("""{"user":{"username":"demo","data_limit":0,"expiry_date_iso":null}}""", "demo")
        val existing = ProtocolSubscription("id", "Old", "https://example.invalid/sub/a/demo", 1, emptyList(), info)
        for (foreign in listOf(false, true)) {
            var commits = 0
            val result = runCatching {
                ProtocolSubscriptionRepository.replace(mock<Application>(), listOf(existing), "id", "Plan", existing.url, fetchBytes = { url ->
                    if (url.encodedPath.startsWith("/get_subpage")) manifest().toByteArray()
                    else if (foreign) """{"user":{"username":"other"}}""".toByteArray() else throw IOException("fixture")
                }) { _, _ -> commits++ }
            }
            assertTrue(result.isFailure); assertEquals(0, commits); assertEquals(info, existing.info)
        }
    }
}
