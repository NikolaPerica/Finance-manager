# Finance manager

[![CI](https://github.com/NikolaPerica/Finance-manager/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/NikolaPerica/Finance-manager/actions/workflows/ci.yml)

Final thesis at the university: an Android app in Kotlin for keeping track of personal
finances. Income and expenses are entered by hand or from a photo of a receipt, grouped
in your own categories, and shown in statistics, budgets, savings goals and insights.

All data stays on the phone, in a local database, and the app needs no account or
internet connection. It is locked with a fingerprint or the phone's PIN.

The interface is in Croatian; amounts are in euros unless another currency is chosen in the settings.

## Features

**Transactions**
- Income and expenses with amount, date, category and note; edit or delete them, with undo
  for deletions.
- A searchable list of all transactions, grouped by day. Search covers notes, categories
  and amounts, ignoring case and diacritics. Filters cover type, category, this month,
  last month, this year or any date range.
- **Receipt scanning:** take a photo of a receipt or pick one from the gallery. The total,
  date and shop are read on the phone (ML Kit text recognition), and a category is
  suggested from the shop.
- **Quick add:** save what the form holds as a template ("Kava · 2,00 €"), then add it for
  today with one tap on the dashboard, with undo. The most used come first; a long press
  removes one.

**Categories**
- Your own income and expense categories, each with one of eight colours.
- Rename them, and their transactions and reminders follow.
- Merge one category into another, or delete one and leave its transactions without a
  category.

**Overview**
- **Dashboard:** current balance, monthly budget, insights, upcoming payments, savings
  goals and recent transactions.
- **Statistics:** spending or income per category for a month, a year or all time, and a
  column chart over time. Tap a category to see its transactions.
- **Insights:**
  - a month-end balance forecast
  - categories that cost notably more or less than usual at this point of the month
  - budgets at risk
  - unusually large expenses
  - how much was saved last month
- **Spending calendar:** a month day by day, darker the more was spent, with the days nothing
  was spent marked. On top: days in a row without spending and how many this month. Tap a
  day for its transactions.
- **Subscriptions:** monthly costs found in the transactions themselves: the same expense (same
  note, or same category without one), about the same amount, at least three months in a
  row. They're listed with their monthly and yearly total, and each can become a payment
  reminder with one tap.
- **Weekly summary:** on Sunday evening a notification sums up the week: what was spent, on
  which category most, and how that compares with the four weeks before. It can be turned
  off in the settings.

**Planning**
- **Monthly budgets** per expense category, with a "close to the limit" (80 %) and an
  "over budget" warning, and a notification when an expense crosses either.
- **Safe to spend today:** on the dashboard, what is left of the budgets shared out over
  the days left in the month, minus what was already spent today. Reminder payments still
  due this month are set aside first.
- **Savings goals** with an optional deadline, deposits and withdrawals, and how much to
  put aside each month to make it.
- **Payment reminders**, one-off or recurring (monthly to yearly). A notification comes the
  day before, on the day, and while overdue. "Plati" records the expense and moves the
  reminder to its next date.

**Data**
- **Export:** a PDF report (A4, for sharing or printing) and a CSV table that opens in Excel
  with Croatian settings, for a month, a year or all time.
- **Backup and restore:** all data (templates included) and the settings in one JSON file,
  which can be saved to Google Drive. The file is checked before anything is replaced.

**Security and convenience**
- Fingerprint or device PIN to open the app, and again after two minutes in the
  background (adjustable). "Zaključaj" in the menu locks it at once.
- Content is hidden in the recent apps overview.
- **Home screen widget:** shows the balance and this month's totals, with buttons to add
  income or an expense.

**Settings**
- Theme: the system's, light or dark.
- Currency: the sign shown with amounts (€, $, £, CHF, KM and others). Amounts are not converted.
- The day months start on (1st–28th), e.g. payday. Budgets, statistics, insights, filters,
  exports and the widget all count months from it; each month is named after the calendar
  month it starts in.
- The time of the daily payment reminder.
- How long the app may stay in the background before it locks again.
- Hiding the amounts on the home screen widget, which anyone holding the phone can see.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose (BOM 2026.09) with Material 3, a single activity with type-safe Navigation Compose |
| Architecture | MVVM: one ViewModel with a `StateFlow` per screen, over a repository; four Gradle modules (see below) |
| Dependency injection | Hilt 2.60 (KSP); screen arguments reach ViewModels through assisted injection |
| Settings | Jetpack DataStore (Preferences) |
| Database | SQLite through Room 2.8 (KSP), schema exported to `core/data/schemas`, migrations from version 1 |
| Login | Android's BiometricPrompt (fingerprint, falling back to PIN/pattern/password) |
| Notifications | `AlarmManager` daily check, re-armed after reboot |
| Widget | AppWidget with RemoteViews, redrawn directly whenever data or settings change |
| Receipts | ML Kit text recognition through Google Play services |
| Export | Android `PdfDocument`, kotlinx.serialization (JSON backup) |
| Build | Gradle 9.8 (Kotlin DSL, version catalog), Android Gradle Plugin 9.4 |
| Android | minSdk 28 (Android 9), compileSdk / targetSdk 37 |
| Tests | JUnit 4, Robolectric 4.17, Compose UI tests, widget layout tests |

## Project layout

The project is split into four Gradle modules. Each one may only use the modules below it,
so for example a screen can't reach the database directly and the calculations can't
touch Android.

```
:app            screens, ViewModels, Hilt wiring, widget, notifications
 ├─ :core:data      Room database, migrations, FinanceRepository, JSON backup   (Android library)
 ├─ :core:domain    statistics, budgets, goals, insights, filters, receipts, CSV  (plain Kotlin)
 └─ :core:model     entities: Transaction, Category, Reminder, SavingsGoal…      (plain Kotlin)
```

```
core/model/   …/model/          entities with their Room annotations
core/domain/  …/domain/         Format.kt (money, dates, amount input)
              ├── budgets/      budget levels and alerts
              ├── goals/        goal progress
              ├── insights/     insights and month-end forecast
              ├── stats/        statistics periods and totals
              ├── transactions/ search and filters
              ├── reminders/    due status
              ├── BudgetMonths  months that start on a chosen day
              ├── receipt/      reading totals, dates and shops from receipt text
              ├── export/       CSV and report data
              └── widget/       widget summary
core/data/    …/data/           database, DAOs, migrations, FinanceRepository, settings (DataStore)
              └── backup/       JSON backup format
              schemas/          exported Room schemas (versions 1–6)

app/          …/financemanager/
              ├── MainActivity.kt   single activity; app lock, widget shortcuts
              ├── FinanceApp.kt     Application: notification channels, reminder alarm, widget sync
              ├── QuickAction.kt    shortcuts into the app (widget buttons)
              ├── di/               Hilt module: database, repository, app lock
              ├── export/           PDF drawing
              ├── notifications/    payment reminders and budget alerts
              ├── receipt/          text recognition with ML Kit
              ├── security/         app lock timing
              ├── widget/           home screen widget
              └── ui/               navigation, login, dashboard, forms, lists, statistics,
                                    insights, budgets, goals, reminders, export, settings,
                                    components, theme
```

## Database

Room database `app_database`, version 6. Amounts are euros (`REAL`), dates are ISO text
(`yyyy-MM-dd`), and transaction types are `INCOME` or `EXPENSE`.

**categories**

| Column | Type | |
|---|---|---|
| id | INTEGER, primary key | |
| name | TEXT | unique per type (checked by the app) |
| type | TEXT | `INCOME` / `EXPENSE` |
| monthlyBudget | REAL, nullable | monthly limit for an expense category |
| color | INTEGER, nullable | palette colour (0–7); derived from the id when empty |

**transactions**

| Column | Type | |
|---|---|---|
| id | INTEGER, primary key | |
| amount | REAL | always positive; `type` gives the direction |
| date | TEXT | |
| note | TEXT | |
| category | TEXT | category name, empty for none |
| type | TEXT | `INCOME` / `EXPENSE` |

**reminders**

| Column | Type | |
|---|---|---|
| id | INTEGER, primary key | |
| name | TEXT | |
| amount | REAL | |
| period | TEXT | `ONCE`, `MONTHLY`, `QUARTERLY`, `SEMI_ANNUALLY`, `YEARLY` |
| firstDueDate | TEXT | later due dates are counted from it |
| paidCount | INTEGER | payments made so far |
| category | TEXT | expense category for the payment, may be empty |
| note | TEXT | |

**goals**

| Column | Type | |
|---|---|---|
| id | INTEGER, primary key | |
| name | TEXT | |
| target | REAL | amount to save |
| deadline | TEXT, nullable | |
| color | INTEGER, nullable | palette colour |

**goal_contributions**

| Column | Type | |
|---|---|---|
| id | INTEGER, primary key | |
| goalId | INTEGER | foreign key to `goals`, deleted with the goal |
| amount | REAL | positive for a deposit, negative for a withdrawal |
| date | TEXT | |

**templates**

| Column | Type | |
|---|---|---|
| id | INTEGER, primary key | |
| name | TEXT | what the quick-add button says |
| amount | REAL | |
| type | TEXT | `INCOME` / `EXPENSE` |
| category | TEXT | category name, "" for none; follows renames like transactions |
| note | TEXT | |
| uses | INTEGER | how often it was used; the most used come first |

Migrations, each tested on a real database built from the exported schema:
- 1 → 2: reminders
- 2 → 3: budgets
- 3 → 4: category colours
- 4 → 5: savings goals
- 5 → 6: quick-add templates

## Building and testing

Requires JDK 17 or newer and the Android SDK with platform 37. Open the project in a
recent Android Studio, or run:

```
./gradlew assembleDebug                          # build the app
./gradlew testDebugUnitTest :core:domain:test    # all tests
./gradlew lintDebug                              # lint
```

Everything runs on the JVM, so no emulator is needed. The tests sit next to what they
test: the calculations in `:core:domain` on the plain JVM, the Room and migration tests in
`:core:data` on Robolectric, and the ViewModel tests and the Compose UI tests of the main
flows, which run through the real activity and database, in `:app`.

### Continuous integration

GitHub Actions (`.github/workflows/ci.yml`) runs on every push and pull request:
1. tests
2. lint
3. debug and release builds

Test and lint reports and the debug APK are kept with each run for 90 days.

## Authors

- [@NikolaPerica](https://github.com/NikolaPerica)
