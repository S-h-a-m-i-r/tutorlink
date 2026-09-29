# Inkpot — App Store Optimization notes (Google Play, Pakistan)

Written 29 Sep 2026, before launch. Covers why `listing.md`, `listing-ur.md`,
`screenshots-captioned/` and the website `<head>` say what they say, and what
to do after launch.

**Nobody can guarantee a #1 position on Google Play, or any position.** Google
does not publish its ranking method, results differ by phone, language and
person, and they change all the time. What this document can do is make the
listing clearly relevant for the right searches and give the app a fair start.
The rest comes from real teachers installing it, keeping it and rating it.

---

## 1. How the research was done

- **Play search results** for about 60 queries, fetched on 29 Sep 2026 from
  `play.google.com/store/search?...&c=apps&gl=PK&hl=en` (and `hl=ur` for the
  Urdu checks), logged out, so without personalisation. A real teacher's phone
  will show a somewhat different order.
- **Play autocomplete for Pakistan** (the suggestions Play shows while typing),
  used as the demand signal. Play publishes no search volumes, so a term that
  Play autocompletes is treated as "people search this"; a prefix with no
  relevant suggestion is treated as low demand. This tells you whether people
  search a term, not how many.
- **Competitor detail pages** (title, short description, category, installs,
  rating count) for 37 apps.
- English, Roman Urdu and Urdu-script queries.

Findings about search behaviour:

- Play corrects **"tution" to "tuition"** (identical results), so the common
  misspelling needs no targeting.
- Urdu script: Play reads **"فیس" as "face"** and returns face-editing apps;
  "ٹیوشن", "ٹیوشن فیس", "فیس رجسٹر" return **no tuition fee app at all**. No
  competitor has an Urdu listing for this use.
- Roman Urdu: "fees ka …" produces **no relevant autocomplete**; "hisaab" and
  "khata" belong to shop and udhaar ledgers (Easy Khata 5M+, DigiKhata,
  CreditBook). "tuition hisaab" returns **zero** tuition apps. Low demand and
  the wrong neighbours, so Roman Urdu is used only as a natural phrase in the
  full description, never in the title or short description.

## 2. Keyword map

Demand = Pakistan autocomplete. Competition = what the top 10–12 results
looked like on 29 Sep 2026.

| Term (and close variants) | Intent | Demand (PK autocomplete) | Competition observed | Placed in |
|---|---|---|---|---|
| **tuition fee / tuition fees** (…record, …tracker, …collection app, …management app) | teacher wants to track fees | Yes, many variants | **Medium.** All 12 results relevant, but only 3 are strong (Fees Management 50K+ / 1.8K ratings, Tuition App 100K+, Fee Management 10K+); the rest have 50–5K installs and almost no ratings | Short desc ("Home tuition fee register"); full desc ("tuition" ×4, "tuition fees" ×1); Urdu title, short and full ("ٹیوشن" ×6) |
| **fee register / fees register** (students fees register, fees and attendance register) | exact match for what Inkpot is | Yes | **Low.** Only 2–3 relevant apps, then attendance registers and unrelated apps (WhatsApp, Instagram at #5–8). **None of the top 8 competitors uses the word "register"** | Short desc; full desc ×2 (+ "attendance register"); Urdu short and full "فیس رجسٹر" |
| **fees record / fee record (book)** (student fees record app, monthly fees record) | same | Yes | **Low–medium.** 3–4 relevant, then bookkeeping and unrelated apps | Full desc ×2; Urdu "فیس کا ریکارڈ" ×2 |
| **home tuition** (home tuition app for teachers) | teachers who teach at home; also tutor-job seekers | Yes | **Mixed intent.** Results are tutor marketplaces (OTOO, UrbanPro, Filo); no fee app owns the term | Short desc; full desc ×3; Urdu "ہوم ٹیوشن" ×3 |
| **teacher diary** (teacher diary app, teacher digital diary) | teacher's record book | Yes | **Low.** Lesson-planner/diary apps with 100–1K installs; none handles fees | Title ("Teacher's Diary"); full desc; Urdu title "ٹیچر کی ڈائری" |
| **attendance register** (…for tuition, …for student, tuition attendance app) | take attendance | Yes | **High** for the generic term (Attendance Register by Rudra Nirvan 1M+, 9K ratings); **low** for "tuition attendance" | Short desc ("attendance"); full desc ×4 incl. "attendance register"; Urdu "حاضری" ×5 |
| **fee reminder** (fee reminder app, fees reminder app) | remind parents | Yes | **Medium.** 2 fee apps, then generic bill reminders | Short desc ("parent reminders"); full desc heading "Fee reminders on WhatsApp", "reminder" ×5 |
| **tuition teacher** (tuition teacher app, tuition app for teachers) | teacher tools | Yes | **Medium, mixed** (tutoring platforms, AI tutors) | Title "Teacher"; full desc "home tuition teachers" ×2 |
| **fee receipt** | send receipt | not checked | **Wrong neighbours:** receipt-maker apps dominate | Full desc heading "Fee receipts", "receipt" ×4 |
| **offline / no internet** (offline fee register) | works without data | not checked | **Low.** Only Fees Management says "offline" | Full desc "works offline", "no internet needed"; screenshot 5; website |
| part payment, advance, mid-month joining | the monthly-cycle problem | not checked | Nobody describes these clearly | Full desc; screenshots 2 and 3 |
| WhatsApp reminder, Roman Urdu | how parents are contacted | not checked | Only TuitionPilot (India) leads with WhatsApp (two others mention it once); nobody mentions Roman Urdu | **Full desc only** (brand names are not allowed in the title or short description) |
| fees ka hisaab, hisaab, khata | Roman Urdu money words | None relevant | Owned by shop ledgers (5M+ installs), wrong intent | "fees ka hisaab" once, as a natural phrase, in the full desc |
| tuition manager / tuition management app | institute software | Yes | **High.** 12+ apps with it in the title | **Not targeted.** Inkpot is a teacher's register, not institute software; chasing it would be both hard and a worse match |
| Urdu script: ٹیوشن، فیس، رجسٹر، حاضری | Urdu-language phones | Unknown, probably low | **None.** No Urdu listing exists for this use | `listing-ur.md` |

**Top 10 target keywords:** tuition fee(s) · fee(s) register · home tuition ·
teacher diary · attendance (register) · fees record · fee reminder ·
tuition teacher · fee receipt · offline.

### Density in the final English text (script count)

Full description: 3,125 characters, 569 words. "fee/fees" 13, "parent" 9,
"tuition" 4, "home tuition" 3, "fee register" 2, "fees record" 2, "tuition fees" 1,
"attendance" 4, "reminder" 5, "receipt" 4, "WhatsApp" 5, "Roman Urdu" 3,
"batch" 4, "teacher" 3, "diary" 1, "Pakistan" 1. Each core term appears 2–5
times, spread across different sections; no lists of keywords, no repeated
phrases. "Fee" is higher because it is the subject of the app.

## 3. Competitors (Pakistan store, 29 Sep 2026)

| App | Short description (as published) | Category | Installs | Ratings |
|---|---|---|---|---|
| Fees Management – Student App (Generation Next) | Manage student fees, batches, reminders & attendance — Offline & cloud backup! | Finance | 50K+ | 4.5 (1,803) |
| My Tuition Fees (Code 13 Studios) | My Tuition Fees — Manage students, batches, payments, and reports. | Productivity | 1K+ | — |
| Fee Management (iD SYSTEM) | Fee Management is a comprehensive app designed for schools and coaching centers. | Education | 10K+ | 4.6 (124) |
| Tuition App – Coaching Manager (Epic Innovations) | Tuition App is specifically designed for management of classes with mobile app | Education | 100K+ | 4.0 (1,480) |
| Tuition Fee Tracker (Jamshaid & Usman) | Manage tuition fees, students, payments, and reports easily. | Productivity | 50+ | — |
| Tuition Manager – Fee Tracker (Rayvila) | Simple tuition fee tracker to manage student payments and records | Tools | 1K+ | — |
| School Coaching Management App (Dhvanil) | Class & School Fee, Attendance, Student, ID Card, Report, Staff, Management App | Education | 100K+ | 4.5 (976) |
| Tuition Class & School Manager (Dhvanil) | Tuition, coaching & school management: fees, attendance, exams & live classes | Productivity | 50K+ | 4.5 (849) |
| TCMS – Tuition Manager (InvoTech) | TUITION CLASS MANAGEMENT SYSTEM - TCMS | Education | 100K+ | 3.9 (907) |
| Tuition Manager (Kamalvasini) | Manage students, attendance, fees, and expenses for your coaching | Business | 1K+ | — |
| TuitionPilot | Fees, attendance & parent updates on WhatsApp — made for tuition teachers. | Education | 100+ | — |
| Student Fee Manager & Reminder (Monova) | Track school fees, payments & due with reminders, reports and expense history. | Finance | 100+ | — |
| Teacher Diary Lesson Planner (Owesi) | Store lesson plans, diary, degrees & service records — encrypted. | Productivity | 100+ | — |
| TeacherDiary – Tutor Organizer | Teacher planner for tutors: schedule lessons, track students, and earnings. | Education | 1K+ | — |

"—" = too few ratings for Play to show a score.

**Patterns**

- Almost everyone targets **institutes and coaching centres**, often
  India-first (UPI QR codes, CBSE, Hindi "hajri"). Nobody speaks to a teacher
  running a few batches at home.
- **Nobody mentions Pakistan, Urdu or Roman Urdu.** The one app that looks
  Pakistani (Tuition Fee Tracker, 50+ installs) doesn't either.
- **Nobody uses "register"**, the word teachers use for their notebook, which
  Pakistan autocompletes ("fees register", "fees and attendance register").
- Only one leader puts **offline** in its short description; none says
  "no account" or "no internet permission".
- Copy is generic ("ultimate solution", "comprehensive"), often with emoji
  bullets. Plain, specific copy (part payments, mid-month joining, Roman Urdu
  messages) is Inkpot's clearest conversion advantage.
- Outside the top 3–5, **most competitors have no visible rating**. A
  new app with a few dozen genuine ratings already looks credible next to most
  of the field.

**Gaps Inkpot fills:** "fee register" wording, home tuition teachers, Pakistan
and Roman Urdu, offline and private, the monthly cycle explained (part and
advance payments, mid-month joining), and the only Urdu listing in the niche.

## 4. Decisions and rationale

### Title — `Inkpot: Tuition Teacher Diary` (29)

Chosen by the owner on 29 Sep 2026, before launch, over the first choice
`Inkpot: Teacher's Diary` (23). It keeps "teacher diary", which has low
competition, and adds **"tuition"**, the
head term in nearly every relevant Pakistan query ("tuition fee", "tuition app
for teachers", "tuition teacher app", "attendance register for tuition").
It is still not a fees-only name. The title is the most heavily weighted field,
so this was the single biggest text change available.
("Inkpot: Tuition Fee Register" was rejected: stronger for fee searches, but it
boxes the brand into fees.) Don't change the title again after launch unless
the search-terms report gives a clear reason.

### Short description (78/80)

`Home tuition fee register, attendance and parent reminders. Know who has paid.`

It puts the phrases "home tuition", "tuition fee", "fee register",
"attendance" and "reminders" in the second most important field, and ends with
the brand promise. It has no brand names, no "best/free/#1", no emoji and no
caps. WhatsApp is left out on purpose: another company's brand does not belong
in the short description.

### Full description (3,125/4000)

The main keywords are in the first sentence. There are scannable sections, one
real Roman Urdu reminder quoted word for word from the app
(`util/Whatsapp.kt`), the privacy section kept, and a line saying the app is
in English. Every claim was checked against `README.md` and the source: three
joining choices, oldest bill first, advance used on the next bill, the 36-month
arrows, the receipt shared through the share sheet, the absence note offered
only when a number is saved, notifications off until switched on, and no
INTERNET permission in the manifest. Nothing from the not-built list is
mentioned. The words "free", "best", "top" and "#1" appear nowhere in the
listing; the privacy section states "no ads, no analytics and no tracking" as
facts, not as a promotion.

### Category — **Education** (was Business)

- 16 of the 27 fee/tuition management apps I checked are in **Education**
  (Productivity 3, Business 3, Tools 3, Finance 2). So are all three 100K+
  tuition managers.
- Category does **not** decide search ranking. The top 3 for "tuition fee" in
  Pakistan sit in Productivity, Finance and Education. What category does
  change is **"Similar apps" and browse neighbours**. In Education those are
  tuition managers, which is what you want. In Business they are shop khata
  apps (Easy Khata, DigiKhata), which is not.
- It fits the plan to grow into a broader teaching platform.
- Choosing Education does not by itself bring in the Families policy; the
  target-audience answers do. Keep **18 and over only** exactly as in
  `CHECKLIST.md` Step 5.

**Tags:** Education, Productivity, Business, plus at most two more only if
Play Console offers one that is obviously relevant (Google: "it should be very
clear to a user … why the tag is relevant"). The tag list only exists inside
Play Console and changes over time, so I could not check the current names.
Don't pick Personal finance: it is not personal money.

### Urdu translation (`listing-ur.md`)

- Title `Inkpot: ٹیوشن ٹیچر کی ڈائری` (27): "tuition teacher's diary".
- Short (76): "Home tuition fee register, attendance and parent reminders. Know
  who has paid."

Why: it is the only Urdu listing in the niche, and Urdu-script queries
currently return nothing relevant. The reach is modest, because Play shows it
only to phones set to Urdu. It uses the loanwords teachers actually use
(فیس، بیچ، رسید، بیک اپ، واٹس ایپ). It says the app itself is in English.

### Screenshots (`screenshots-captioned/`)

- Real app screens only: no device frames, no added UI, no ranking claims.
- 1080 × 1920 (9:16), 24-bit PNG with no alpha, as Google recommends for
  eligibility.
- The caption band is 340 px (17.7 %), under Google's 20 % tagline limit.
- Headlines are 5 words or fewer, and each states what that very screen
  shows. The owner asked for the moments that grab attention, so they come
  right after the core benefit (the first 2–3 do most of the converting):
  1. **Know who has paid** (November, with October's unpaid half carried
     forward: "Rs 4,000 of it is from earlier months")
  2. **Fee receipts in one tap** (the real receipt PNG the app shares)
  3. **Absent? Tell the parent** (the prompt after marking a student absent)
  4. **Full, part or advance** (payment saved, with the drawn tick)
  5. **Mid-month joining, made clear** (joining choice)
  6. **No signup. No internet needed.** (welcome screen)
- All six come from the real app on an emulator, with the device date moved
  to 4 Nov 2026 so a real carry-forward exists. No WhatsApp screen is shown:
  it is another company's app.

**Promo video:** none for launch (the owner's decision).

### Website (`docs/`)

- Added a keyword-led `<title>` (59 characters) and meta description, a
  canonical URL, Open Graph and Twitter card tags, and JSON-LD
  `SoftwareApplication` (Inkpot, Android, EducationalApplication, price 0 PKR,
  author Downforce Labs).
- The privacy page got the same in its `<head>`; its body is unchanged. Added
  `robots.txt` and `sitemap.xml`.
- `og.png` (1200 × 630) uses the new inkpot logo; link previews on WhatsApp
  and Facebook show it.
- No `aggregateRating` in the JSON-LD. Never add one until there are real
  ratings, and even then only ones shown on the page itself. So expect no star
  snippet in Google results.
- `robots.txt` caveat: crawlers only read `robots.txt` at the root of a host
  (`s-h-a-m-i-r.github.io/robots.txt`), not at `/tutorlink/robots.txt`. The
  file does no harm, but submit the sitemap in Search Console instead (see §7).

## 5. What actually moves Play ranking

In rough order of weight for a new, small app. Google confirms some of these
and not others; where it doesn't, the list says so.

1. **Relevance of the text.** The title counts most, then the short
   description, then the full description. This is done. Keyword stuffing
   doesn't help and can get the listing rejected.
2. **Install velocity and conversion.** How many people who see the listing
   install it, especially in the first weeks. Concentrated installs from real
   teachers do more than a slow trickle. Installs from links (WhatsApp, the
   website) count too.
3. **Ratings and reviews.** The average, how many, and how recent. A listing
   with no rating converts worse. Some ASO practitioners believe the words in
   reviews help ranking; Google has not confirmed it.
4. **Retention and uninstalls.** Google says it favours high-quality apps. An
   app that is installed and then removed within days sends the wrong signal.
   For Inkpot, retention depends on the teacher reaching **the 1st of the next
   month**, when the bills appear by themselves.
5. **Android vitals.** Apps above Google's bad-behaviour thresholds
   (user-perceived crash rate 1.09 %, ANR rate 0.47 %) can be shown less and
   get a warning on their listing. The 3,000-event monkey run with 0 crashes
   is a good start; watch the real numbers.
6. **Update cadence.** Regular, meaningful updates (every 4–8 weeks at first)
   keep quality up and give reviewers a reason to update ratings. Google
   doesn't say that freshness alone ranks an app.
7. **Localisation.** The Urdu listing helps with Urdu-language phones only.

## 6. Launch plan (after production is approved)

**Timing.** Aim to go live around the **20th–25th of a month**. Teachers who
install then enter their batches before the 1st and see the app make the
bills on its own within a week. That is the moment that keeps them.

**Day 0 — go live in Pakistan**

1. Production → roll out to Pakistan. Upload `screenshots-captioned/`, set the
   category to Education, and add the Urdu translation.
2. Message all closed-test testers. Testers **cannot leave public reviews on
   test versions**, but a production release with a higher `versionCode`
   reaches them automatically. So ask them to update from Play and, if they
   actually use it, leave an **honest** rating and a sentence about what they
   use it for. Anyone who wants to can also leave the test from the opt-in
   link.
3. Add the Play link to the website (see §7).

**Days 1–14 — the first 30–50 real teachers.** This is the part that matters
most.

- Go through your own network of home tuition teachers, teachers' WhatsApp
  groups and local Facebook groups for tutors (Lahore, Faisalabad, other
  Punjab cities). Post in each group once. Don't spam.
- Sit with 3–5 teachers for 10 minutes and set up their real batches with
  them. A teacher with real students entered keeps the app.
- Ask every teacher, not only happy ones, to rate honestly **after** they have
  used it for a week.

  A message you can send (Roman Urdu):

  > Assalam o Alaikum! Main ne home tuition teachers ke liye fees ka register
  > app banaya hai, naam hai Inkpot. Har mahine khud bill banata hai, aadhi
  > fees yaad rakhta hai, aur parents ko WhatsApp reminder Roman Urdu mein
  > tayyar kar deta hai. Internet ki zaroorat nahi, data aap ke phone pe hi
  > rehta hai. Link: [PLAY LINK]. Agar aik hafta use karne ke baad kaam ka lage
  > ya na lage, Play Store pe apni sachi raaye zaroor dein. Shukriya!

- **Never** offer anything for a rating, swap reviews, or ask people who
  haven't used the app to rate it. Play removes such ratings and can penalise
  the app.
- Reply to every review within 1–2 days, politely and specifically.

**Weeks 2–6**

- Fix the top one or two complaints and ship an update (raise `versionCode`),
  with release notes that name the fix.
- Watch Android vitals weekly.
- Later, consider Play's in-app review prompt, shown after the first month's
  bills. It needs a Play library added to the app, so first check it does not
  conflict with the "no internet permission / no tracking" promises in the
  privacy policy and Data safety form.

## 7. Post-launch checklist

- [ ] **Website: add the Play link** once the app is live. Add a "Get it on
      Google Play" link or badge (follow Google's badge guidelines) in the hero
      of `docs/index.html`. Add `"installUrl"` / `"downloadUrl"` with the Play
      URL to the JSON-LD and bump `lastmod` in `sitemap.xml`. Until then there
      is deliberately no Play link.
- [ ] **Google Search Console:** add a URL-prefix property for
      `https://s-h-a-m-i-r.github.io/tutorlink/`, verify it with the HTML-tag
      or HTML-file method (a file inside `docs/`), and submit `sitemap.xml`.
- [ ] Make sure the website favicon and hero mark match the new inkpot logo
      (the feature graphic and icon were already updated on 29 Sep 2026).
- [ ] **After 2–4 weeks:** open Play Console's store-listing acquisition
      report (Grow users → Store performance; the "search terms" breakdown;
      menu names vary between Console versions). Note which searches bring
      visitors and which ones install.
      - If a searched term converts well but isn't in the short description,
        work it in.
      - If "tuition" searches dominate, reconsider the alternative title (§4).
- [ ] Once there are about 1,000+ listing visitors a month, run **store
      listing experiments** (A/B tests) on the short description or the first
      screenshot, one change at a time. With less traffic the results are
      noise.
- [ ] **Custom store listing for Pakistan:** not needed while Pakistan is the
      only country (the main listing *is* the Pakistan listing). It becomes
      useful if you add other countries: keep the Pakistan-specific copy
      (rupees, Roman Urdu) as the custom listing and write a neutral default.
- [ ] Read reviews for the words teachers use, and use those words in the
      next listing update.
- [ ] Re-check lengths after every edit (script below).

## Length check

Checked 29 Sep 2026. Characters are counted as Play counts them (UTF-16 code
units; all text here is in the Basic Multilingual Plane, so this equals the
number of characters).

| Field | English (`listing.md`) | Urdu (`listing-ur.md`) |
|---|---|---|
| App name (≤30) | 23 | 27 |
| Short description (≤80) | 78 | 76 |
| Full description (≤4000) | 3,125 | 3,091 |
| Release notes (≤500) | 143 | 114 |

To re-check after editing (reads the first fenced block under each `##`
heading; for release notes the count includes the `<en-US>` / `<ur>` tags,
which Play does not count):

```bash
python3 - <<'EOF'
import re
for f in ["listing.md", "listing-ur.md"]:
    s = open(f, encoding="utf-8").read()
    for h, body in re.findall(r'^## ([^\n]+)\n(?:(?!\n## ).)*?```\n(.*?)\n```', s, re.S | re.M):
        print(f, "|", h[:30], "|", len(body.encode("utf-16-le")) // 2)
EOF
```
