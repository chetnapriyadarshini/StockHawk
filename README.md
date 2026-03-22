# StockHawk — Stock Portfolio Tracker (Android)

An Android stock tracking application that displays real-time stock prices and interactive historical performance charts, with full accessibility support and localisation — built as part of the Udacity Android Developer Nanodegree.

---

## Features

- **Stock Search & Portfolio** — Search for any stock by ticker symbol and add it to a personal watchlist
- **Live Prices** — Fetches current stock prices via the Yahoo Finance REST API
- **Historical Charts** — Interactive line graphs showing stock performance over configurable time intervals (1 week, 1 month, 3 months, 1 year) using MPAndroidChart
- **Accessibility** — Full content descriptions, TalkBack support, and touch target sizing compliant with Android accessibility guidelines
- **Localisation** — String resources and number formatting adapted for multiple locales
- **Home Screen Widget** — Displays current portfolio prices directly on the home screen

---

## Technical Highlights

| Component | Implementation |
|---|---|
| Stock data | Yahoo Finance REST API |
| Chart rendering | MPAndroidChart (third-party library) |
| Local storage | SQLite via ContentProvider + CursorLoader |
| Background sync | SyncAdapter / JobScheduler |
| Accessibility | Content descriptions, TalkBack, RTL support |
| Widget | AppWidgetProvider |

---

## Setup

```bash
git clone https://github.com/chetnapriyadarshini/StockHawk.git
```

Open in Android Studio and build. No API key required for the Yahoo Finance endpoint used.

---

## Context

Forked from the Udacity Android Developer Nanodegree starter project and substantially extended with chart integration, accessibility features, and localisation as required by the project rubric.

---

## Contact

Created by [@chetnapriyadarshini](https://github.com/chetnapriyadarshini)
