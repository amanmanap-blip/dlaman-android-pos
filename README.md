# d’Laman Standalone Android POS (Offline)

This project includes the **complete ordering interface inside the APK**. No web domain, Wi-Fi, internet connection or Node.js server is required. Cash/ Card/ QR are *staff-recorded* payments only. The POS58B prints via paired Bluetooth Classic SPP and drives the RJ-connected drawer **only for new confirmed Cash payments**. Reprints never pulse the drawer.

## Important limitations
- Orders, menu changes, reservations, deposits, discounts, and sales exist **only on this tablet**. They are not shared with other devices. Kitchen and drinks tabs are on this same tablet. Printed Dapur/Minuman slips require using the relevant print buttons and pairing the correct printer.
- Data persists in the WebView's localStorage and in a separate native JSON cache. Do not clear app data or uninstall before creating a backup.
- Use **Backup Data** regularly. Use **Restore Data** only when you intend to replace existing data. Save backups somewhere outside the tablet (e.g. Drive/USB).
- The Bluetooth printer must be paired and selected using **BT Printer**; actual printer and drawer commands need physical testing.
- The debug APK is for testing. Production releases should be signed with a stable keystore and be secured with tablet access controls.

## Build APK online with GitHub Actions
Upload the **contents of this project folder** to a GitHub repository root; `.github/workflows/build-apk.yml` is included. Go to Actions > Build dLaman Standalone APK > Run workflow; download the resulting APK artifact.

## App data
Local-only, offline. The Android WebView loads `file:///android_asset/index.html`. Both application interface and 45-item menu are included. No website URL is requested.

## New feature: 2 free drinks per set
- All menu items under Set Kampung prompt for exactly two free drinks: Teh O Ais or Sirap Ais (same or mixed).
- Each new set added prompts separately; cancelled selections do not add a set.
- Free drinks are recorded as zero-cost components and routed to Minuman, while food components remain in Dapur.
- A quantity of 2 sets yields 4 free drinks; the bill remains 2 × the set price.
- Additional order rounds work the same way.
- For a fully offline app, data is stored on the current device and should be backed up regularly.

## New: Packaging charge at checkout
- At **Mark Paid**, a field **Caj Bungkus (RM)** defaults to RM1.00, editable per bill; set it to RM0.00 if no packaging is needed.
- Final balance = menu total + packaging - reservation deposit - discount. This also determines Cash received and change.
- Packaging charge is stored on the original bill, included on 58mm customer receipt/reprint and shown as **Caj Bungkus (Paid)** in the Sales Dashboard.
- Unpaid reversal resets the packaging charge and checkout details; paying again allows a fresh amount.
- Original set-meal free drink choices, Minuman routing and Bluetooth POS58B Cash-only drawer behaviour remain unchanged.
- Keep your previously installed standalone app and back up its data before updating. Keep application ID unchanged and install an APK signed with the same key if upgrading in place.


Packaging charge defaults to RM0.00 at every checkout, editable by staff. Previous bills preserve their saved packaging charges.


## d’Laman launcher icon
The supplied d’Laman emblem is now the app launcher icon. This package includes legacy mdpi–xxxhdpi mipmaps and Android 8+ adaptive launcher icon resources. The package uses the existing applicationId; to update an installed app while preserving data, sign with the same signing certificate. GitHub's debug signing key is not stable across fresh hosted runners.

## Updated official logo (20 October 2026)
The launcher icons were generated from the exact d’Laman logo provided by the restaurant. Android adaptive and standard launcher sizes are included.
