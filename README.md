# Finance manager

Final thesis at the university.

Mobile app in Kotlin for finance tracking.

Purpose of app is to monitor Your finances grouped by categories. User will be able to enter their income and expenses and categorize them in their own categories.

The application will sort and display income and expenditures by days, weeks and months.

financial data will be displayed in visual form using graphs and reports.

The user will be able to create payments reminders.

All data will be stored localy on mobile phone, without using internet access.

App will be portected by fingerprint or password login

## Database layout:

#### "Categories" table:

Field "id" (INTEGER, primary key) - unique identifier of the category <br>
Field "name" (TEXT) - name of the category <br>
Field "type" (TEXT) - category type ("income" or "expenditure") <br>


#### "Transactions" table:

Field "id" (INTEGER, primary key) - unique identifier of the transaction<br>
Field "amount" (FLOAT) - transaction amount<br>
Field "category_id" (INTEGER, foreign key) - reference to the category in the table "Categories"<br>
Field "type" (TEXT) - type of transaction ("income" or "expenditure")<br>
Field "note" (TEXT) - note or description of the transaction<br>
Field "date" (TEXT) - date of transaction<br>

#### "Reminders" table:

Field "id" (INTEGER, primary key) - unique identifier of the reminder<br>
Field "name" (TEXT) - name of the reminder/payment<br>
Field "amount" (REAL) - payment amount<br>
Field "period" (TEXT) - repetition period ("ONCE", "MONTHLY", "QUARTERLY", "SEMI_ANNUALLY", "YEARLY")<br>
Field "firstDueDate" (TEXT) - date of the first payment ("yyyy-MM-dd")<br>
Field "paidCount" (INTEGER) - number of payments made; the next due date is counted from the first one<br>
Field "category" (TEXT) - expense category used when the payment is recorded (optional)<br>
Field "note" (TEXT) - description of the payment (optional)<br>

## Features

- Create custom categories
- Graphs reports
- Fingerprint or password protection
- Payment reminders: recurring or one-off, a daily notification the day before, on the day and while
  overdue, and "Pay" records the expense and moves the reminder to its next date


## Tech Stack

**Mobile:** Kotlin 2.4, Jetpack Compose + Material 3, single activity with Navigation Compose

**Architecture:** ViewModel + StateFlow per screen, a repository over Room

**Database:** SQLite via Room 2.8 (KSP), schema exported to `app/schemas`

**Login:** BiometricPrompt – fingerprint, with the device PIN/pattern/password as fallback

**Build:** Gradle 9.8 (wrapper, Kotlin DSL), Android Gradle Plugin 9.4 with built-in Kotlin, version catalog in `gradle/libs.versions.toml`

**Android:** minSdk 28, compileSdk / targetSdk 37 (edge-to-edge)

## Project layout

```
app/src/main/java/com/example/financemanager/
├── MainActivity.kt          single activity, hosts Compose
├── FinanceApp.kt            Application; creates the database and repository
├── data/                    Room entities, DAOs, database (+ migrations), FinanceRepository
├── notifications/           daily WorkManager check and payment notifications
└── ui/
    ├── navigation/          NavHost and type-safe routes
    ├── login/               biometric login screen
    ├── dashboard/           balance, totals and transaction list (swipe to delete)
    ├── transaction/         add income / expense form
    ├── reminders/           payment reminders list and form
    ├── components/          shared composables and animations
    └── theme/               colours, typography, shapes
```

## Building

Requires JDK 17 or newer and the Android SDK (platform 37). Open the project in a recent
Android Studio, or run:

```
./gradlew assembleDebug        # build the app
./gradlew testDebugUnitTest    # unit tests (incl. Room/migration tests on Robolectric)
./gradlew lintDebug            # lint
```


## Authors

- [@NikolaPerica](https://github.com/NikolaPerica)

