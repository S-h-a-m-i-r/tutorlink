# TutorLink — fee register for home tuition teachers

Android, Kotlin + Jetpack Compose, entirely local (Room + DataStore). No server,
no accounts, no runtime permissions.

**The specs live in `~/Desktop/teacherPD/`** and are the source of truth:

| Doc | What it decides |
|---|---|
| `v1-pitch.md` | Scope, the operating rhythm, how to pitch it |
| `data-model.md` | Schema, the fee rules, period generation, allocation |
| `design.md` | Palette, type, components, screens, receipt, logo |
| `onboarding-and-auth.md` | No accounts; the teacher profile; when auth arrives |
| `play-store-policy.md` | Permission strategy, Data Safety, release traps |

If the code disagrees with those documents, the code is wrong.

## Build and run

```bash
./gradlew :app:test            # 82 unit tests — the fee logic
./gradlew :app:assembleDebug

# emulator
~/Library/Android/sdk/emulator/emulator -avd Pixel9_API36 &
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n pk.groscale.feeregister/.MainActivity
```

## What exists so far — v1 feature-complete

The app runs end to end and every v1 promise from `teacherPD/v1-pitch.md` is built.
Screenshots of the whole flow are in `teacherPD/screens/`.

- **Onboarding** — welcome, three conversational questions, first batch, first
  student. `setupComplete` is the only routing flag; there is no account.
- **Mid-month joining, both ways** — a student joining on the 18th is billed
  18–30 and then the 1st-of-month cycle, or stays on his own 18-to-18 cycle
  (`domain/JoiningCycle.kt`). The add-student form asks only when the join date
  is not the 1st — on the 1st all three answers bill the identical thing — and
  prices each option by running the real generator, so the teacher reads the
  actual bills out to the parent before either agrees.
- **Invoice generation** on every app *resume*, idempotent via the unique
  `(enrollmentId, periodStart)` index, and at most once per calendar day. Runs
  off the main thread. Students are never re-entered: every active enrollment is
  billed again on the 1st, for ever. Resume rather than ViewModel `init` because
  a phone left backgrounded over the 30th keeps the process alive, and the month
  would otherwise roll over without any bills being made.
- **This month** — pending hero, collection bar, unpaid/paid sections, month
  arrows clamped to a 36-month window. An empty month says *why* it is empty:
  next month is always blank until its 1st, and a bare "nothing here" reads as
  lost data to a teacher who then thinks he must add everyone again. Arrears are carried onto the row and into
  the hero, so the pending total is everything owed; the collection bar stays
  scoped to this month's bills, which is what it means.
- **Receive payment** — oldest-invoice-first allocation, partial payments,
  advance credit. Guarded against double submission.
- **Receipt** — 1080x1440 PNG rendered on a raw Canvas so it looks identical on
  every phone, shared through the system sheet.
- **WhatsApp reminders** — Roman Urdu, PK number normalisation, **sequential**
  (deep links open one chat at a time, so there is no "send to all"), with
  `lastRemindedAt` so it does not repeat within a day.
- **Attendance** — tap only the absentees; only absences are stored.
- **Editing** — student name, parent's phone, and fee. A fee edit re-prices the
  bill for the period still running, so long as no money has landed on it; closed
  months and paid bills keep the fee they were billed at (`domain/Repricing.kt`).
- **Leaving** — soft, so history survives. Answers the leave-mid-period question
  design.md section 12 left open, and answers it **prorate**
  (`domain/Settlement.kt`): the part-month is cut to the class days actually
  taught via a negative `adjustment`, so the bill still says what the parent was
  shown. Before confirming, the teacher is told which way the money goes — still
  to collect, or **to give back**. The give-back figure is his alone: it is never
  put in a reminder or a receipt, because whether it is given is his call to make
  face to face.
- **Backup** — human-readable versioned JSON via the Storage Access Framework
  (no permissions), and replace-all restore in one transaction behind an explicit
  confirmation. An example file is at `teacherPD/example-backup.json`.
  Restore is reachable from **both** Settings and the welcome screen; the welcome
  one matters more, because a teacher holding a new phone has no Settings tab yet
  and the restored profile carries `setupComplete`, so the app routes itself to
  Home. Both share `ui/components/BackupFilePicker.kt` — only the confirmation
  differs, since one has a register to lose and the other does not.
- **Absences can be told to the parent** — marking a student absent offers to open
  WhatsApp with a Roman Urdu note written. Offered only on present → absent, only
  when a number is on file, and never sent automatically.
- **Settings** — tuition details, reminders, backup, restore.
- **Reminders** — two, both **off until switched on**: one when a batch starts
  ("mark who did not come", carrying what is still pending), one on the morning a
  fee falls due. `domain/Reminders.kt` decides *when*; the Android side only owns
  the alarm and the notification.

  Play compliance is the shape of this feature, per `play-store-policy.md` §1:
  `POST_NOTIFICATIONS` is asked for **at the switch, never at first launch**, and
  a refusal leaves the switch off rather than lying. Alarms are **inexact**
  (`setAndAllowWhileIdle`) so `SCHEDULE_EXACT_ALARM` — a declaration form and a
  common rejection — is never needed. Two channels, so fee reminders can be
  silenced from Android's own settings without losing attendance ones.
  Notifications are `VISIBILITY_PRIVATE`: what a parent owes is not for a
  stranger glancing at the teacher's lock screen. `BootReceiver` is the only
  exported component and does nothing but re-book alarms.

  The shipping APK declares exactly `POST_NOTIFICATIONS`,
  `RECEIVE_BOOT_COMPLETED`, and the signature-level
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` that androidx.core injects. Verified
  with `aapt2 dump permissions` on the release APK, not by reading the source
  manifest.

82 unit tests cover the fee rules, both billing anchors, class-day counting,
rounding, re-pricing after a fee edit, the joining-cycle choice, the leaving
settlement, reminder scheduling, allocation and money formatting. `InvoiceGeneratorTest` covers the fee rules and the period
rules *together* — both halves were right on their own while the bill they
produced between them was not.

## Hardening

Verified, not assumed:

- **3,000-event monkey run: 0 crashes, 0 ANRs**, app alive and coherent
  afterwards, long junk names ellipsised without breaking the amount column.
- **Every button and row is tap-throttled** (`ui/components/Throttle.kt`). A fast
  double tap on Save would otherwise be two payments.
- **Every write goes through `safely()`** (`util/Safely.kt`) and surfaces a plain
  sentence in a snackbar. Nothing reaches the default crash handler, and
  `CancellationException` is rethrown rather than swallowed.
- **Heavy work is off the main thread**: invoice generation, payment allocation,
  bitmap rendering, backup encode/decode, file IO.
- **StrictMode in debug** catches main-thread disk and network work.
- **Room migrations are mandatory** — `data/db/Migrations.kt`. There is no
  `fallbackToDestructiveMigration`, deliberately: silently wiping a teacher's
  register on update is worse than failing loudly. The v1 to v2 migration was
  tested by installing over a populated v1 database.
- **Every Material colour slot is set**, so the purple baseline cannot leak into a
  dialog or sheet.

## Still not built

- Sibling grouping in the UI (the `Family` table exists and is written to, so one
  combined bill per family is a UI change, not a schema change)
- Editing a batch after creation
- Holidays UI (the table and the fee maths already honour them)
- Optional biometric app lock
- Tests, marks, result cards — deliberately v2

## Store listing and release

`teacherPD/` was lost in the Sep 2026 Mac migration. Its Play documents are
replaced by `play-store/`:

- `CHECKLIST.md` — every Play Console step and form answer, in order
- `listing.md` — title, short and full description, store settings
- `privacy-policy.html` — fill the placeholders, then host it publicly
- `icon-512.png`, `feature-graphic-1024x500.png`, `screenshots/`

Release builds are signed with the upload key in `upload-keystore.jks`, read
through `keystore.properties` at the project root. Both are git-ignored and must
be backed up outside this folder.

```bash
./gradlew :app:test :app:bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

## Notes for later

- `applicationId` is `pk.groscale.feeregister` and **must never change** — the
  display name is provisional and lives only in `strings.xml`.
- The launcher mark is a first pass. Brief is in `design.md` section 9.
- IBM Plex Sans is not bundled yet; `ui/theme/Type.kt` has the TODO. The scale and
  weights are already correct, so it is a one-line swap.
- kotlinx-serialization is in the version catalog but not applied — wire it in
  with the backup feature. AGP 9 provides Kotlin itself, so only the serialization
  compiler plugin needs adding.
