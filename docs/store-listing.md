# Store listing (app store optimisation and answer-engine text)

An app has no web page for Google to crawl, so the equivalent of SEO is the store listing: the
words Google Play indexes, and the answers an AI assistant can quote when someone asks for an
app like this one. This file is the single source for that text. Paste it into Play Console
(Grow > Store presence > Main store listing); keep it in step with the app and with the website
(`HOME/seo_content.py` in the website repo).

Rules for this text: every sentence must be true of the app as it ships, no keyword lists inside
the description, and no claims about features that are only on the website.

## Title (30 characters at most)

```
MPA: Tasks, Expenses & Games
```
(28 characters)

## Short description (80 characters at most)

```
Tasks, expenses, reminders, Sudoku, Chess, Ludo and converters. Works offline.
```
(78 characters)

## Full description (4000 characters at most)

```
MPA (Multi Portal Application) puts everyday apps in one place: a task list, an expense tracker, reminders, games and handy tools. It is free, and most of it works without an internet connection.

WHAT YOU CAN DO
• Productivity: keep a to-do list, track spending against a monthly budget with a category breakdown and CSV export, and set reminders that notify you on your phone.
• Games: Sudoku with notes, hints and best times; Chess against the computer at three levels or with a friend; Ludo for 2 to 4 players; Tic Tac Toe; Match Cards in three sizes; Number Guess with levels; and two-player Pong.
• Utilities: a unit converter with eight categories, a binary and decimal converter, and a Decision Maker that picks from your own list.
• Your week: a short recap of tasks done, spending, games played and active days.

PRIVATE BY DEFAULT
Tasks, expenses, reminders and game progress are saved on your device. Signing in is optional. If you sign in with your MPA website account, tasks and expenses can be kept in step with the website. Sign-in tokens are encrypted on your device.

WORKS OFFLINE
Games, converters, tasks, expenses and reminders work without internet. Signing in, syncing and sending a message to the team need a connection.

FREE
MPA is free to use. The same apps are also available at https://mpa.pythonanywhere.com, where you can use them in any browser.

QUESTIONS
Is MPA free? Yes.
Do I need an account? No. Create a free account only if you want to sync with the website.
Where is my data? On your device. The website keeps its own copy only if you sign in.
Does it work without internet? Yes, apart from signing in, syncing and contacting us.
```

## Category and tags

- Category: Productivity (Games and Tools are separate Play categories; pick the one that matches
  what most people open the app for, and revisit after the first month of install data).
- Tags to select in Play Console: To-do list, Expense tracker, Reminders, Sudoku, Chess, Unit converter.

## Graphics checklist

- Icon 512x512 (the launcher icon exported without the mask).
- Feature graphic 1024x500: app name and "Tasks, expenses and games in one place".
- At least 4 phone screenshots in this order, since the first two decide most installs:
  Home, Tasks, Expense Tracker, Sudoku. Add Chess, Unit Conversion and Your week.
- Do not use `static/logo.jpg` until it is confirmed as an approved MPA logo (see README).

## Links between the website and the app

These need facts that are not in this repository, so they are left for the owner:

1. Once the app is published, add the Play Store link to the website footer and the About page,
   and to the `sameAs` list of the website's Organization structured data.
2. To let a tap on a website link open the app (and let Google show the app for those pages), the
   website must serve `/.well-known/assetlinks.json` with the package `com.grk.mpa.app` and the
   SHA-256 fingerprint of the release signing key (Play Console > Setup > App signing), and the
   manifest needs `https` intent filters with `android:autoVerify="true"` for the host
   `mpa.pythonanywhere.com`. Do this only for pages the app can open, and add the matching
   `navDeepLink` entries in `MpaNavHost`.
3. Ask for ratings after a positive moment (a finished game or a completed task), never at launch.
   Ratings and reviews are the largest ranking signal after the title and description.
