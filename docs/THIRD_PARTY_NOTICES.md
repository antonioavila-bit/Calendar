# Source, artwork and offline-input provenance

This is an independent customization in antonioavila-bit/Calendar, not an official Fossify release. The GPLv3 LICENSE and upstream copyright notices are retained. Original Fossify source is based on commit 764b24cda492345653bd18c36d2498a01bd8eb76. New scheduling source is in org.fossify.calendar.security. The first preview uses a dedicated normalized scheduling workspace; it does not import or synchronize the inherited personal-calendar events. Legacy source remains for future adaptation, but its activities, OS calendar-provider paths and sync receivers are not runtime entry points in this APK.

The native handwriting adapter follows the owner's expressly authorized read-and-copy-only Inventory-App code, commit 260194c0766c280f82c76a1b03f19df0770575fb, app/src/main/java/com/inventory/offline/MainActivity.kt around lines 2928–3024. No donor repository was changed. This integrates the device's installed input method; it is not a bundled recognition model. Offline recognition still requires an installed offline-capable keyboard/language configuration.

## Approved launcher image
The latest user-supplied navy seahorse/calendar/clock artwork supersedes the earlier blue calendar icon. The bitmap is mechanically resized without redrawing or replacing its contents. Adaptive-icon padding preserves the full composition against launcher masks.

Original conversation PNG SHA-256: ba4896d9ac339b7d85df3562c0121d1dc70c6792c9a2d0afa06e2a335b2d3a8c.
Packaged 256px WebP SHA-256: c62ad97916c00194c5f1c82d313e9884b5f5c5454fcf85924084301b01550cc7.
The original full-resolution PNG remains in the conversation, not in the repository. The packaged WebP is committed at app/src/main/res/drawable-nodpi/shift_brand.webp.

## Packaging
The source archive accompanies each CI APK artifact. The preview uses an Android debug signature, not a production signing identity. Do not publish private signing keys. Production signing and actual S25/Tab S7 acceptance remain separate release gates. CI downloads build tools/dependencies; the installed application does not download dependencies or use a network API. User-selected external SMS/share/document/keyboard apps operate under their own permissions.
