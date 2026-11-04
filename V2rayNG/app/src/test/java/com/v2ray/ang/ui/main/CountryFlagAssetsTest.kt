package com.v2ray.ang.ui.main

import java.io.DataInputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CountryFlagAssetsTest {
    @Test fun everySupportedIsoCountryHasAFullResolutionBundledFlag() {
        val root = File("src/main/assets/country-flags")
        val archives = root.listFiles()!!.associate { it.nameWithoutExtension to ZipFile(it) }
        try {
            Locale.getISOCountries().forEach { country ->
                val code = country.lowercase(Locale.ROOT)
                val zip = archives[code.first().toString()]!!
                val entry = zip.getEntry("$code.png")
                assertNotNull(entry, "Missing $country")
                DataInputStream(zip.getInputStream(entry)).use {
                    assertEquals(0x89504e470d0a1a0aUL.toLong(), it.readLong(), country)
                    assertEquals(13, it.readInt(), country)
                    assertEquals(0x49484452, it.readInt(), country)
                    assertEquals(2048, it.readInt(), country)
                    assertEquals(1536, it.readInt(), country)
                }
            }
        } finally { archives.values.forEach { it.close() } }
        assertTrue(File("src/main/assets/flags-LICENSE.txt").readText().contains("MIT"))
    }
}
