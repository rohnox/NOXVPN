# Country flag backgrounds

Runtime artwork is rendered from lipis/flag-icons7.3.2, MIT licensed, 4:3 SVG sources. Source archive: flags-svg-7.3.2.zip; copyright/license: flags-LICENSE.txt. Original package integrity is recorded in flags-source-metadata.json. Source: https://github.com/lipis/flag-icons/tree/v7.3.2 .

The 257 two-letter flags are rasterized at2048x1536 using @resvg/resvg-js2.6.2. They are grouped by first letter in app/src/main/assets/country-flags/*.zip. The manifest records the PNG SHA256 and dimensions. All Java ISO3166 country codes are checked by CountryFlagAssetsTest. Extra upstream two-letter artwork does not broaden accepted country metadata.

Only the exact country code returned for the actual exit IP selects a background. Decode runs in MainViewModel's IO job through MainRepository; the Compose card only presents the resulting transient Bitmap. Flags need no HTTP download, location permission or runtime rendering library. Missing artwork or unresolved IP/country retains the existing background. The small badge emoji remains decorative next to the localized country name.

For regeneration, install flag-icons@7.3.2 and @resvg/resvg-js@2.6.2 in a temporary build-tool directory, render the two-letter SVG files from flags/4x3 at width2048 (4:3), then zip PNGs by first letter using their lowercase two-letter names. These build tools are not shipped in the APK. No flag/image is generated from a customer's data.
