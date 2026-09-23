# MonoMail

MonoMail is an e-ink fork of [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android), styled for black and white e-ink displays like the Mudita Kompakt (480x800). All of Thunderbird's functionality is kept; only the visual layer changes: pure black and white colors and no animations.

## Why not a full MMD rewrite

Thunderbird's UI is far too large to rebuild on MMD components while still tracking upstream. Instead, MonoMail achieves the MMD look (pure `#000000`/`#FFFFFF`, no animation, no dark mode) by overriding Thunderbird's centralized theme layers. This keeps the diff against upstream tiny, so frequent upstream merges stay cheap.

## What MonoMail changes

Every deviation from upstream lives in one of these files:

| File | Change |
|---|---|
| `components/ui/bolt/src/commonMain/kotlin/net/thunderbird/components/ui/bolt/theme/thunderbird/ThemeColors.kt` | Compose (Bolt) palette forced to black/white; dark scheme = light scheme |
| `core/ui/legacy/theme2/thunderbird/src/main/res/values/colors.xml` | Legacy XML palette forced to black/white; dark = light |
| `app-thunderbird/src/main/res/values/themes.xml` | Hardcoded accent colors (swipe actions, star, OpenPGP, bullet points) to black/white; window animations disabled |
| `app-thunderbird/src/main/res/values/monomail_overrides.xml` | New file. App-level resource overrides (contact avatar fallback colors all black) |
| `app-thunderbird/src/main/res/values/strings.xml` | App and brand name: MonoMail |
| `app-thunderbird/build.gradle.kts` | `applicationId` = `com.monoapps.monomail` (installs alongside stock Thunderbird) |
| `app-thunderbird/src/main/AndroidManifest.xml` | Extra intent filters so OAuth redirects on the upstream schemes reach MonoMail |
| `app-thunderbird/src/{debug,release}/kotlin/.../auth/TbOAuthConfigurationFactory.kt` | OAuth redirect URIs hardcoded to the upstream application id (the providers' OAuth clients are registered against those schemes) |
| `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/eink/` | New package: the entire e-ink main UI (paged message list, folders, accounts, search, reader). See `EINK_UI.md` |
| `app-common/.../StartupRouter.kt`, `.../MessageListLauncher.kt` | Entry points route to `EinkMailActivity` instead of `MessageHomeActivity` |
| `legacy/ui/legacy/src/main/java/com/fsck/k9/UiKoinModules.kt` | Registers the eink Koin module |
| `legacy/ui/legacy/src/main/res/anim/*.xml` | All legacy transition animations replaced with zero-duration no-ops |
| `feature/launcher/src/main/kotlin/app/k9mail/feature/launcher/navigation/FeatureLauncherNavHost.kt` | Compose navigation transitions set to `None` |

Only `app-thunderbird` is built and released; `app-k9mail` is left untouched.

## Keeping up with upstream

The fork keeps `main` as the working branch and merges upstream regularly:

    git fetch upstream
    git merge upstream/main
    git push origin main

Conflict policy:

- New files (`MONOMAIL.md`, `monomail_overrides.xml`) never conflict.
- Palette files (`ThemeColors.kt`, `colors.xml`): upstream color tweaks conflict trivially; always keep the MonoMail side (everything is black/white by construction).
- `themes.xml` and `FeatureLauncherNavHost.kt` are the only files where upstream structure changes need real attention; re-apply the black/white values and `None` transitions onto the new upstream structure.
- Prefer adding same-named resources in `monomail_overrides.xml` over editing upstream resource files; app module resources win during resource merging.

## Known caveats

- OAuth (Gmail, Yahoo, AOL, Fastmail, Microsoft) reuses upstream's OAuth clients and redirect schemes; only the `beta` and `daily` build types were not adjusted and would fail with `redirect_uri_mismatch`.
- Compose `AnimatedVisibility` in a few dialogs still animates (touching those files is not worth the merge cost); the effect on e-ink is minor.
- HTML emails render whatever colors the sender used; the e-ink display dithers them.

## Building

    ./gradlew :app-thunderbird:assembleDebug
    ./gradlew :app-thunderbird:installDebug
