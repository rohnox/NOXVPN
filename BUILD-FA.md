نسخه فعلی: Special VPN ۱٫۵٫۸؛ شناسه com.specialvpn.app. راهنمای امضا RELEASE-FA.md و گزارش این نسخه VALIDATION-1.5.8.md است. ورودی‌های بومی و نسخه ابزارها تغییر نکرده‌اند. مطالب نسخه‌های پیشین در ادامه تاریخی هستند.

نسخه فعلی ۱٫۴٫۳: OpenVPN/Cisco حفظ شده؛ دو دکمه مشخص‌شدهٔ صفحه اصلی حذف و مقداردهی شروع اتصال OpenVPN اصلاح شده است. گزارش: VALIDATION.md.

نسخه فعلی ۱٫۴٫۱: اصلاح مسیر ورود OpenVPN/Cisco؛ گزارش اجرای واقعی در VALIDATION.md و تغییرات در CHANGES-1.4.1.md است.

نسخه فعلی: ۱٫۴٫۰؛ راهنمای اتصال‌ها PROTOCOLS-FA.md، منشأ و ساخت هسته‌ها NATIVE-ENGINES.md و نتیجه اجرا VALIDATION.md است. بخش‌های نسخه‌های قدیمی پایین این فایل تاریخی هستند.

نسخه فعلی: ۱٫۳٫۰؛ راهنمای تغییرات CHANGES-1.3.0.md و گزارش نهایی VALIDATION.md را بخوانید. با همان کلید انتشار قبلی امضا شده است. بخش‌های قدیمی پایین این فایل تاریخی هستند.

نسخه به‌روز: ۱٫۲٫۱؛ تست Real Delay در Servers و آیکون آپدیت همه ساب‌ها در صفحه اول. وضعیت Play Protect و محدودیت‌ها در VALIDATION.md ثبت شده است.

نسخه فعلی: **۱٫۲٫۰**، دارای اتصال خودکار به کمترین پینگ موفق بین کانفیگ‌های واردشده. راهنما: AUTO-CONNECT-FA.md؛ گزارش نهایی: VALIDATION.md. آمار تلاش‌های قدیمی پایین این سند تاریخی است.

نسخه داشبورد اختصاصی برنامه: **۱٫۱٫۰**؛ مبنای هسته و سورس اولیه: v2rayNG 2.3.14. برای نتیجه به‌روز ساخت، "VALIDATION.md" و "REDESIGN-FA.md" را بخوانید.

# ساخت iSpeedTopVPN APP

مبنا دقیقاً v2rayNG 2.3.14 است. خروجی هدف مطابق درخواست، **F-Droid Universal** با شناسه `com.ispeedtopvpn.app` است. حداقل اندروید ۷ / API 24؛ معماری‌ها `arm64-v8a`، `armeabi-v7a`، `x86_64` و `x86`. Universal محدودیت نسخه اندروید یا سخت‌افزار را حذف نمی‌کند.

## نتیجه نهایی — این بخش جایگزین وضعیت اولیه زیر است

**APK Universal Debug ساخته شد و امضا، نام پکیج، نام برند در همه زبان‌ها و کتابخانه‌های چهار معماری تأیید شدند.** خطای ۴۰۴ تلاش اولیه برطرف شد؛ همان نسخه‌های ابزارهای upstream بدون downgrade دریافت شدند. چهار تست جدید پاس شدند. مجموعه کامل: ۱۲۴ پاس از ۱۲۸؛ چهار تست به دلیل نبود sh در ویندوز شکست خوردند. تست نصب و VPN روی گوشی انجام نشده است. گزارش نهایی VALIDATION.md است.

ZIP نهایی شامل ورودی‌های AAR/SO استفاده‌شده در ساخت و سورس کامل آن‌ها است. پس از نصب SDK و JDK، می‌توان مستقیماً Gradle را اجرا کرد؛ برای بازسازی HEV در ویندوز از scripts/build-native-windows.ps1 و در Linux از اسکریپت اصلی استفاده کنید. هدرهای symlink در آرشیو به محتوای هدف تبدیل شده‌اند تا آرشیو قابل حمل باشد.

## سابقه تلاش اولیه (پیش از ساخت موفق)

**APK تولید نشده است.** ساخت Debug اجرا شد ولی پیش از کامپایل، پلاگین `com.android.application:9.4.1` موجود در نسخه مبنا resolve نشد. درخواست مستقیم فایل POM به مخزن Google نیز HTTP 404 برگرداند. نسخه وابستگی‌ها برای پنهان‌کردن این مشکل تغییر داده نشده است. Android SDK/NDK و دستگاه اندرویدی در محیط آماده نبودند. نصب، اتصال و ظاهر نهایی روی دستگاه تأیید نشده‌اند. گزارش کامل در `VALIDATION.md` و لاگ‌ها در `verification/` است.

## پیش‌نیازهای دقیق نسخه مبنا

- Linux یا WSL برای اسکریپت بومی HEV؛ Android Studio برای ویرایش و اجرا.
- JDK 21 طبق workflow رسمی؛ Gradle wrapper داخل پروژه (9.8.2).
- Android SDK: `platforms;android-37.0`، `build-tools;37.0.0`، `platform-tools`.
- Android NDK `30.0.16248370`، متغیرهای `ANDROID_HOME` و `NDK_HOME`.
- دسترسی HTTPS به Google Maven، Maven Central، Gradle Plugin Portal، JitPack و GitHub.
- AGP `9.4.1`، Kotlin `2.4.21` و بقیه نسخه‌ها در `V2rayNG/gradle/libs.versions.toml` قفل هستند. تا وقتی artifactهای این نسخه‌ها قابل دریافت نباشند، ساخت همین سورس ممکن نیست؛ تغییر نسخه ابزارها نیازمند بررسی سازگاری مستقل است.

## دریافت ورودی‌های بومی و ساخت Debug

ZIP سورس شامل درخت کامل زیرماژول‌ها با revision مبنا است. اگر از checkout گیت استفاده می‌کنید، ابتدا `git submodule update --init --recursive` را اجرا کنید. فایل‌های تولیدی AAR/SO داخل تحویل سورس قرار نگرفته‌اند؛ اسکریپت زیر همان مراحل upstream را انجام می‌دهد:

```bash
export JAVA_HOME=/absolute/path/to/jdk-21
export ANDROID_HOME=/absolute/path/to/android-sdk
export NDK_HOME="$ANDROID_HOME/ndk/30.0.16248370"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
sdkmanager 'platforms;android-37.0' 'build-tools;37.0.0' 'platform-tools' 'ndk;30.0.16248370'
sdkmanager --licenses
bash scripts/build-ispeed.sh debug
```

اسکریپت HEV را برای هر چهار ABI می‌سازد؛ هسته `libv2ray.aar` را از تگ دقیق `v26.10.21` دریافت می‌کند و تست‌ها و ساخت F-Droid را اجرا می‌کند. برای بازسازی AAR از منبع، راهنمای `AndroidLibXrayLite/README.md` را با Go نسخه `go.mod` همان زیرماژول دنبال کنید و خروجی را به `V2rayNG/app/libs/libv2ray.aar` منتقل کنید.

هیچ `-PABI_FILTERS` ارسال نکنید، چون ساخت Universal را غیرفعال می‌کند. فایل مورد انتظار پس از موفقیت:

`V2rayNG/app/build/outputs/apk/fdroid/debug/iSpeedTopVPN_1.2.1-fdroid_universal.apk`

بعد از آماده‌شدن ورودی‌های بومی، در Windows می‌توانید از پوشه `V2rayNG` اجرا کنید:

```powershell
.\gradlew.bat :app:testFdroidDebugUnitTest :app:compileFdroidDebugKotlin :app:assembleFdroidDebug
```

نصب Debug با کلید آزمایشی خود Gradle است؛ برای انتشار استفاده نشود:

```bash
adb install -r V2rayNG/app/build/outputs/apk/fdroid/debug/iSpeedTopVPN_1.2.1-fdroid_universal.apk
```

با Android Studio / APK Analyzer شناسه `com.ispeedtopvpn.app` و وجود هر سه فایل زیر را برای هر چهار معماری داخل APK بررسی کنید:

- `lib/<abi>/libgojni.so`
- `lib/<abi>/libhev-socks5-tunnel.so`
- `lib/<abi>/libhevsockstun.so`

## Release و امضا

```bash
bash scripts/build-ispeed.sh release
```

خروجی Release بدون تنظیم امضا، unsigned است. کلید را **خارج مخزن** ایجاد و نگهداری کنید؛ هیچ کلید انتشار یا رمز عبوری در این تحویل وجود ندارد:

```bash
keytool -genkeypair -keystore /secure/outside-repository/ispeed-release.jks -alias ispeed -keyalg RSA -keysize 3072 -validity 10000
```

مسیر خروجی unsigned را از Gradle بخوانید. `UNSIGNED_APK` را به همان مسیر اختصاص دهید. رمزها را تعاملی وارد کنید؛ در فرمان یا فایل گیت ننویسید:

```bash
"$ANDROID_HOME/build-tools/37.0.0/zipalign" -P 16 -f 4 "$UNSIGNED_APK" /secure/output/ispeed-aligned.apk
"$ANDROID_HOME/build-tools/37.0.0/apksigner" sign --ks /secure/outside-repository/ispeed-release.jks --ks-key-alias ispeed --out /secure/output/iSpeedTopVPN-release-signed.apk /secure/output/ispeed-aligned.apk
"$ANDROID_HOME/build-tools/37.0.0/apksigner" verify --verbose --print-certs /secure/output/iSpeedTopVPN-release-signed.apk
```

بعد از امضا APK را تغییر ندهید. برای همه به‌روزرسانی‌ها همان کلید خصوصی را نگه دارید. سورس متناظر، `LICENSE`، `FORK-NOTICE.md` و اطلاعات مجوز وابستگی‌ها را همراه توزیع منتشر کنید. Debug و Release با کلیدهای متفاوت معمولاً روی یکدیگر نصب نمی‌شوند؛ قبل از حذف برنامه برای تعویض امضا، از کانفیگ‌های خود نسخه پشتیبان بگیرید.

## استفاده

هیچ سروری از پیش اضافه نشده است. از منوی + لینک موجود در کلیپ‌بورد، فایل یا QR را وارد کنید یا کانفیگ دستی بسازید. اشتراک، مسیریابی، DNS، زبان و تم از منوی برنامه در دسترس‌اند. برای آمار بایت، نمایش سرعت را در تنظیمات فعال کنید و دوباره متصل شوید. علامت «—» داده ناموجود است؛ صفر یا اتصال ساختگی نمایش داده نمی‌شود. نام سرور انتخاب‌شده ممکن است با سرور نشست در حال اجرا متفاوت باشد تا زمانی که سرویس را با انتخاب جدید restart کنید.
