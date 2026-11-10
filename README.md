# InstaChat

A native Android fake-DM simulator for pranks and storytelling. You build the
fictional friend, write both sides of the conversation, and InstaChat presents
it in a polished, dark messaging UI. Everything is local to the device; no
network permission, accounts, or real messages are involved.

> **Simulated chat.** The app is explicitly labelled in Settings and in the
> About section. It is not affiliated with any social network. Main UI icons
> are adapted from a public Figma community set; avatars, images, and the logo
> are original to this project.

## Stack

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01), Material 3
- Single Activity + Navigation Compose, MVVM
- Hilt 2.52 for dependency injection
- Room 2.6.1 with Flow DAOs, repository layer, exported schema, migrations
- DataStore Preferences for app settings
- Coil for local image loading
- MediaRecorder / MediaPlayer for voice notes
- Photo Picker / Document provider for media import (no media permission needed)
- System Roboto type with a compact, high-contrast dark layout

## Features

### Virtual contacts
- Create/edit any number of fictional friends: name, username, bio, profile
  picture from gallery or camera.
- Per-conversation gradient theme (Default, Sunset, Ocean, Midnight), a
  selectable default theme for new friends, and light/dark/system app
  appearance. The app opens in its signature dark look by default.
- "Show as active now" per contact, reflected in the chat header.
- Generated, original illustrated seed avatars and a synthetic demo photo.

### Inbox and chat
- Centered inbox header, dark search pill, story-style activity row, unread
  markers, and a five-item bottom bar.
- 64dp avatars, last-message preview plus inline time, and no divider clutter.
- Black chat canvas, uppercase day/time phase separators, grouped bubbles.
- Magenta → purple gradient sent bubbles and `#262626` received bubbles.
- "Seen Friday" style receipts; per-message timestamps are available in the
  long-press message sheet and can be edited there.
- Full-screen photo viewer with pinch-zoom and swipe-down dismiss.
- Voice notes with 32-bar waveform; hold to record, slide left to cancel, slide
  up to lock, then send from the recording pill.

### Scripted conversations
- **Manual mode:** open the composer's plus sheet, choose the friend under
  “Sending as”, and type their side yourself. The composer placeholder then
  reads “Reply as …”.
- **Queue / auto mode:** add ordered replies with individual reply delays and
  typing-indicator durations. Enable *Auto-reply after my messages* and every
  message you send gets the next scripted reply. Use the composer's plus sheet
  to run one reply or auto-play the whole queue. Animated typing dots appear
  before each reply.
- Per-contact typing simulation toggle; reorder by arrows or edit any line.
- **Backfill** messages at any past timestamp, or edit a message's exact date
  and time through the long-press sheet.
- **Screen-record view** hides script/editing chrome, keeps the screen awake,
  and auto-plays the queue with typing indicators. Use the phone's native
  screenshot/recording controls — there is no custom export engine.
- The clean chat header keeps outlined call/video buttons; they explain that
  calls are not part of the simulation.

### Media
- Photos from the system Photo Picker and camera. Images are copied into
  `filesDir/media/images`; the database stores the app-private path.
- Voice notes recorded hold-to-send via `MediaRecorder` as `.m4a`, stored in
  `filesDir/media/voice`, with a live recording waveform.
- Import any audio file as a **friend's** voice note from the composer’s + sheet.
- Playback shows a generated waveform, elapsed fill, duration, and play/pause.

## Screens

| Route | Screen |
| --- | --- |
| `inbox` | Conversations, search, unread dots |
| `chat/{contactId}` | Message history, script controls, composer |
| `chat/{contactId}?presentation=true` | Clean screen-record view |
| `contact_editor/{contactId}` | New/edit fictional friend (`-1` = new) |
| `script/{contactId}` | Queue, delays, auto-reply and typing switches |
| `theme/{contactId}` | Per-conversation theme picker |
| `settings` | Appearance, simulation, permissions, data, about |

## Architecture

```
app/src/main/java/com/instachat/app/
├── InstaChatApp.kt              # @HiltAndroidApp + one-time demo seeding
├── MainActivity.kt              # single activity, app theme, Navigation
├── data/
│   ├── local/                   # Room entities, DAOs, converters, database
│   ├── media/                   # private media storage + MediaRecorder wrapper
│   ├── prefs/                   # DataStore settings
│   ├── repository/              # ChatRepository + DemoSeeder
│   └── model/                   # inbox projection model
├── di/                          # Hilt modules
├── ui/
│   ├── chat/                    # Chat UI, bubbles, palette, actions sheet
│   ├── contact/                 # Contact editor
│   ├── inbox/                   # Inbox
│   ├── script/                  # Script queue editor
│   ├── settings/                # Settings & About
│   ├── themepicker/             # Per-chat theme picker
│   ├── components/              # Shared avatar, buttons, dialogs, system bars
│   ├── navigation/              # NavHost and routes
│   └── theme/                   # Colors, typography, shapes, themes
└── util/                        # Time formatting, date/time dialogs, helpers
```

`ChatRepository` is the single data entry point. DAOs expose `Flow`s; ViewModels
combine those with UI state. Hardware recording is owned by the visible chat and
released on dispose. Seed media is generated locally on first launch.

## Database

Room database version 4 (`instachat.db`):

- `contacts(id, name, username, bio, avatarPath, themeId, createdAt, autoScriptEnabled, typingSimulationEnabled, activeNow)`
- `messages(id, contactId, sender [ME|FRIEND], type [TEXT|IMAGE|VOICE], text, mediaPath, durationMs, timestamp, status [SENT|DELIVERED|SEEN], reaction, scripted)`
- `themes(id, name, sentColorStart, sentColorEnd, receivedColor, background)`
- `scripts(id, contactId, orderIndex, text, delayMs, typingMs)`

Foreign keys cascade from contacts; indices cover inbox/unread/message queries.
Migrations:

- **1 → 2**: adds `autoScriptEnabled`, `typingSimulationEnabled`, and
  `messages.scripted`.
- **2 → 3**: adds the `(contactId, status)` index for unread counts.
- **3 → 4**: adds `contacts.activeNow` and `scripts.typingMs`, and migrates the
  built-in palettes to the design-system values/names.

The schema is exported to `app/schemas/`. Migration tests create a real v1
database, run all migrations, and validate the final schema and data.

Seed: four themes (Default, Sunset, Ocean, Midnight) and two fictional friends
with a demo conversation (text, reactions, a generated photo, read receipt)
plus five scripted replies with delay + typing durations. Only seeded once;
Settings → Restore demo content resets everything.

## Permissions

- **Microphone** — requested on first hold-to-record press, with rationale and
  a path to Android settings if denied.
- **Camera** — requested before taking a profile photo or chat photo.
- **Photos & media** — deliberately no storage permission: Android's Photo
  Picker and the system document picker provide user-selected access. The
  Settings screen explains this.

All user-chosen media is copied into app-private storage. Clearing chat or a
profile removes its files. The app has no internet permission.

## Build

Requirements: Android SDK 35, JDK 17+ (JDK 21 tested), Android Studio or CLI.

```bash
# sdk.dir is already set in local.properties for this machine
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Install on a running device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests and checks:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # needs a device/emulator
./gradlew lintDebug
```

The instrumentation suite covers the 1→2→3→4 migrations, final-schema validation,
and inbox ordering/unread behavior.

## Design notes

- Tokens: pure black canvas, `#262626` received bubbles/composer surfaces,
  `#1C1C1E` search/sheet surfaces, white primary text, `#8E8E93` secondary text,
  `#0A84FF` blue accent, and a `#C42BC1 → #6C4DFF` magenta-purple sent gradient.
- Typography uses the system font: 24sp bold inbox title, 18sp chat name, 16sp
  message body, 15sp list names, 14sp previews/status, 13sp phase separators.
- Bubbles are capsule-shaped (24dp), content-fit with a 75% max width. Messages
  from the same person in a group stack tightly (1dp), while separate groups get
  16dp of breathing room. All chat themes paint a black canvas and vary only the
  sent gradient.
- The inbox has a story-style activity row, accent Requests label, and five-item
  nav (home, reels, messages, search, profile/settings).
- The composer is a slim 56dp dark pill with a larger accent-blue camera button,
  text field, and mic / gallery / emoji / plus actions. The plus sheet is the
  home for the simulator controls so the normal chat surface stays clean.
- Settings is a quiet grouped list with hairline dividers, drill-down theme
  dialogs, direct permission actions, restore/recording info, and an About
  dialog carrying the simulated-chat label.
- Message arrival slides up 12dp + fades in 200ms; typing dots scale over 400ms
  with staggered starts; reactions pop in 180ms; send/mic cross-fades in 120ms.
- Message bubbles expose TalkBack descriptions such as “Friend said … at 4:52 PM”.
- The DM surfaces use icons adapted from the public
  [“Instagram - UI Icons (Community)”](https://www.figma.com/design/R8Byl2QgjPVH0uZ6bgXxUC/Instagram---UI-Icons--Community-?node-id=5-426)
  Figma set: back, camera, gallery, emoji, plus, send, search,
  home, reels, compose, chevron, reply, moon, lock, play, copy, sparkles, and
  audio. Icons that the set does not cover (call, video, mic, pause, timestamp,
  edit, delete, settings, and info) keep the original hand-drawn vector family.
  XML drawables live in `res/drawable`, with SVG sources in
  `design/icons/svg`; the Material Icons dependency is not included.
- No Instagram/Meta logos, wordmarks, or network branding are used. The app is
  not affiliated with Instagram or Meta. The icon set is a community UI kit;
  seed avatars, seed photos, and the launcher/splash logo are original to this
  project.
