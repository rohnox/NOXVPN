# iSpeedTopVPN APP — 1.5.3

نمودار مجموع مصرف ساب با سابقه ثبت‌شده ۳۰ روز اخیر، کارت مستقل «سهمیه مصرف»، نام پنل، روزهای باقی‌مانده و پیام سبز پس از آپدیت موفق اشتراک. وضعیت OpenVPN مستقیم از سرویس خوانده می‌شود و پیش از ورود به اتصال OpenVPN/Cisco، فرمان توقف Xray ارسال می‌شود.

سابقه مصرف از اولین به‌روزرسانی این نسخه ثبت می‌شود؛ لینک ساب تاریخچه قبلی روزانه را ارائه نمی‌کند. عدد مصرف دوره اشتراک با مجموع مصرف یک ماه فرض نمی‌شود.

[تغییرات](CHANGES-1.5.3.md) · [بررسی‌ها و محدودیت‌ها](VALIDATION-1.5.3.md) · [ساخت](BUILD-FA.md) · [امضا](RELEASE-FA.md)

فایل‌های APK و ZIP کامل نسخه ۱٫۵٫۳ همراه تحویل محلی ارائه می‌شوند. یادداشت‌ها و لینک Release نسخه‌های قبل در ادامه تاریخی‌اند.

# iSpeedTopVPN APP — 1.5.2

نمودار حجم مصرف واقعی اتصال، کارت سهمیه اشتراک، نام دریافتی از پنل، روزهای باقی‌مانده و به‌روزرسانی خودکار ساب هنگام ورود به برنامه. درخواست‌های ورود تکراری کمتر از یک دقیقه ادغام می‌شوند؛ تازه‌سازی دستی همچنان در دسترس است.

[تغییرات](CHANGES-1.5.2.md) · [نتیجه بررسی‌ها و محدودیت‌ها](VALIDATION-1.5.2.md) · [راهنمای ساخت](BUILD-FA.md) · [امضا](RELEASE-FA.md)

خروجی‌های APK و ZIP کامل نسخه ۱٫۵٫۲ همراه تحویل محلی ارائه می‌شوند. لینک Release زیر مربوط به نسخه قبلی است.

# iSpeedTopVPN APP — 1.5.1

نسخه اندروید با رابط مشکی و طلایی، پشتیبانی فارسی و انگلیسی، Xray و بخش مستقل OpenVPN/Cisco؛ بر پایه سورس واقعی v2rayNG 2.3.14 با حفظ تاریخچه و مجوزهای اصلی.

- [دانلود APK و ZIP کامل سورس](https://github.com/sabialballa/iSpeedTopVPN/releases/tag/v1.5.1)
- [تغییرات نسخه](CHANGES-1.5.1.md) · [گزارش ساخت و محدودیت‌ها](VALIDATION.md)
- [ساخت](BUILD-FA.md) · [امضای انتشار](RELEASE-FA.md) · [OpenVPN/Cisco](PROTOCOLS-FA.md) · [سورس و مجوز هسته‌ها](NATIVE-ENGINES.md)

برای ساخت با همان ورودی‌های Native نسخه تحویلی، ZIP کامل سورسِ Release را دریافت کنید. درخت Git سورس و submoduleهای اصلی را نگه می‌دارد؛ فایل‌های تولیدشده AAR/SO/JAR که در Git نادیده گرفته می‌شوند، در ZIP کامل موجودند. ساخت دوباره هسته‌ها و محدودیت‌های آن در راهنماها مستند است. قالب‌های Workflows بالادستی در docs/upstream-workflows/ حفظ شده‌اند؛ CI و امضای انتشار در حساب جدید تنظیم یا اجرا نشده‌اند و هیچ کلید خصوصی به گیت‌هاب فرستاده نشده است.


برای کلون در ویندوز، مسیر کوتاه انتخاب کنید و پشتیبانی مسیرهای طولانی را فعال نگه دارید:

```sh
git clone -c core.longpaths=true --recurse-submodules https://github.com/sabialballa/iSpeedTopVPN.git
```

رندرهای کد در `verification/1.5.1/renders/` با داده آزمایشی ساخته شده‌اند و آزمون اتصال واقعی نیستند. برای بازتولید اختیاری رندرها، از پوشه V2rayNG اجرا کنید:

```sh
./gradlew -I ../scripts/visual-checks.init.gradle -Pispeed.visualChecks=true :app:testFdroidDebugUnitTest --tests '*PremiumVisualTest'
```

مخزن خصوصی است و دانلود فایل‌های Release به دسترسی همین مخزن نیاز دارد. یادداشت‌های نسخه‌های قبلی در ادامه تاریخی‌اند.

# iSpeedTopVPN APP — 1.5.0

Modern black/gold dashboard and an independent OpenVPN/Cisco bottom tab. Home, Servers, Subscriptions, Open/Cisco, Controls. See CHANGES-1.5.0.md and VALIDATION.md. Earlier notes below are historical.

# iSpeedTopVPN APP — 1.4.3

OpenVPN start initialization fix; only the two Home Quick Access Xray add/test buttons removed. OpenVPN/Cisco and .ovpn import remain. See CHANGES-1.4.3.md and VALIDATION.md. Prior notes below are historical.

# iSpeedTopVPN APP — 1.4.1

OpenVPN/Cisco entry freeze fix: initialize the status-service cache and move pipe/Binder reads off the UI callback. See CHANGES-1.4.1.md and VALIDATION.md. Prior notes below are historical.

# iSpeedTopVPN APP — 1.4.0

Embedded OpenVPN file import and an OpenConnect manager for AnyConnect-compatible servers, alongside the existing Xray dashboard. See [usage](PROTOCOLS-FA.md), [changes](CHANGES-1.4.0.md), [engine provenance/rebuild](NATIVE-ENGINES.md) and [actual verification](VALIDATION.md). Earlier notes below are historical.

# iSpeedTopVPN APP — 1.3.0

Metallic black and gold; empty internal Default plan hidden, immediate fetch for newly added subscriptions. See [changes](CHANGES-1.3.0.md) and [validation](VALIDATION.md). Earlier release notes below are historical.

نسخه Release اختصاصی ۱٫۲٫۱ ساخته و امضا شد؛ راهنما: [RELEASE-FA.md](RELEASE-FA.md). هشدار Play Protect هنوز روی این فایل بررسی نشده است. گزارش فعلی: [VALIDATION.md](VALIDATION.md).

# iSpeedTopVPN APP — 1.2.1

Home one-tap subscription refresh and real-delay testing in Servers. Custom Compose dashboard, server cards, provider quota and live-device traffic chart. Adds one-button automatic connection: fresh real-ping tests of imported configurations, lowest successful latency, cancellation and stale-result protection. Based on GPL v2rayNG 2.3.14.

Universal Debug APK built; signature, ABI, alignment, resources and F-Droid lint verified. No device or live-VPN success is claimed. See [auto-connect guide](AUTO-CONNECT-FA.md), [validation](VALIDATION.md) and [build/signing steps](BUILD-FA.md).

---
# v2rayNG

A V2Ray client for Android, support [Xray core](https://github.com/XTLS/Xray-core) and [v2fly core](https://github.com/v2fly/v2ray-core)

[![API](https://img.shields.io/badge/API-24%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/lollipop)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg)](https://kotlinlang.org)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/2dust/v2rayNG)](https://github.com/2dust/v2rayNG/commits/master)
[![CodeFactor](https://www.codefactor.io/repository/github/2dust/v2rayng/badge)](https://www.codefactor.io/repository/github/2dust/v2rayng)
[![GitHub Releases](https://img.shields.io/github/downloads/2dust/v2rayNG/latest/total?logo=github)](https://github.com/2dust/v2rayNG/releases)
[![Chat on Telegram](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/v2rayn)

---

## Download / 下载

Download the latest release here:

在这里下载最新版本：

[https://github.com/2dust/v2rayNG/releases](https://github.com/2dust/v2rayNG/releases)

> [!TIP]
> v2rayNG is the mobile version. For the desktop version, please visit the v2rayN \
> v2rayNG 是手机版，电脑版请访问 v2rayN
>
> https://github.com/2dust/v2rayN

---

### Geoip and Geosite

- geoip.dat and geosite.dat files are in `Android/data/com.v2ray.ang/files/assets` (path may differ on some Android device)
- download feature will get enhanced version in this [repo](https://github.com/Loyalsoldier/v2ray-rules-dat) (note: it needs a working proxy)
- latest official [domain list](https://github.com/Loyalsoldier/v2ray-rules-dat) and [ip list](https://github.com/Loyalsoldier/geoip) can be imported manually
- possible to use a third-party dat file in the same folder, like [h2y](https://guide.v2fly.org/routing/sitedata.html#%E5%A4%96%E7%BD%AE%E7%9A%84%E5%9F%9F%E5%90%8D%E6%96%87%E4%BB%B6)

More in our [wiki](https://github.com/2dust/v2rayNG/wiki)

### Geoip 与 Geosite

- geoip.dat 和 geosite.dat 文件位于 `Android/data/com.v2ray.ang/files/assets`（部分设备路径可能不同）
- 下载功能将获取该 [仓库](https://github.com/Loyalsoldier/v2ray-rules-dat) 中的增强版本（注意：此功能需要一个可用的代理）
- 最新官方 [域名列表](https://github.com/Loyalsoldier/v2ray-rules-dat) 和 [IP 列表](https://github.com/Loyalsoldier/geoip) 可手动导入
- 也可在同一文件夹中使用第三方 dat 文件，例如 [h2y](https://guide.v2fly.org/routing/sitedata.html#%E5%A4%96%E7%BD%AE%E7%9A%84%E5%9F%9F%E5%90%8D%E6%96%87%E4%BB%B6)

更多内容请见我们的 [wiki](https://github.com/2dust/v2rayNG/wiki)

---

## Development guide / 开发指南

### Note

- Android project under the V2rayNG folder can be compiled directly in Android Studio, or using the Gradle wrapper. But the v2ray core inside the aar is (probably) outdated.
- The aar can be compiled from the Golang project [AndroidLibV2rayLite](https://github.com/2dust/AndroidLibV2rayLite) or [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite). For a quick start, read the guides for [Go Mobile](https://github.com/golang/go/wiki/Mobile) and [Makefiles for Go Developers](https://tutorialedge.net/golang/makefiles-for-go-developers/).
- v2rayNG can run on Android Emulators. For WSA, VPN permission needs to be granted via `appops set [package name] ACTIVATE_VPN allow`.

### 提示

- V2rayNG 文件夹下的 Android 项目可直接在 Android Studio 中编译，或使用 Gradle wrapper 编译。但 aar 内置的 v2ray core（可能）已过时。
- aar 可由 Golang 项目 [AndroidLibV2rayLite](https://github.com/2dust/AndroidLibV2rayLite) 或 [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) 编译而成。快速入门可参考 [Go Mobile](https://github.com/golang/go/wiki/Mobile) 指南和 [Makefiles for Go Developers](https://tutorialedge.net/golang/makefiles-for-go-developers/)。
- v2rayNG 可在 Android 模拟器上运行。对于 WSA，需要通过 `appops set [package name] ACTIVATE_VPN allow` 授予 VPN 权限。

---


## GPG Verification / GPG 签名校验

Release files are signed with GPG to verify authenticity and integrity, helping prevent mirror, ISP, or CDN hijacking.

发布文件已使用 GPG 签名，可用于校验文件真实性与完整性，预防镜像站、运营商或 CDN 劫持。

### Fingerprint / 公钥指纹

```text
7694 5E9F 3E9A 168F 8070 F195 805D 661C
134D FAF6 8903 C199 463C 31E5 AE90 3AE0
```

---

## Community / 社区

Telegram Group / Telegram 群组：

[https://t.me/v2rayN](https://t.me/v2rayN)

Telegram Channel / Telegram 频道：

[https://t.me/github_2dust](https://t.me/github_2dust)
