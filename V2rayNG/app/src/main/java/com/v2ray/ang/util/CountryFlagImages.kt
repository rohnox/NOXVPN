package com.v2ray.ang.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.Locale
import java.util.zip.ZipInputStream

/** Bundled artwork only. Call on IO; no HTTP request, extraction or persistent user data. */
object CountryFlagImages {
    fun load(context: Context, country: String): Bitmap? {
        if (country !in Locale.getISOCountries()) return null
        val code = country.lowercase(Locale.ROOT)
        context.assets.open("country-flags/${code.first()}.zip").use { input ->
            ZipInputStream(input).use { archive ->
                while (true) {
                    val entry = archive.nextEntry ?: return null
                    if (entry.name == "$code.png") {
                        return BitmapFactory.decodeStream(archive, null, BitmapFactory.Options().apply {
                            inScaled = false
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        })
                    }
                    archive.closeEntry()
                }
            }
        }
    }
}
