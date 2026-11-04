نسخه ۱٫۵٫۸ با نام Special VPN و شناسه com.specialvpn.app و همان کلید انتشار نسخه ۱٫۵٫۷ ساخته می‌شود. APK نسخه Release روی Release قبلی با همین پکیج قابل ارتقاست؛ نصب عملی روی گوشی هنوز آزمایش نشده است. پکیج قدیمی iSpeedTopVPN جداست و انتقال از آن به خروجی و ورود تنظیمات نیاز دارد.

# انتشار امضاشده ۱٫۵٫۸

خروجی `SpecialVPN-1.5.8-universal-release.apk` واقعاً با `assembleFdroidRelease` ساخته و سپس هم‌تراز و با کلید انتشار اختصاصی امضا شده است. کلید خصوصی بیرون از سورس نگهداری می‌شود و در ZIP نیست. جزئیات نتیجه‌ها و محدودیت‌ها در `VALIDATION.md` است.

اثر انگشت SHA256 گواهی عمومی انتشار:

`3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8`

هش خود APK با هش گواهی متفاوت است؛ هش APK را از SHA256SUMS-1.5.8.txt خروجی بخوانید. برای هر نسخه بعدی همان کلید خصوصی را استفاده کنید. کلید و رمز را امن و جداگانه بکاپ بگیرید؛ گواهی عمومی جایگزین کلید خصوصی نیست.

## انتقال از Debug

Debug و Release امضای متفاوت دارند. قبل از حذف Debug، از کانفیگ‌ها و اشتراک‌ها خروجی بگیرید و سالم‌بودن فایل را بررسی کنید. سپس Debug را حذف، Release را نصب و بکاپ را وارد کنید. انتقال عملی روی گوشی هنوز آزمایش نشده است. بکاپ می‌تواند رمز و اطلاعات حساس داشته باشد؛ آن را عمومی نکنید.

## ساخت مجدد

ابزارها و پیش‌نیازهای همان سورس در BUILD-FA.md آمده است. در پوشه V2rayNG و با Android SDK/JDK تنظیم‌شده:

```powershell
.\gradlew.bat :app:assembleFdroidRelease
```

خروجی پیش از امضای دستی:

`app/build/outputs/apk/fdroid/release/SpecialVPN_1.5.8-fdroid_universal.apk`

برای امضای همان فایل، متغیرهای مسیر را با مسیرهای واقعی جایگزین کنید. apksigner رمز را تعاملی درخواست می‌کند؛ رمز را در فرمان یا گیت ننویسید:

```powershell
$tools = Join-Path $env:ANDROID_HOME 'build-tools/37.0.0'
$unsigned = 'app/build/outputs/apk/fdroid/release/SpecialVPN_1.5.8-fdroid_universal.apk'
& "$tools/zipalign.exe" -P 16 -f 4 $unsigned 'ispeed-aligned.apk'
& "$tools/apksigner.bat" sign --ks 'C:/secure/ispeedtopvpn-release.p12' --ks-key-alias ispeedtopvpn --out 'iSpeedTopVPN-release.apk' 'ispeed-aligned.apk'
& "$tools/apksigner.bat" verify --verbose --print-certs 'iSpeedTopVPN-release.apk'
& "$tools/zipalign.exe" -c -P 16 4 'iSpeedTopVPN-release.apk'
```

کلید جدید نسازید اگر کلید انتشار قبلاً ساخته شده است. فایل امضاشده را بعد از امضا تغییر ندهید. سورس متناظر و مجوز GPL را همراه توزیع در دسترس قرار دهید.

## Play Protect

امضای اختصاصی، Release بودن و بررسی استاتیک، حذف هشدار یا تأیید امنیت گوگل را تضمین نمی‌کنند. هشدار روی Debug گزارش شده است؛ نتیجه نسخه Release نیاز به آزمایش روی گوشی دارد. مسیر رسمی بررسی: https://developers.google.com/android/play-protect/warning-dev-guidance

فرم درخواست: https://support.google.com/googleplay/android-developer/contact/protectappeals

پیش‌نویس و گزارش بررسی بیرون از سورس، در بسته play-protect-review آماده شده‌اند. ارسال فرم و بارگذاری APK در VirusTotal انجام نشده است؛ فرم باید دقیقاً نسخه‌ای را معرفی کند که هشدارش بازتولید شده است. اطلاعات هویت/حساب را نمی‌توان حدس زد و ثبت توسعه‌دهنده با درخواست بررسی امنیت یکی نیست.

بررسی آپدیتِ پروژه مبنا همچنان انتشارهای رسمی V2rayNG را نشان می‌دهد؛ آن فایل‌ها آپدیت امضاشده این فورک نیستند. تا ارائه نشانی انتشار اختصاصی، آپدیت iSpeedTopVPN را از خروجی‌های امضاشده همین پروژه دریافت کنید.
