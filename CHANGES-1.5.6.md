# 1.5.6 — streamlined dashboard actions

- Remove the header plus and overflow buttons from subscriptions, servers and settings, and the overflow button from Home. Protocols retain their separate navigation/content. Per-server action buttons are unchanged.
- Replace Home's refresh icon with a gold text button. English shows only “Update Subscription”; Persian shows only “آپدیت اشتراک”. It dispatches the same existing update-all-subscriptions action and uses the real update result banner.
- Remove the Manage footer from the subscription tab. Add subscription (including its empty state) opens the same existing import dialog as the supplied screenshot, with link, clipboard, QR, file and additional import-method actions. Import work remains owned by MainViewModel/MainActivity; no new parsing or persistence path is introduced.
- Rename the selected subscription card's Refresh text to “Update Sub” in English and “آپدیت اشتراک” in Persian. Per-card update icon actions retain localized names and their original behavior.
- Keep advanced operations available through a text entry in the settings tab, using the existing menu/actions and deletion confirmations. No three-dot header is needed.
- Version1.5.6 / base versionCode769. Subscription selection, quota/history, actual daily picker, OpenVPN/Cisco, package, signing certificate, permissions and native engines remain unchanged.
