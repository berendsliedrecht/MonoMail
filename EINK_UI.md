# MonoMail e-ink UI

A ground-up UI for the Mudita Kompakt (480x800 e-ink), replacing Thunderbird's main screens while reusing its mail engine. MMD components wherever they fit; custom black/white Compose where they don't.

## Principles

- Pure `#000`/`#FFF`. No grays, no elevation, no shadows, no ripple, no animation.
- Paged, never scrolled: every list shows a fixed page of rows with prev/next controls and a `page/total` indicator. One full e-ink refresh per page.
- One idea per screen. Top bar: bold title + at most two actions. Bottom bar: paging + primary action. 2dp black rules separate the bars from content.
- Tap targets at least 48dp. Long-press opens an MMD bottom sheet with secondary actions; no swipe gestures.
- Emphasis through weight and inversion (selected/unread = bold or black-on-white inverted), never color.

## Screens

### Inbox / message list (home)
- Top bar: folder name (bold), unread count, actions: search, folders.
- Rows (8 per page): line 1 = sender (bold when unread) + short date right-aligned; line 2 = subject; optional star/attachment glyphs. No avatars, no preview text.
- Bottom bar: `[folders] [<] page/total [>] [compose]`.
- Long-press row: bottom sheet with read/unread, star, archive, delete, move.
- Empty page: "No messages." centered. Syncing shown as text ("Syncing...") in the top bar subtitle slot, no spinner.

### Folders
- Top bar: account name (tap = account switcher), action: sync all.
- Rows: folder name + unread count right-aligned, paged.
- Unified inbox row pinned first when more than one account.

### Accounts
- Simple paged list: account name + email, current one inverted. Tap = switch and return to inbox. Last row: "Add account" (opens existing setup flow).

### Reader
- Top bar: back + position in list (`3/41`).
- Header: sender name and address, subject (bold, larger), date, to/cc summary. Rule below.
- Body: extracted plain text, 16sp, generous line height. Discrete page-jump: body advances one viewport per tap on next/prev, no fling scrolling.
- Attachments listed as rows under the body, in both the text and original views. Tap saves to the system Downloads folder (fetching from the server first when the content is not local), with inline status text ("Downloading...", "Saved to Downloads").
- Bottom bar: `[< prev] [reply] [more] [next >]` where more = bottom sheet (reply all, forward, star, unread, move, delete, view original).
- "View original" swaps the body for the classic WebView HTML rendering inside the same chrome; back returns to text.

### Search
- Top bar becomes an MMD text field. Results identical to inbox rows, paged. Searches the active account (all folders).

### Compose
- V1 launches the existing composer (fully functional: attachments, drafts, identities, encryption), already black/white themed. A native MMD composer is a later phase.

### Kept legacy (black/white themed)
Settings, account setup, OAuth. Reached from the folders screen overflow.

## Architecture

- New package `com.fsck.k9.ui.eink` inside `legacy/ui/legacy` (new files only): single `EinkMailActivity` hosting all screens with manual navigation (enum + back stack list), one ViewModel per screen concern, backed by the same repositories/controllers the current UI uses.
- The app's launcher/startup routes to `EinkMailActivity` instead of `MessageHomeActivity`. Notifications and widgets keep working by routing through the same entry points.
- Old screens remain in the APK (settings etc.); nothing is deleted, keeping the upstream diff additive.
