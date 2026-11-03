package com.v2ray.ang.ui.subscription

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubscriptionPresentationTest {
    private val bucket = SubscriptionCache(AppConfig.DEFAULT_SUBSCRIPTION_ID, SubscriptionItem(remarks = "Default"))
    private val first = SubscriptionCache("one", SubscriptionItem(remarks = "Default", url = "https://example.test/sub"))
    private val second = SubscriptionCache("two", SubscriptionItem(remarks = "My plan"))

    @Test fun `fresh install has no default plan`() {
        assertTrue(visibleSubscriptions(listOf(bucket)).isEmpty())
        assertTrue(visibleSubscriptions(emptyList()).isEmpty())
    }
    @Test fun `user plans remain regardless of name or enabled state`() {
        assertEquals(listOf(first, second), visibleSubscriptions(listOf(bucket, first, second)))
        assertEquals(listOf(first.copy(subscription = first.subscription.copy(enabled = false))),
            visibleSubscriptions(listOf(first.copy(subscription = first.subscription.copy(enabled = false)))))
    }
    @Test fun `actual configured default and manual servers remain accessible`() {
        val configured = bucket.copy(subscription = bucket.subscription.copy(url = first.subscription.url))
        assertEquals(listOf(configured), visibleSubscriptions(listOf(configured)))
        assertEquals(listOf(bucket), visibleSubscriptions(listOf(bucket), manualBucketHasServers = true))
    }
    @Test fun `reorder retains hidden bucket and uses ids`() {
        assertEquals(listOf(bucket, second, first), reorderedSubscriptions(listOf(bucket, first, second), "two", "one"))
    }
    @Test fun `removed or duplicate targets do not mutate order`() {
        val items = listOf(bucket, first, second)
        assertEquals(items, reorderedSubscriptions(items, "missing", "one"))
        assertEquals(items, reorderedSubscriptions(items, "one", "missing"))
        assertEquals(items, reorderedSubscriptions(items, "one", "one"))
    }
    @Test fun `new url subscription is fetched once by guid`() {
        assertEquals(listOf("one"), subscriptionsToFetch(listOf(bucket), listOf(bucket, first, second)))
        assertTrue(subscriptionsToFetch(listOf(bucket, first), listOf(bucket, first)).isEmpty())
    }
    @Test fun `empty disabled and removed subscriptions never trigger fetch`() {
        assertTrue(subscriptionsToFetch(emptyList(), listOf(bucket, second, first.copy(subscription = first.subscription.copy(enabled = false)))).isEmpty())
        assertTrue(subscriptionsToFetch(listOf(first), emptyList()).isEmpty())
    }
}
