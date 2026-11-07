# ساخت Special VPN

پروژهٔ اندروید در پوشهٔ V2rayNG است. نام برنامه Special VPN و پکیج آن com.specialvpn.app است؛ فضای نام داخلی برای سازگاری JNI تغییر نکرده است. حداقل اندروید ۷ / API 24 و چهار معماری arm64-v8a، armeabi-v7a، x86 و x86_64 پشتیبانی می‌شوند.

## دریافت سورس و پیش‌نیازها

برای ساخت آماده، ZIP کامل سورسِ پیوست ریلیز را دریافت کنید. آرشیو خودکار گیت‌هاب جایگزین آن نیست، چون ورودی‌های تولیدشدهٔ Native را شامل نمی‌شود. برای checkout گیت، زیرماژول‌ها را با دستور زیر آماده و ورودی‌های Native را طبق اسکریپت‌های پروژه و مستندات هر موتور بازسازی کنید:

```powershell
git submodule update --init --recursive
```

JDK 21، Android SDK و NDK لازم‌اند. نسخه‌های دقیق ابزارها در V2rayNG/gradle/libs.versions.toml، V2rayNG/app/build.gradle.kts و اسکریپت‌های ساخت Native قرار دارند. پروژه را در Android Studio باز کنید یا JAVA_HOME و ANDROID_HOME را تنظیم کنید. فایل local.properties و اطلاعات خصوصی امضا را وارد گیت نکنید.

## ساخت F-Droid Universal

از پوشهٔ V2rayNG اجرا کنید؛ برای خروجی Universal گزینهٔ ABI_FILTERS نفرستید:

```powershell
.\gradlew.bat :app:testFdroidDebugUnitTest :app:assembleFdroidDebug
.\gradlew.bat :app:assembleFdroidRelease :app:lintFdroidDebug
```

خروجی نمونهٔ نسخهٔ ۱.۶.۸:

- Debug: app/build/outputs/apk/fdroid/debug/SpecialVPN_1.6.8-fdroid_universal.apk
- Release پیش از امضای دستی: app/build/outputs/apk/fdroid/release/SpecialVPN_1.6.8-fdroid_universal.apk

Release را طبق [راهنمای امضا](RELEASE-FA.md) هم‌تراز و امضا کنید. نسخهٔ Debug برای توسعه است و نباید جای APK انتشار به مشتری تحویل داده شود.

## بررسی‌ها

[گزارش نسخهٔ فعلی](VALIDATION.md) نتیجهٔ واقعی تست و ساخت را مشخص می‌کند. در محیط ویندوز چهار تست RootProcessRunner به ابزار sh نیاز دارند؛ این محدودیت را موفقیت کامل تست‌ها تلقی نکنید. ساخت APK جای تست نصب، مجوز VPN و اتصال واقعی روی دستگاه را نمی‌گیرد. کانفیگ‌ها، پاسخ اشتراک و کلید خصوصی را در لاگ یا مخزن منتشر نکنید.
