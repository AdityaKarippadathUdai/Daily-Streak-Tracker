# HabitFlow — Persistent Habit & Streak Tracker

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Build System](https://img.shields.io/badge/Build%20System-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![UI Toolkit](https://img.shields.io/badge/UI%20Toolkit-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

**HabitFlow** is a native, high-performance, and offline-first personal growth application built for Android using **Kotlin**, **Jetpack Compose**, and **Material Design 3 (M3)**. Engineered with a premium **Dark Cosmic visual theme**, the app guides users towards building positive long-term habits using responsive analytics, gamified streak tracking, and interactive visualizations.

---

## 📱 Architecture & Native State Engine (The LocalStorage Equivalent)

In a web application, transient state resides in browser-level `localStorage` or session files. In a production-grade native mobile environment, such APIs are replaced with modern Android storage paradigms that guarantee high durability, process-death survivability, and zero latency.

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│           (HomeScreen, AnalyticsScreen, etc.)          │
└───────────▲────────────────────────────────▲───────────┘
            │                                │ CollectStateWithLifecycle
            │ Flow<List<T>>                  │
┌───────────┴────────────────────────────────┴───────────┐
│                    ChallengeViewModel                  │
│       Exposes unified StateFlow from Room Flows        │
└───────────▲────────────────────────────────────────────┘
            │
┌───────────┴────────────────────────────────────────────┐
│                    ChallengeRepository                 │
│         Queries local SQLite schemas via Room DAOs     │
└────────────┬───────────────┬───────────────────────────┘
             │               │
┌────────────▼──────┐ ┌──────▼─────────────┐ ┌───────────▼───────┐
│   ChallengeTable  │ │ CompletionRecord   │ │     TaskTable     │
│ (Active/Archived) │ │ (yyyy-MM-dd logs)  │ │ (Subtask checklist)│
└───────────────────┘ └────────────────────┘ └───────────────────┘
```

### 1. Robust SQLite Persistence via Room ORM
All user-entered variables, checklist states, subtasks, custom habits, and decades of log lists are fully persisted directly inside a relational SQLite database.
* **Non-Blocking Async Flows**: Data interfaces return reactive Kotlin `Flow<List<T>>` objects. Database triggers automatically re-route stream events to update the UI on the spot.
* **Entities**:
  * `Challenge`: Tracks core habits, templates, recurring periodic schedules, and categories (e.g., Health, Fitness, Mind).
  * `CompletionRecord`: Tracks instances of daily clears, linking unique challenge IDs to a specific calendar date query `yyyy-MM-dd`.
  * `Task`: Lightweight checkable sub-elements associated with specific habits to support multi-step micro-routines.

### 2. High-Speed App State Preferences (SharedPreferences)
User theme variables—such as preferring **Light Mode**, **Pure Midnight Black**, or falling back to the **System Default**—are written using native key-value Android `SharedPreferences`. This keeps styling details immediately ready before the first Compose rendering cycle, completely mitigating screen-flicker issues.

### 3. State Generation and ViewModel Pipeline
The architecture follows strict **MVVM (Model-View-ViewModel)** guidelines. `ChallengeViewModel` translates SQLite database repositories into readable states:
* Combines active listings and completions via Kotlin `combine` operators.
* Formulates a live memory footprint using `.stateIn(scope, SharingStarted.WhileSubscribed(5000), default)` ensuring zero thread-blocking and zero overhead when apps run in the background.

---

## 📈 Advanced Streak & Milestone Engine

Calculating exact streaks based on raw calendar listings requires robust historical mapping. HabitFlow handles this locally on-device inside a specialized mathematical helper (`StatsEngine.kt`):

* **Active Days Streak**: Analyzes historical `CompletionRecord` entries. Ensures that if at least one habit of any category is finished on a calendar date, the general active streak of consistency is incremented.
* **Perfect Days Streak 👑**: Identifies consecutive days where **all active habits** are checked off. This represents absolute focus and is gamified through unique visual dashboard alerts.
* **Historical High Marks**: Keeps a continuous track of the highest historical active/perfect intervals ever cleared, so users can visual-progress metrics over time.

---

## 📊 Interactive "Recharts"-Style Spline Canvas Chart

To provide developers and users with rich visual-analytics similar to leading web dashboards, HabitFlow features a custom-built interactive **Consistency Analysis spline chart**:

```
Consistency Curve (Mon ➔ Sun)
   ▲  
   │            ╭───●───╮       ●  (Outer Glow: Selected Day Mon) 40%
1  ├───────────╭╯  / \  ╰╮─────/──── [Area Gradient Underlay]
   │          ╭╯  /   \  ╰╮   /
0.5├─────────╭╯  /     \  ╰╮╭/
   │   ●────╭╯  /       \  ╰●
   └─┼──┼──┼──┼──┼──┼──┼──┼──┼──┼──► Days
    (Mon Tue Wed Thu Fri Sat Sun)
```

* **Cubic Bezier Spline Curves**: Replaces step-wise graphs with beautifully curved cubic paths. Utilizing native `Path.cubicTo()` formulas, the Canvas paints a smooth anti-aliased trail across nodes.
* **Responsive Area Gradient**: Mimics premium web visualization engines (like Recharts) by filling the area bounded beneath the spline curve with a dynamic `Brush.verticalGradient` that tapers down to transparent.
* **Interactive Tooltip Display**: Implements gesture-detecting pointer listeners inside `Modifier.pointerInput`. Tapping any vertical column calculates coordinates, triggers visual node pulses, and highlights a floating card summarizing total checks, active ratios, and percentage consistency.
* **Dynamic Grid Layout**: Calculates layout widths via `BoxWithConstraints` to scale correctly across phones, foldables, and wide tablets without pixel-stretching.

---

## 🎨 Visual Identity & Material 3 Specs

HabitFlow adheres strictly to the Material Design 3 guidelines to establish an inviting, modern workspace:

* **Dark Cosmic Visual Palette**: Custom dark components use high-contrast blue (`#60A5FA`), amber (`#FBBF24`), and deep space-slate canvas backdrops (`#0D111F` to `#07090E`).
* **Fluid Spacing System**: Explicit 8dp grid alignments, ensuring consistent padded buffers ranging from 12dp to 24dp for a balanced and breathable experience.
* **Edge-to-Edge Fluidity**: Uses Compose `enableEdgeToEdge()` configurations alongside proper layout inset modifiers to merge with status bars, system navigation handles, and display cutouts.
* **Custom Adaptive Launcher Icon**: The application is configured with structured adaptive icon resources (`res/mipmap-anydpi-v26`), overlaying a brand vector drawable over a premium space-midnight background vector gradient.

---

## 📂 Project Structure

```
/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt               # Entrypoint, Navigation routing structure
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/AppDatabase.kt   # Room Database class & Migration setup
│   │   │   │   │   ├── model/                    # Kotlin Models (Challenge, CompletionRecord)
│   │   │   │   │   └── repository/               # Repository pattern layer
│   │   │   │   ├── notification/                 # Background alarm schedules & receivers
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/                  # Navigational Views (Home, Analytics, Achievements, etc.)
│   │   │   │   │   ├── theme/                    # Theme, Color, Typography, Shapes definitions
│   │   │   │   │   └── viewmodel/                # ChallengeViewModel
│   │   │   │   └── utils/
│   │   │   │       └── ChallengeStats.kt         # StatsEngine & overall streak analytics
│   │   │   └── res/
│   │   │       ├── drawable/                     # Icons, vector backgrounds, custom launcher graphics
│   │   │       └── values/strings.xml            # App name definitions and localization keys
│   │   └── test/java/com/example/                # Unit Tests & JVM Robolectric controllers
└── build.gradle.kts                              # Root project dependencies & metadata
```

---

## 🛠️ Developer Setup & Build Instructions

Follow these simple steps to compile, install, and run HabitFlow locally:

### Prerequisites
* **Android Studio Core / Koala / Ladybug** or newer.
* **JDK 17** installed and configured in your shell path.
* An active Android Emulator or a physical device connected via USB debugging.

### 1. Verification & Compilation
First, verify standard build definitions, compile-time Kotlin generation tasks, and Room schema configurations:
```bash
gradle compileDebugKotlin
```

### 2. Build Debug APK
Generate an installable `.apk` package locally:
```bash
gradle assembleDebug
```
The output file is located at: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Run JUnit & Robolectric Tests
HabitFlow implements extensive Unit testing suites testing core functionality and engine parameters. Run tests with:
```bash
gradle :app:testDebugUnitTest
```

---

## 🧪 Testing Suite Overview

To preserve maximum regression protection, the project houses high-speed local JVM testing structures in `/app/src/test`:

* **ExampleUnitTest.kt**: Validates basic calculations, dates formatting constraints, and basic string comparisons.
* **ExampleRobolectricTest.kt**: Simulates full Android context environments (e.g., testing `SharedPreferences` read/whites, activity components, database workflows) inside rapid-firing Java Virtual Machines without emulation overhead.
* **GreetingScreenshotTest.kt**: Configured with Roborazzi to conduct Visual verification audits on compose containers, ensuring layout coordinates are not accidentally shifted.
