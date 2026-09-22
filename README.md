# Msebenzi 🛠️

**Msebenzi** ("work" in isiXhosa) is an Android app that connects households
with local informal workers (cleaners, gardeners, handymen, and similar
services) in a transparent, fair way. This repository contains the Part 2
working prototype: a Kotlin Android app, a REST API with a hosted database,
automated tests, and a CI pipeline via GitHub Actions.

> 📹 **Demo video:** [ADD YOUR YOUTUBE/DRIVE LINK HERE]
>
> 🤖 **AI usage disclosure:** see [`AI_USAGE.md`](./AI_USAGE.md)

---

## 1. What the app does

| Feature | Status in this prototype |
|---|---|
| Register & log in (password encrypted) | ✅ Implemented — bcrypt hashing server-side, JWT session, EncryptedSharedPreferences client-side |
| Change settings (language, logout) | ✅ Implemented — English / Afrikaans / isiXhosa |
| Job feed (browse open jobs) | ✅ Implemented — offline-first, cached in Room |
| Post a job (household) | ✅ Implemented, with input validation |
| View job details & accept a job (worker) | ✅ Implemented, with offline queueing |
| Connects to a hosted REST API + database | ✅ Implemented — Node/Express + PostgreSQL |
| Handles invalid input without crashing | ✅ Implemented across all forms |
| Offline support (Room cache + sync outbox) | ✅ Implemented — see `data/JobRepository.kt`, `data/SyncWorker.kt` |
| **User Defined 1: Trusted ratings and reviews** | ✅ Implemented — rate a completed job 1–5 stars, average shown on profile |
| **User Defined 2: Worker profile and verification** | ✅ Implemented — skills, bio, reputation, admin-approved verification badge |
| **User Defined 3: Targeted job alerts** | ✅ Implemented (in-app, not push) — workers are notified only when a job matches their listed skills |
| In-app chat, push notifications (FCM) | 🕒 Deferred to final PoE (per assignment brief, not all features are required for the prototype) |

### 1.1 How the three "User Defined" features work

**Trusted ratings and reviews** — once a job is marked complete, either
party can rate the other (1–5 stars + optional comment). Ratings can't be
left on an open/accepted job, can't be duplicated, and only the two people
actually on that job can rate each other (`backend/routes/ratings.js`).
The ratee's `ratingAverage`/`ratingCount` update automatically and show
on their Profile screen.

**Worker profile and verification** — every worker has a profile
(`ProfileActivity`) showing their skills, a short bio, star rating, and a
"Verified" badge. A worker can request verification from their own
profile; approving it is a deliberately minimal admin action (this
assignment doesn't call for a full admin UI), done via:

```bash
curl -X PUT https://<your-api-url>/api/admin/users/<worker-id>/verify \
  -H "x-admin-token: <your ADMIN_TOKEN from .env>"
```

Show this curl call in your demo video, then reload the worker's profile
in the app to show the badge appear.

**Targeted job alerts** — an in-app "Alerts" tab (bottom navigation),
backed by `backend/routes/notifications.js`. When a household posts a
job, only workers whose comma-separated `skills` field contains that
job's category get notified (`backend/utils/notify.js`) — not every
worker, which is what makes it *targeted*. Households also get notified
when their job is accepted or completed.

---

## 2. Project structure

```
msebenzi/
├── backend/              # Node.js + Express REST API
│   ├── config/db.js      # Sequelize connection (Postgres in prod, SQLite for local dev)
│   ├── models/           # User, Job
│   ├── routes/           # auth, users, jobs
│   ├── middleware/auth.js
│   ├── tests/            # Jest + Supertest automated tests
│   └── server.js
├── android/              # Kotlin Android Studio project
│   └── app/src/main/java/com/msebenzi/app/
│       ├── data/         # Models, Retrofit, Room, SessionManager, SyncWorker
│       ├── adapters/     # RecyclerView adapter
│       └── *Activity.kt  # Splash, Login, Register, Main (feed), JobDetail, PostJob, Settings
├── .github/workflows/build.yml   # CI: runs backend tests + builds the Android app
└── README.md
```

---

## 3. Running the backend API locally

```bash
cd backend
cp .env.example .env      # then edit .env if needed
npm install
npm run dev                # starts on http://localhost:3000
```

With no `DATABASE_URL` set, the API automatically uses a local SQLite file
(`msebenzi.sqlite`) so you can develop without provisioning Postgres first.

Run the automated tests:

```bash
npm test
```

---

## 4. Hosting the API (so the Android app can reach it from your phone)

The app must talk to a database that is genuinely hosted online — not just
`localhost` — for the demo video. The quickest free option is **Render**:

1. Push this repository to GitHub (see section 6).
2. Create a free [Render](https://render.com) account.
3. **New + → PostgreSQL** → create a free database → copy its **Internal/External
   Connection String**.
4. **New + → Web Service** → connect your GitHub repo → set:
   - **Root directory:** `backend`
   - **Build command:** `npm install`
   - **Start command:** `npm start`
   - **Environment variables:**
     - `DATABASE_URL` = the connection string from step 3
     - `JWT_SECRET` = any long random string
     - `ADMIN_TOKEN` = any long random string (used to approve worker verification — see section 1.1)
5. Deploy. Render will give you a public URL like
   `https://msebenzi-api.onrender.com`.
6. Open `android/app/build.gradle.kts` and set `API_BASE_URL` to that URL
   (must end with a trailing slash `/`).
7. Rebuild the app.

> Any other host (Railway, Fly.io, a free-tier AWS/Azure instance, etc.)
> works the same way — the API just needs `DATABASE_URL`, `JWT_SECRET` and `ADMIN_TOKEN`.

---

## 5. Running the Android app

1. Open the `android/` folder in **Android Studio** (Hedgehog or newer).
   Let it sync Gradle — this generates the Gradle wrapper for you
   automatically the first time.
2. Confirm `API_BASE_URL` in `app/build.gradle.kts` points at your hosted
   API (section 4).
3. Run on a device or emulator with internet access.
4. Register a **household** account and a **worker** account (e.g. in two
   emulator instances, or one after logging out of the other) to see the
   full post → browse → accept flow.

### Testing offline behaviour
Turn on airplane mode after the job feed has loaded once — the app keeps
showing the last cached feed (Room), and any job you try to accept while
offline is queued and automatically synced by `SyncWorker` once you're back
online.

---

## 6. Version control with GitHub

```bash
git init
git add .
git commit -m "Initial commit: Msebenzi Part 2 prototype"
git branch -M main
git remote add origin https://github.com/<your-username>/msebenzi.git
git push -u origin main
```

Commit regularly as you build — the rubric specifically checks for commit
history showing incremental progress, not one giant commit.

### GitHub Actions
`.github/workflows/build.yml` runs automatically on every push to `main`:
1. Installs backend dependencies and runs the Jest/Supertest test suite.
2. Builds the Android app's debug APK and runs its unit tests.

Check the **Actions** tab on GitHub after pushing to see it run. A green
check ✅ next to your commit means both the API and the app build cleanly
outside of your own machine — exactly what this requirement is checking for.

> **Note on the Gradle wrapper:** this repo does not commit the binary
> `gradle-wrapper.jar` file. Android Studio generates it for you the first
> time you open the project locally. CI instead installs Gradle directly
> (see the comment in `build.yml`) so the pipeline works either way.

---

## 7. Design considerations

- **Offline-first**: informal workers often have limited or intermittent
  data. The job feed is backed by a local Room database that's read first,
  refreshed from the network when available, and never leaves the user
  looking at a bare error screen for a connectivity blip.
- **Security**: passwords are hashed with bcrypt before ever touching the
  database; the JWT issued at login is stored in `EncryptedSharedPreferences`
  on-device rather than plain SharedPreferences.
- **Accessibility & inclusivity**: the app ships in English, Afrikaans and
  isiXhosa (`values`, `values-af`, `values-xh`) since the target users are
  South African households and informal workers.
- **Resilience**: every form validates its input client-side before it
  ever reaches the network layer, and every network call is wrapped so a
  timeout or a malformed response shows a friendly message instead of
  crashing the app.

---

## 8. Known limitations of this prototype

- In-app chat and real push notifications (FCM) are designed (see the
  Part 1 document) but not yet implemented — targeted alerts exist as an
  in-app "Alerts" tab instead of a true push notification, and this is
  deferred to the final PoE as the brief allows.
- Worker verification approval is a token-protected API call rather than
  a full admin dashboard — appropriate scope for this prototype, but call
  it out explicitly in your demo video (see section 1.1).
- The free tier of Render spins down after inactivity, so the very first
  request after a while can take ~30–60 seconds to "wake up" the API —
  mention this in your demo video if it happens on camera.
- UI has been polished (rounded cards/fields, colour-coded status chips,
  a consistent palette) but final branding/asset work is still left for
  the PoE submission per the assignment brief.
