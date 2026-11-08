# TL BRAIN TEST — Master Architecture & Development Roadmap

**Owner:** PRITAM CHOUDHARY  
**Tagline:** Learn, Think, Play, Improve.

TL BRAIN TEST is a mobile-first educational quiz, brain-training, and lightweight 2D gaming platform for students in Classes 1 to 12 (in Hindi and English), managed through a secure Main Admin Panel and a standalone Question Entry Module on a 100% free-tier Firebase backend.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Why You Must Switch Workspace Before Coding React (Beginner Guide for Android Mobile)**
> When this project was started, the **"Build an Android app"** option was tapped, which locked this specific workspace to **Kotlin (Native Android)** rather than **React + TypeScript (Web + PWA)**. Because you confirmed you want to **switch to a separate React web project first**, please review this complete Stage 1 Master Plan and follow the exact mobile button steps right below.

### Confirmed Decisions (From Your Answers)

- **Confirmed Decision 1 — Platform & Hosting Path**: Build first as a **React + TypeScript Progressive Web App (PWA)** on free hosting so it works in any mobile browser and can later be wrapped into an Android APK. *(Note: If you ever change your mind and click "Proceed" inside this current workspace instead of switching, I can alternatively build this exact 3-module system directly as a Native Android APK).*
- **Confirmed Decision 2 — Invite-Only Authentication**: **Admin-Approved Google Sign-In Only** with zero public signup. Students cannot register themselves; the Admin pre-approves each student's Gmail address in the Admin Panel, and unapproved or expired accounts are blocked immediately.
- **Confirmed Decision 3 — Separate Question Entry Module**: Three strictly isolated modules sharing one Firebase database: **(1) Student User Portal**, **(2) Main Admin Control Panel**, and **(3) Dedicated Question Entry & Management Studio** (an independent workspace strictly focused on creating, editing, reviewing, publishing, and analyzing questions).
- **Confirmed Decision 4 — 100% Free-Tier Real-Time Database**: Use **Firebase Cloud Firestore** with live real-time listeners for everything—student profiles, quizzes, leaderboards, subscriptions, announcements, and private 1-to-1 Student-to-Admin support chat—staying strictly inside Firebase's free Spark tier with zero paid services activated.

### Exact First Steps on Your Android Phone (Which Buttons to Tap)

1. **Save or Copy Your Master Setup**: Keep this plan open for reference, or copy your Stage 2 instructions once you are ready.
2. **Open the AI Studio Menu**: Tap the **top-left menu icon (three horizontal lines)** on your Android phone screen in Google AI Studio.
3. **Start a Web Project**: Tap **"Build"** (or **"New App" / "+"**).
4. **Do NOT Tap "Build an Android app"**: Leave the Android chip unselected so AI Studio creates a **React + TypeScript Web App** workspace by default.
5. **Paste Your Next Command**: In the new Web chat box, type:  
   `Build TL BRAIN TEST (Owner: Pritam Choudhary, Tagline: Learn, Think, Play, Improve) in React + TypeScript using Admin-approved Google Sign-In, Cloud Firestore real-time sync, and 3 separate portals (User Panel, Main Admin Panel, and Separate Question Entry Module). Start Stage 1 & Stage 2 now.`

> [!NOTE]
> **Zero Paid Services Rule**: Every tool in this architecture (Firebase Authentication with Google Sign-In, Cloud Firestore Spark Tier, Gemini Free Tier via server-side environment secret, and PWA Web Hosting) operates on free tiers. No paid billing plan will ever be activated without your explicit approval.

---

## 1. Overview & Core Concept

- **What It Does**: Provides an invite-only learning and brain-training arena where Class 1–12 students practice class-wise subjects and General Knowledge in Hindi and English, play 14 fast 2D cognitive mini-games, earn XP and badges, compete on timed leaderboards, and chat directly with the Administrator for support and manual subscription renewal (Rs. 50 for 3 months via manual UPI or cash).
- **Target Audience / Persona**:
  - **Students (Classes 1 to 12)**: Mobile learners in India studying in Hindi or English who want engaging, game-like daily subject practice and brain training.
  - **Administrator (Pritam Choudhary)**: Manages student Gmail approvals, 3-month subscriptions, Gemini AI question generation, banners, prizes, leaderboards, announcements, and customer support directly from an Android phone.
  - **Question Curators / Admin (Separate Question Module)**: Focused entry operators or the Owner using a clean, dedicated Question Entry Module to author, edit, preview, and publish bilingual questions with timers, explanations, and difficulty stats.
- **Key Value**: Combines curriculum-aligned Class 1–12 testing with high-speed 2D brain games in a neon educational gaming interface—while giving the Owner 100% manual control over student access and subscription revenue with zero payment-gateway fees.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Gatekeeper Sign-In & Subscription Check**:
   - User taps **"Sign in with Google"**.
   - System checks the signed-in Gmail against Firestore:
     - **Owner / Admin Role**: Unlocks portal selector for **Main Admin Panel**, **Separate Question Entry Module**, or **Student Preview**.
     - **Question Editor Role**: Routes directly into the **Separate Question Entry Module**.
     - **Whitelisted Student (Active Subscription)**: Enters the **Student User Panel** matched to their assigned Class (1–12) and preferred language (Hindi / English).
     - **Unregistered / Banned / Expired Account**: Displays a clean Neon Lock Screen showing subscription status, Owner contact instructions (WhatsApp / Support info), and Rs. 50 / 3-month manual activation guidance.
2. **Student Learning & Gaming Loop**:
   - Student lands on the **Dashboard** showing XP bar, current Level, streak, announcements banner, YouTube/WhatsApp quick links, Class subjects, GK hub, and the 14 Brain Games arcade.
   - Completing a timed Quiz or 2D Game calculates accuracy, awards XP, checks badge milestones, and updates Daily/Weekly/Monthly/All-Time leaderboards in real time.
3. **Standalone Question Entry Workflow**:
   - Operator opens the **Question Entry Module**, selects Class (1–12 or GK), Subject, and Language (Hindi / English), enters the question stem, optional HTTPS image URL, 4 options, correct answer, explanation, difficulty, points, and timer seconds, then saves as **Draft** or **Published**.
4. **Admin Operations & AI Generation Workflow**:
   - Admin opens the **Main Admin Panel** on mobile, whitelists a student's Gmail after receiving Rs. 50 UPI/cash, sets the 90-day expiry date (or custom dates), generates draft questions with Gemini AI, reviews and approves drafts into the live bank, and replies to student support messages in real time.

### Visual Identity & Theme

- **Aesthetic Direction**: **Cyber-Academic Neon Gaming** — dark obsidian surfaces, electric purple neon borders, crisp white high-contrast reading cards for long study sessions, and **zero emojis anywhere** (100% geometric vector icons).
- **Color Palette & Mood**:
  - `--background`: `#070510` (Deep Obsidian Black)
  - `--surface`: `#120D24` (Dark Violet-Black Elevated Card)
  - `--surface-bright`: `#1D1538` (Interactive Tile Surface)
  - `--primary`: `#8B5CF6` (Electric Royal Purple)
  - `--accent-glow`: `#C084FC` (Neon Lavender Glow for borders, active tabs, and XP bars)
  - `--foreground`: `#FFFFFF` (Pure Crisp White for primary text and question readability)
  - `--muted-foreground`: `#C4B5FD` (Soft Violet-White for labels and metadata)
  - `--success`: `#10B981` (Correct Answer / Active Subscription)
  - `--warning`: `#F59E0B` (Timer Warning / Expiring Soon)
  - `--danger`: `#EF4444` (Wrong Answer / Banned / Expired)
- **Typography & Hierarchy**:
  - **Display & Gaming Headings**: Geometric futuristic display font (*Orbitron* or *Rajdhani*) for levels, scores, timers, and neon headers.
  - **Body & Bilingual Study Text**: Clean, high-legibility sans-serif (*Plus Jakarta Sans* paired with *Noto Sans Devanagari* for crystal-clear Hindi and English rendering on mobile screens).
- **Component Styling & Layout**:
  - Mobile-first single-column layout (`max-width` adaptive containers on larger screens) with sticky bottom icon navigation in the Student Panel and a collapsible mobile command drawer in the Admin and Question Entry portals.
  - Subtle outer neon glow (`0 0 16px rgba(139, 92, 246, 0.35)`) on active cards, CTA buttons, and leaderboard podiums.
  - **Strict Iconography Rule**: Clean vector icons (Lucide / Material Icons) for every subject, game, badge, and button—never emojis.
- **Interactive Feedback & Motion**:
  - Smooth countdown ring animations on timed questions, instant green/red neon flash feedback on option selection, animated XP level-up progress bars, skeleton shimmer loaders, and clear empty-state illustrations.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Admin-Whitelisted Google Sign-In Instead of Password Management**
  - *Chosen Approach*: Students sign in with their Gmail via Google Sign-In, gated by an Admin-created whitelist record in Firestore (`allowedUsers`).
  - *Why*: Eliminates forgotten-password resets for young students, prevents credential sharing via strict UID binding, works out-of-the-box on Firebase's free tier, and blocks 100% of public signups.
  - *Alternatives Considered*: Manual email/password creation requires Admin password resets and separate credential distribution overhead.
- **Decision 2: Dedicated Separate Question Entry Module**
  - *Chosen Approach*: Isolate question authoring, editing, publishing, and question-level analytics into its own standalone portal route and navigation shell, separate from student management and billing controls in the Main Admin Panel.
  - *Why*: Keeps daily high-volume question entry fast and clutter-free on an Android phone screen, and allows granting `question_manager` permission in the future without exposing student ban or subscription controls.
  - *Alternatives Considered*: Embedding a small form inside the Main Admin Panel makes mobile entry cramped and mixes content authoring with sensitive user administration.
- **Decision 3: Single Cloud Firestore Real-Time Engine**
  - *Chosen Approach*: Store structured data (users, questions, leaderboards, subscriptions) and real-time 1-to-1 support threads in Cloud Firestore using scoped real-time snapshot listeners.
  - *Why*: Keeps all security rules unified in one place, delivers instant live chat updates between Student and Admin, and stays comfortably within Firebase Spark free-tier quotas.
  - *Alternatives Considered*: Running both Firestore and Realtime Database doubles configuration and security-rule maintenance without adding capability.
- **Decision 4: Server-Protected Gemini AI Draft-and-Review Pipeline**
  - *Chosen Approach*: Route Gemini AI question generation through a secure backend/server environment key so private API keys never appear in client code, and save all AI-generated questions strictly with `status: "draft"` until an Admin reviews and clicks Publish.
  - *Why*: Protects API keys from browser inspection and guarantees 100% educational accuracy for Class 1–12 students before any question goes live.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Three-Portal System Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                    TL BRAIN TEST — CLIENT APPLICATION                     │
│             (Mobile-First PWA • Purple/Black/White Neon UI)               │
├───────────────────────┬─────────────────────────┬─────────────────────────┤
│   1. STUDENT PANEL    │  2. MAIN ADMIN PANEL    │  3. QUESTION ENTRY MOD  │
│  (Whitelisted Users)  │  (Owner: P. Choudhary)  │  (Standalone Studio)    │
├───────────────────────┼─────────────────────────┼─────────────────────────┤
│ • Dashboard & Banners │ • Student Whitelist/Ban │ • Add / Edit / Delete   │
│ • Class 1-12 Subjects │ • Subscription & Dates  │ • Publish / Unpublish   │
│ • Hindi / English UI  │ • Search & Activity Log │ • Class 1-12 & Subject  │
│ • Timed Quiz & GK     │ • Gemini AI Generator   │ • 4 Options & Correct   │
│ • 14 2D Brain Games   │ • Draft Review Queue    │ • Explanation & Timer   │
│ • XP, Levels & Badges │ • Banners, Links, Gifts │ • Difficulty & Points   │
│ • 4x Leaderboards     │ • Leaderboard Controls  │ • HTTPS Image Preview   │
│ • Subscription Status │ • Monthly Reset/Winners │ • Per-Question Stats    │
│ • 1-to-1 Admin Chat   │ • Support Chat Inbox    │   (Attempts, Accuracy)  │
│ • Announcements       │ • Alerts & Analytics    │                         │
└───────────┬───────────┴────────────┬────────────┴────────────┬────────────┘
            │                        │                         │
            ▼                        ▼                         ▼
┌───────────────────────────────────────────────────────────────────────────┐
│               ROLE & SUBSCRIPTION SECURITY GATEKEEPER                     │
│    Google Sign-In Auth -> Verify Email Whitelist -> Check Ban & Expiry    │
└───────────────────────┬───────────────────────────────────┬───────────────┘
                        │                                   │
                        ▼                                   ▼
┌─────────────────────────────────────────┐ ┌───────────────────────────────┐
│     FIREBASE CLOUD FIRESTORE (FREE)     │ │  SECURE GEMINI AI GENERATOR   │
│  Zero-Trust Security Rules Enforcement  │ │  (Server-Protected API Call)  │
├─────────────────────────────────────────┤ ├───────────────────────────────┤
│ • users & subscriptions                 │ │ • Prompt: Class + Subject +   │
│ • questions & questionStats             │ │   Language + Difficulty       │
│ • quizAttempts & gameScores             │ │ • Structured JSON Schema      │
│ • leaderboards & monthlyWinners         │ │ • Writes ONLY to Draft Queue  │
│ • supportThreads/{studentUid}/messages  │ │ • Mandatory Admin Approval    │
│ • appConfig, banners, prizes, alerts    │ │   Before Publishing           │
└─────────────────────────────────────────┘ └───────────────────────────────┘
```

### Data Model & Zero-Trust Security Strategy

- **`users/{uid}` & `allowedEmails/{email}`**: Stores student profile (name, approved Gmail, Class 1–12, preferred language Hindi/English, role `student` | `question_manager` | `admin`, `isBanned`, XP, level, accuracy totals, badges, and subscription start/expiry timestamps). Security rules allow students to read their own profile and update non-privileged preferences only; only `admin` can create/whitelist accounts, ban users, or modify subscription dates.
- **`questions/{questionId}`**: Stores Class (1–12 or `GK`), subject, language (`hi` | `en`), question text, optional HTTPS image URL, 4 options, correct option index, explanation, difficulty (`easy` | `medium` | `hard`), points, timer seconds, `isPublished` boolean, `source` (`manual` | `gemini_ai`), and usage statistics (`attemptsCount`, `correctCount`). Students can read **only** `isPublished == true` questions when their subscription is active and `isBanned == false`. Only `admin` and `question_manager` roles can write.
- **`quizResults/{resultId}` & `leaderboards/{period}_{id}`**: Records quiz/game scores, accuracy, XP earned, and period keys (`daily`, `weekly`, `monthly`, `allTime`), plus `winnersHistory/{monthId}` when monthly leaderboards reset.
- **`supportThreads/{studentUid}/messages/{msgId}`**: Strict 1-to-1 private collection. Security rules enforce `request.auth.uid == studentUid || isAdmin()`—making student-to-student messaging impossible at the database level.
- **`appSettings/global`, `banners`, `prizes`, `announcements`**: Controls Leaderboard ON/OFF toggle, YouTube/WhatsApp redirect URLs, HTTPS promotional banners, prize showcase cards, and app update notifications.

### Interactive Component & State Mapping

- **Auth & Subscription Guard**: Listens to Firebase Auth + live `users/{uid}` snapshot; instantly transitions the screen to a locked renewal screen if an Admin bans a user or if the current date passes `subscriptionEndDate`.
- **Quiz & Timer Engine**: Loads filtered published questions by Class, Subject, and Language; runs a smooth per-question countdown timer; locks selection on tap or timeout; displays explanation card; and commits XP, accuracy, and question statistics atomically.
- **14 Lightweight 2D Games Engine**: Pure HTML5 Canvas / reactive 2D state loops running at 60fps on mobile touchscreens with zero heavy 3D libraries—covering arithmetic drills, memory grid flips, millisecond reaction taps, word/number scrambles, riddles, multiplication table sprints, color stroop recognition, and 10-second finger-tap speed tests.

---

## 5. Complete 8-Stage Development Sequence & Master Feature Checklist

We will build TL BRAIN TEST strictly stage-by-stage without skipping steps or breaking working features:

### Stage-by-Stage Roadmap

- **STAGE 1 (Current Stage — Master Setup & Roadmap)**: Complete technical architecture, security design, full feature checklist, and workspace setup guidance. **[COMPLETED IN THIS PLAN]**
- **STAGE 2 (Complete User Panel & Frontend Shell)**: Setup Purple/Black/White neon theme, vector icon system (zero emojis), Hindi/English language switcher, Student Dashboard, Class 1–12 & Subject selector, Profile, Achievements UI, Announcements view, and responsive navigation.
- **STAGE 3 (Firebase Configuration & Invite-Only Authentication)**: Connect Firebase project, enable Google Sign-In, deploy Zero-Trust Firestore Security Rules, wire Admin Gmail bootstrap (`PRITAM CHOUDHARY`), and enforce the no-public-signup whitelist + ban/expiry gatekeeper.
- **STAGE 4 (Main Admin Panel)**: Build mobile-friendly Admin Control Center—create/whitelist & edit students, ban/unban, manual Rs. 50 / 3-month & custom date subscription controls, daily student activity log, user search/filter, HTTPS banner manager, YouTube/WhatsApp link manager, prize/gift manager, notifications, app update announcements, and analytics dashboard.
- **STAGE 5 (Separate Manual Question Management Panel)**: Build the standalone Question Entry Studio—add, edit, delete, publish/unpublish questions, Class 1–12 + GK & Subject selectors, 4 options, correct answer, explanation, difficulty, points, timer, HTTPS image preview, and per-question accuracy/attempt statistics.
- **STAGE 6 (Gemini AI Question Generator, Quiz Engine & All 14 2D Games)**: Wire secure server-side Gemini API question generation into the Admin Draft Review Queue; connect live timed Quiz & GK engine; build all 14 lightweight 2D educational games with XP & level progression.
- **STAGE 7 (Subscriptions, Leaderboards, Rewards & Live Customer Support)**: Activate Daily/Weekly/Monthly/All-Time leaderboards, Admin Leaderboard ON/OFF switch, manual + automatic monthly reset with Previous Winners History, Prize showcase, and real-time 1-to-1 Student-to-Admin Firestore chat.
- **STAGE 8 (Security Audit, Bug Fixing, PWA & Free Website Publishing)**: Verify Firestore rules against unauthorized reads/writes, test mobile responsiveness and offline PWA installability ("Add to Home Screen"), and guide you button-by-button to publish live on free hosting (and optional APK packaging).

---

### Master Feature Checklist (Completed vs Pending)

#### Project Planning & Architecture
- [x] Full project analysis and beginner-friendly mobile workflow
- [x] Complete 3-panel technical architecture & Firestore schema design
- [x] 8-stage step-by-step development roadmap

#### Authentication & Core Design
- [ ] Purple, black, and white neon educational gaming UI (zero emojis, 100% vector icons)
- [ ] Bilingual Hindi & English support across User Panel & Quizzes
- [ ] Admin-approved Google Sign-In with zero public signup
- [ ] Role-based routing (Student Panel, Main Admin Panel, Separate Question Entry Module)

#### Panel 1: Student User Panel
- [ ] Student Dashboard (banners, announcements, quick links, daily progress)
- [ ] Student Profile & Profile Settings (Classes 1 to 12, class-wise subjects)
- [ ] Timed Quiz Engine & General Knowledge (GK) section
- [ ] XP, progressive Levels, Accuracy stats, and detailed Result breakdowns
- [ ] Achievements and unlockable Badges
- [ ] Daily, Weekly, Monthly, and All-Time Leaderboards
- [ ] Subscription status & expiry viewer (Rs. 50 / 3 months manual renewal info)
- [ ] Announcements & App Update notices
- [ ] Private 1-to-1 Student-to-Admin Customer Support chat

#### All 14 Lightweight 2D Educational Games
- [ ] 1. Math Challenge
- [ ] 2. Addition
- [ ] 3. Subtraction
- [ ] 4. Multiplication
- [ ] 5. Division
- [ ] 6. Number Pattern
- [ ] 7. Memory Match
- [ ] 8. Reaction Test
- [ ] 9. Number Scramble
- [ ] 10. Word Scramble
- [ ] 11. Riddles
- [ ] 12. Table Test
- [ ] 13. Color Recognition
- [ ] 14. Finger Speed

#### Panel 2: Main Admin Panel
- [ ] Secure Admin access & Analytics Dashboard
- [ ] Create (whitelist Gmail), edit, search, filter, ban, and unban student accounts
- [ ] Subscription management (Rs. 50 / 3-month preset + custom start/expiry dates)
- [ ] Daily student activity tracking
- [ ] Gemini AI Question Generator with mandatory Admin review before publishing
- [ ] HTTPS Banner manager, YouTube & WhatsApp redirect manager, Prizes & Gift images manager
- [ ] Leaderboard ON/OFF toggle, Manual & Automatic monthly reset, Previous Winners history
- [ ] Admin Customer Support real-time inbox, Notifications & App Update announcements

#### Panel 3: Separate Manual Question Entry & Management Module
- [ ] Dedicated standalone Question Entry interface with separate permission guard
- [ ] Add, edit, delete, publish, and unpublish questions
- [ ] Class (1–12 & GK), Subject, Language, 4 options, correct answer, and explanation fields
- [ ] Difficulty level, Points, Timer seconds, HTTPS Image URL preview, and Question statistics

#### Security, PWA & Free Deployment
- [ ] Zero-trust Firebase Firestore Security Rules & hidden server-side Gemini API key
- [ ] Installable mobile PWA setup & free live hosting deployment
