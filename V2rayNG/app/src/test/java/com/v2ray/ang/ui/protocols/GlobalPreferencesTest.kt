package com.v2ray.ang.ui.protocols

import de.blinkt.openvpn.core.GlobalPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GlobalPreferencesTest {
    /** A fresh class loader models an independent Android process with no Application setup. */
    private fun freshProcess(): Class<*> {
        val name = GlobalPreferences::class.java.name
        val bytes = checkNotNull(GlobalPreferences::class.java.getResourceAsStream("GlobalPreferences.class"))
            .use { it.readBytes() }
        return object : ClassLoader(null) {
            fun loadPreferences(): Class<*> = defineClass(name, bytes, 0, bytes.size)
        }.loadPreferences()
    }

    @Test fun `cold notification and launch paths have usable defaults without app restrictions`() {
        val process = freshProcess()
        for (getter in listOf("getForceConnected", "getMinimalUi", "getAllowInitialImport")) {
            assertEquals(false, process.getMethod(getter).invoke(null))
        }
    }

    @Test fun `managed overrides work and another process still starts with independent defaults`() {
        val managed = freshProcess()
        val service = freshProcess()
        managed.getMethod("setInstance", Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).invoke(null, true, true, true)
        for (getter in listOf("getForceConnected", "getMinimalUi", "getAllowInitialImport")) {
            assertEquals(true, managed.getMethod(getter).invoke(null))
            assertEquals(false, service.getMethod(getter).invoke(null))
        }
    }
}
