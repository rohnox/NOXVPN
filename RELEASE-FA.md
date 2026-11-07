# انتشار و امضای Special VPN

پکیج انتشار com.specialvpn.app است. برای ارتقای نسخه‌های نصب‌شده، همان کلید انتشار را نگه دارید؛ ساخت کلید جدید باعث ناسازگاری امضای ارتقا می‌شود. کلید خصوصی و رمز باید بیرون از مخزن و ZIP سورس نگهداری و جداگانه پشتیبان‌گیری شوند.

اثر انگشت SHA-256 گواهی انتشار فعلی:

`3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8`

این مقدار هش گواهی است، نه هش APK. هش فایل‌ها در SHA256SUMS هر ریلیز قرار دارد. گواهی عمومی جایگزین کلید خصوصی نیست.

## ساخت و امضا

پس از آماده‌سازی طبق [راهنمای ساخت](BUILD-FA.md)، از V2rayNG اجرا کنید:

```powershell
.\gradlew.bat :app:assembleFdroidRelease
$tools = Join-Path $env:ANDROID_HOME 'build-tools/37.0.0'
$unsigned = 'app/build/outputs/apk/fdroid/release/SpecialVPN_1.6.8-fdroid_universal.apk'
& "$tools/zipalign.exe" -P 16 -f 4 $unsigned 'specialvpn-aligned.apk'
& "$tools/apksigner.bat" sign --ks 'C:/secure/release.p12' --ks-key-alias YOUR_EXISTING_ALIAS --out 'SpecialVPN-release.apk' 'specialvpn-aligned.apk'
& "$tools/apksigner.bat" verify --verbose --print-certs 'SpecialVPN-release.apk'
& "$tools/zipalign.exe" -c -P 16 4 'SpecialVPN-release.apk'
```

مسیر و alias مثال را با کلید موجود خودتان جایگزین کنید. apksigner رمز را تعاملی می‌پرسد؛ رمز را در فرمان یا گیت ننویسید. فایل APK را پس از امضا تغییر ندهید.

## انتشار

versionCode و versionName را افزایش دهید، بررسی‌ها و محدودیت‌های اجراشده را مستند کنید، APK امضاشده و سورس متناظر را همراه مجوز GPL در ریلیز مخزن SpecialVPN قرار دهید. فایل کنترل SHA-256 و گواهی عمومی را اضافه کنید؛ نسخهٔ Debug برای انتشار عادی لازم نیست. بررسی آپدیت داخل برنامه از ریلیزهای همین مخزن استفاده می‌کند و نصب با تأیید کاربر انجام می‌شود.

Debug و Release امضای متفاوت دارند. پیش از هر حذف یا جابه‌جایی نصب، از تنظیمات خروجی بگیرید و سالم‌بودن آن را بررسی کنید. اطلاعات بکاپ ممکن است حساس باشد.

امضای معتبر یا ساخت Release، تأیید Google Play Protect را تضمین نمی‌کند. در صورت هشدار، راهنمای رسمی گوگل را دنبال کنید: https://developers.google.com/android/play-protect/warning-dev-guidance

جزئیات نتیجهٔ ساخت و تست نسخهٔ فعلی در [VALIDATION.md](VALIDATION.md) است.
