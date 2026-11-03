# Device verification — not run

No Android device/emulator session was available to this build. ADB could not initialize its default `.android` directory under the current filesystem restrictions. No live server, credentials or subscription was copied from the browser or used in tests. Compilation/JVM tests/static APK checks do not establish connectivity.

All checks below are **Not run**:

| Area | Required checks |
| --- | --- |
| Installation | Fresh install; same-certificate update from 1.3.0; preserve existing Xray data; Play Protect behavior |
| OpenVPN | SAF grant/cancel; inline certificate import; encrypted private key; password prompt; challenge response; real server connection; invalid credentials; certificate failure; traffic accuracy; disconnect |
| Cisco | Create/edit profile; address/port; group selection; password; OTP/MFA; server certificate confirmation/rejection; native connection; disconnect; gateway compatibility |
| Lifecycle | Cold/repeated start; notification tap; process death; return from recents; screen rotation; background/foreground; battery restriction; VPN consent deny/accept; VPN revocation; no stale connected state |
| Switching | Xray → OpenVPN → Cisco and reverse; one active Android tunnel; actual service stop; independent notification IDs; no cross-engine traffic/status attribution |
| Modes | Keep Xray VPN/root/proxy modes intact; hub disables new-engine connect outside VPN mode; route/DNS settings remain engine-specific |
| Devices | Android API 24/25/26/27 legacy execution; API 28+ extracted binaries; current foreground-service/notification rules; arm64/armv7/x86/x86_64 runtime; 16KiB-page device |
| UI | Persian RTL/English; narrow/landscape/large font; light/dark; IME/system bar insets; inherited advanced Cisco forms |

Do not distribute as a device-validated production VPN until these checks are completed using authorized test accounts. Keep diagnostic logs local and redact server addresses, account names, tokens, certificates and configuration contents before sharing.
