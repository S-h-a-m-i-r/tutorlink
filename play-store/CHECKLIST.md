# Publishing TutorLink on Google Play — step by step

Written 29 Sep 2026. Replaces the lost `teacherPD/play-store-policy.md`.
Everything in this folder is ready to upload; the steps below are the ones only
you can do in Play Console.

## What is already done (in the code)

| Requirement | Status |
|---|---|
| Target API 36 (required for new apps since 31 Aug 2026) | ✅ `targetSdk = 36` |
| Signed release bundle (.aab) | ✅ `app/build/outputs/bundle/release/app-release.aab` |
| 16 KB page-size support (required for API 35+) | ✅ verified on both native libs |
| Minimal permissions — no declaration forms needed | ✅ only `POST_NOTIFICATIONS` + `RECEIVE_BOOT_COMPLETED` |
| Notification permission asked in context, not at launch | ✅ |
| No exact alarms (`SCHEDULE_EXACT_ALARM` is a common rejection) | ✅ inexact alarms |
| No internet permission, no ads, no analytics SDKs | ✅ |
| Release build shrunk with R8 (1.6 MB APK instead of 20 MB) | ✅ |
| 82 unit tests pass; 3,000-event monkey test on release: 0 crashes | ✅ |
| Privacy policy + website | ✅ `docs/` on GitHub Pages — fill in name and email (Step 3) |
| Store listing text, icon, feature graphic, 5 screenshots | ✅ `listing.md` + images |

To rebuild after any change:

```bash
cd ~/Desktop/TutorLink
./gradlew :app:test :app:bundleRelease
# upload: app/build/outputs/bundle/release/app-release.aab
```

---

## Step 0 — Back up the upload key (do this today)

`upload-keystore.jks` and `keystore.properties` (project root) sign every release.
They are git-ignored on purpose. **Copy both to two safe places** (e.g. your Google
Drive and a USB stick). The teacherPD folder was lost in the last Mac migration —
don't let this be next.

If they are ever lost it is recoverable (Play Console → Setup → App signing →
request upload key reset), but it takes days.

## Step 1 — Decide before you pay the $25

**Personal or Organization account?** This matters more than anything else here.

| | Personal | Organization |
|---|---|---|
| Cost | $25 once | $25 once |
| Needs | Your CNIC / passport, address | A registered business + **D-U-N-S number** (free, can take 1–4 weeks) |
| **Closed test before going live** | **Required: 12 testers opted in for 14 days in a row** | Not required |
| Name shown on Play | Your name | Business name |

If Groscale is your own registered business, an Organization account skips the
14-day test. Otherwise go Personal and plan the 12 testers now — your 5+ teacher
friends plus family and friends is the easiest route.

Also check:

- Sign up with **your own** Google account (this Mac still has other people's
  Chrome profiles — don't pick theirs).
- Your card must allow **international online payments** (many Pakistani debit
  cards need this switched on in the bank app first).
- The package name `pk.groscale.feeregister` can **never change** after the first
  upload. Keep it only if "groscale" is yours to use; if not, change
  `applicationId` in `app/build.gradle.kts` **before** Step 5.

## Step 2 — Create the developer account

1. Go to <https://play.google.com/console/signup>, choose account type, pay $25.
2. Enter your legal name and address **exactly** as on your CNIC.
3. Verify identity (upload ID), email and phone.
4. Verify you own an Android phone: install the **Play Console** app and sign in.
5. Wait for verification (usually 1–3 days). This also covers Google's new
   Android developer verification — your app is registered to you automatically.

## Step 3 — Host the privacy policy

Play requires a public privacy policy URL for **every** app, even ones that
collect nothing.

The site lives in `docs/` and is served by GitHub Pages from the public repo:

- Website: <https://s-h-a-m-i-r.github.io/tutorlink/>
- **Privacy policy (paste this into Play Console):**
  <https://s-h-a-m-i-r.github.io/tutorlink/privacy/>

1. Replace `[DEVELOPER NAME]` and `[CONTACT EMAIL]` in `docs/index.html` and
   `docs/privacy/index.html`, then commit and push. The name must match the
   developer name on Play; the email should match your Play contact email.
2. One-time setup: GitHub → repo **Settings → Pages** → Source: *Deploy from a
   branch* → Branch `main`, folder `/docs` → Save. The site is live a minute later.
3. Open the privacy URL in a private browser window to check it loads without login.
4. The repo must stay **public** — making it private again takes the policy page
   down, and Play can suspend an app whose policy link is broken.

## Step 4 — Create the app

Play Console → **Create app**:

| Field | Value |
|---|---|
| App name | `TutorLink: Tuition Fee Tracker` |
| Default language | English (United States) |
| App or game | App |
| Free or paid | **Free** (cannot be changed to paid later) |
| Declarations | Tick both (Developer Program Policies, US export laws) |

## Step 5 — App content forms (Policy → App content)

Answers below match what the code actually does.

| Form | Answer |
|---|---|
| **Privacy policy** | Your URL from Step 3 |
| **Ads** | No, my app does not contain ads |
| **App access** | All functionality is available without any access restrictions |
| **Content rating** | Category: *All other app types*. Answer **No** to every question (no violence, sexual content, profanity, drugs, gambling, user interaction, location sharing, digital purchases). Expected result: Everyone / PEGI 3 |
| **Target audience** | **18 and over only.** Do not tick any under-18 age group — that pulls the app into the Families policy. "Could it appeal to children?" → No |
| **Data safety** | "Does your app collect or share any required user data types?" → **No**. Everything stays on the device, and shares the teacher starts (receipt, WhatsApp, backup file) do not count as collection. The listing will show *No data collected · No data shared* |
| **Advertising ID** | No (the app does not use it) |
| **Government app** | No |
| **Financial features** | My app doesn't provide any financial features (it records fees; it doesn't move money, lend or pay) |
| **Health** | No health features |
| **News** | No |

No permission declaration forms are needed.

## Step 6 — Store listing

Paste everything from `listing.md` and upload the images in this folder
(icon, feature graphic, then screenshots 01 → 05). Fill **Store settings** the same way.

## Step 7 — Closed test (Personal accounts only)

1. **Testing → Closed testing → Create track** (or use the default "Alpha").
2. **Testers** tab → create an email list with **at least 12** Gmail addresses.
   Add 15–20 so one or two dropping out doesn't reset the 14 days.
3. **Create release** → upload `app-release.aab`.
   - When asked about **Play App Signing**, accept (Google keeps the app signing
     key; your `upload-keystore.jks` is only the upload key).
   - Paste the release notes from `listing.md`.
4. **Publishing overview → Send changes for review.** The first review can take
   a few days.
5. Once approved, copy the **opt-in link** and send it to testers. Each tester must:
   open the link **with the same Gmail you listed** → "Become a tester" → install
   from Play → keep it installed and open it now and then for **14 days in a row**.

   A message you can send:

   > Assalam o Alaikum! Main ne tuition teachers ke liye fees ka app banaya hai.
   > Google Play pe launch karne ke liye 14 din testing chahiye. Please is link
   > pe apni Gmail se "Become a tester" dabayein, app install karein, aur 14 din
   > tak uninstall na karein: [OPT-IN LINK]. Shukriya!

6. During the 14 days: collect feedback, fix a thing or two, and upload an update
   (raise `versionCode` to 2). Google asks what you changed during testing, and a
   real update makes the production application stronger.

## Step 8 — Apply for production

After 14 days with 12+ opted-in testers, **Dashboard → Apply for production**.
You answer a short questionnaire: how you found testers, how they used the app,
what feedback you got, what you changed. Answer honestly and specifically
(e.g. "5 home tuition teachers used it with their real batches for two weeks…").
Review takes up to about 7 days. If it's refused, Google says why; run the test
longer and apply again.

## Step 9 — Go live

**Production → Create release** → add the tested bundle → **Countries/regions**:
Pakistan (add more if you like) → **Send for review** → roll out.

## Every update after that

1. In `app/build.gradle.kts`, raise `versionCode` by 1 (and `versionName`, e.g. 1.0.1).
2. `./gradlew :app:test :app:bundleRelease`
3. Play Console → Production → Create release → upload → review → roll out.

Once a year Google raises the target API level (usually due 31 August). Watch for
the Play Console email and bump `targetSdk`.
