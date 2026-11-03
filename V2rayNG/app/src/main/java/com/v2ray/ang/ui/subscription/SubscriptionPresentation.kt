package com.v2ray.ang.ui.subscription

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionCache

/** Keep the internal manual-config bucket, but never show its empty placeholder as a plan. */
internal fun visibleSubscriptions(items: List<SubscriptionCache>, manualBucketHasServers: Boolean = false): List<SubscriptionCache> =
    items.filter { it.guid != AppConfig.DEFAULT_SUBSCRIPTION_ID || it.subscription.url.isNotBlank() || manualBucketHasServers }

/** Reorder by stable identity, retaining hidden buckets and their stored configurations. */
internal fun reorderedSubscriptions(items: List<SubscriptionCache>, from: String, to: String): List<SubscriptionCache> {
    val result = items.toMutableList()
    val source = result.indexOfFirst { it.guid == from }
    val target = result.indexOfFirst { it.guid == to }
    if (source < 0 || target < 0 || source == target) return items
    result.add(target, result.removeAt(source))
    return result
}

/** Fetch newly saved, enabled URL subscriptions; never fetch the internal empty bucket. */
internal fun subscriptionsToFetch(previous: List<SubscriptionCache>, current: List<SubscriptionCache>): List<String> =
    current.filter { item ->
        item.subscription.enabled && item.subscription.url.isNotBlank() && previous.none { it.guid == item.guid }
    }.map { it.guid }
