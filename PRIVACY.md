# Special VPN — حریم خصوصی / Privacy

این صفحه رفتار نرم‌افزار Special VPN را توصیف می‌کند. نسخهٔ به‌روزرسانی: ۲۰۲۶/۱۱/۰۵.

## داده‌های اتصال

لینک اشتراک، کانفیگ‌ها و پروفایل‌هایی که وارد می‌کنید برای اتصال روی دستگاه نگهداری می‌شوند. لینک اشتراک یا فایل خروجی ممکن است اطلاعات دسترسی داشته باشد؛ کپی، QR یا اشتراک‌گذاری آن‌ها با درخواست خود کاربر انجام می‌شود. هیچ سرور یا حساب آماده‌ای در برنامه قرار داده نشده است.

## درخواست‌های شبکه

- اتصال VPN با سروری انجام می‌شود که کاربر انتخاب کرده است؛ ارائه‌دهندهٔ VPN و وب‌سایت مقصد سیاست‌های مستقل خود را دارند.
- به‌روزرسانی ساب و دریافت سهمیه/تاریخچه به نشانی ارائه‌دهندهٔ همان اشتراک درخواست می‌فرستد. تاریخچهٔ سازگار پاسارگارد از endpoint مخصوص همان لینک دریافت می‌شود؛ کلید مدیریتی لازم نیست.
- بررسی IP خروجی از ipify و تشخیص کشور همان IP از country.is استفاده می‌کند. این سرویس‌ها نشانی خروجی درخواست را می‌بینند. تصاویر پرچم داخل برنامه هستند.
- تست تأخیر و بررسی اتصال با مقصد آزمون تنظیم‌شده تماس می‌گیرند. DNS و مسیریابی به تنظیمات کاربر و کانفیگ بستگی دارند.

ردیاب و تبلیغی به این فورک اضافه نشده است. اطلاعات حساب مشتری و کلید خصوصی امضای انتشار در مخزن یا APK تعبیه نمی‌شوند. گزارش خطا و فایل‌هایی که کاربر برای پشتیبانی می‌فرستد باید قبل از ارسال از رمز، لینک خصوصی و کلید پاک شوند.

بررسی نسخهٔ جدید برنامه با GitHub API همین مخزن تماس می‌گیرد. دریافت APK با انتخاب کاربر انجام می‌شود و نصب نیازمند تأیید کاربر در اندروید است. کلید دسترسی گیت‌هاب داخل برنامه نیست.

## کنترل و پشتیبانی

کاربر می‌تواند اشتراک و پروفایل را حذف کند یا دادهٔ برنامه را از تنظیمات اندروید پاک کند. حذف فایل خروجی‌ای که جداگانه ذخیره شده، نیاز به حذف همان فایل دارد. برای پشتیبانی از [Issues مخزن](https://github.com/sabialballa/iSpeedTopVPN/issues) یا [کانال](https://t.me/iSpeedTopVPN) استفاده کنید؛ اطلاعات ورود را عمومی نفرستید. سورس و ریلیزهای مخزن عمومی و برای همه قابل مشاهده‌اند.

مجوزها و اعتبار مؤلفان متن‌باز در بخش About برنامه و سورس حفظ شده‌اند. سیاست تاریخی بالادستی در [CR.md](CR.md) نگه داشته شده و جایگزین توضیح رفتار این فورک نیست.

## English

Special VPN stores user-supplied subscription/configuration and VPN profiles on the device for connection. Subscription refresh and supported usage history contact that subscription provider. Exit-IP lookup uses ipify; country lookup uses country.is for the exit IP. Latency checks contact the configured test destination. VPN servers, DNS resolvers and destinations are determined by user configuration.

No advertising or tracking SDK was added to this fork. Sharing a subscription link, QR or exported configuration is user initiated and may disclose access credentials. Release artifacts do not embed customer accounts or private signing keys. Remove secrets before sending support reports. Users can remove subscriptions/profiles or clear Android app data; independently exported files must be removed separately.

Source and support: [repository](https://github.com/sabialballa/iSpeedTopVPN), [Issues](https://github.com/sabialballa/iSpeedTopVPN/issues), [Telegram](https://t.me/iSpeedTopVPN). Update checks contact this repository’s GitHub releases API; downloading is user initiated and installation requires Android/user confirmation. No GitHub credential is embedded. Source and releases are public and available without login. Original open-source notices are retained.
