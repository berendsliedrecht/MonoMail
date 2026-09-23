# CLAUDE.md

MonoMail is an e-ink fork of Thunderbird for Android for the Mudita Kompakt (480x800, pure black/white). `MONOMAIL.md` lists every deviation from upstream and the merge conflict policy. `EINK_UI.md` specs the e-ink UI screens and design rules. Read both before touching anything.

## Git workflow (always)

The entire fork is ONE commit ("MonoMail: e-ink MMD fork for Mudita Kompakt") on top of upstream/main. Never stack commits. For any change:

    git add -A
    git commit --amend --no-edit
    git fetch upstream
    git rebase upstream/main
    git push --force-with-lease origin main

## Code rules

- Keep the upstream diff minimal and additive: prefer new files; app resource overrides go in `app-thunderbird/src/main/res/values/monomail_overrides.xml`; only touch upstream files already listed in `MONOMAIL.md` (and update that table when a new one is unavoidable).
- The e-ink UI lives in `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/eink/`. Pure `#000`/`#FFF`, no animation, no ripple (`einkClickable`), MMD components where one exists, paged lists over scrolling.
- The e-ink UI never reimplements mail logic: everything goes through `MessagingController`, the extractors, and the same repositories the classic UI uses.
- MMD 1.0.0's `TopAppBarMMD` crashes against this repo's Material3; use the hand-built `EinkTopBar` in `EinkComponents.kt`.

## Build and install

Only `app-thunderbird` is built. The phone runs the debug-signed foss release build (package `com.monoapps.monomail`, no `.debug` suffix):

    ./gradlew :app-thunderbird:assembleFossRelease
    adb install -r app-thunderbird/build/outputs/apk/foss/release/app-thunderbird-foss-release.apk

`installDebug` is ambiguous (foss/full flavors) and would install a separate `.debug` package that does not update the app on the phone.

Testing on the device is done by the user: install the build, list what to check, wait for feedback. Do not drive the UI over adb.
