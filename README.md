# Finance manager

Final thesis at the university.

Mobile app in Kotlin for finance tracking.

Purpose of app is to monitor Your finances grouped by categories. User will be able to enter their income and expenses and categorize them in their own categories.

The application will sort and display income and expenditures by days, weeks and months.

financial data will be displayed in visual form using graphs and reports.

The user will be able to create payments reminders.

All data will be stored localy on mobile phone, without using internet access.

App will be portected by fingerprint or password login

Database layout:
"Categories" table:
Field "id" (INTEGER, primary key) - unique identifier of the category
Field "name" (TEXT) - name of the category
Field "type" (TEXT) - category type ("income" or "expenditure")

"Transactions" table:
Field "id" (INTEGER, primary key) - unique identifier of the transaction
Field "amount" (FLOAT) - transaction amount
Field "category_id" (INTEGER, foreign key) - reference to the category in the table "Categories"
Field "type" (TEXT) - type of transaction ("income" or "expenditure")
Field "note" (TEXT) - note or description of the transaction
Field "date" (TEXT) - date of transaction

Table "Reminders":
Field "id" (INTEGER, primary key) - unique identifier of the reminder
Field "name" (TEXT) - name of the reminder/payment
Field "amount" (FLOAT) - payment amount
Field "period" (TEXT) - repetition period ("once", "monthly", "quarterly", "semi-annually", "annually")
Field "date" (TEXT) - date of the first payment
Field "description" (TEXT) - description of the payment

## Features

- Create custom categories
- Graphs reports
- Fingerprint or password protection
- Payment reminders


## Tech Stack

**Mobile:** Kotlin

**Database:** SQLite


## Authors

- [@NikolaPerica](https://github.com/NikolaPerica)

