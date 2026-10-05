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
Field "amount" (FLOAT) - payment amount<br>
Field "period" (TEXT) - repetition period ("once", "monthly", "quarterly", "semi-annually", "annually")<br>
Field "date" (TEXT) - date of the first payment<br>
Field "description" (TEXT) - description of the payment<br>

## Features

- Create custom categories
- Graphs reports
- Fingerprint or password protection
- Payment reminders


## Tech Stack

**Mobile:** Kotlin 2.2, Android Views + Material 3, ViewBinding

**Database:** SQLite via Room (KSP)

**Build:** Gradle 8.14 (wrapper), Android Gradle Plugin 8.10, version catalog in `gradle/libs.versions.toml`

**Android:** minSdk 28, compileSdk / targetSdk 36 (edge-to-edge)

## Building

Requires JDK 17 or newer and the Android SDK (platform 36). Open the project in a recent
Android Studio, or run:

```
./gradlew assembleDebug
```


## Authors

- [@NikolaPerica](https://github.com/NikolaPerica)

