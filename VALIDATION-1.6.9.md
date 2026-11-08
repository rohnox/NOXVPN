# Special VPN 1.6.9 — validation

Package: `com.specialvpn.app`; versionCode 782 (universal F-Droid APK code 5078200); min SDK 24; target SDK 37.

## Executed

- `:app:assembleFdroidRelease` — successful with Gradle 9.8.2 and JDK 21.
- APK package metadata confirms `com.specialvpn.app`, version 1.6.9 and versionCode 5078200.
- Release APK signature verified (v2 and v3); signing certificate SHA-256 matches the established release certificate: `3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8`.
- ZIP alignment check passed.
- APK SHA-256: `B5D0A7B5217C8882F00BA06EB763F09D52F8F9EED1339D9FF81EA9C84AF7E02D`.

## Not run

- Physical-device installation and visual confirmation of the About screen.
- Unit tests and interactive accessibility/focus checks; this change only removes the Telegram channel menu item.
