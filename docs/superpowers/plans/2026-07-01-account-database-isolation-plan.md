# Account Database Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make each app account use an isolated local business database while keeping login credentials in a shared auth database.

**Architecture:** Split the current SQLite usage into a shared `campus_auth.db` for accounts and per-user `campus_user_<safe-name>.db` files for courses, tasks, orders, services, forum posts, and marketplace items. `CampusTaskRepository` owns both helpers and switches the business helper after login.

**Tech Stack:** Java, Android SDK Views, SQLiteOpenHelper, JUnit 4, Gradle.

---

### Task 1: Account Database Naming Rule

**Files:**
- Create: `app/src/main/java/com/example/campustask/model/AccountDatabaseRules.java`
- Test: `app/src/test/java/com/example/campustask/AccountDatabaseRulesTest.java`

- [ ] Add tests that prove usernames map to deterministic, safe, distinct database names.
- [ ] Run the focused test and confirm it fails because the class does not exist.
- [ ] Implement the naming rule.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Split Auth And Business Databases

**Files:**
- Create: `app/src/main/java/com/example/campustask/data/AuthDbHelper.java`
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskDbHelper.java`
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskRepository.java`

- [ ] Move `users` table ownership to `AuthDbHelper`.
- [ ] Make `CampusTaskDbHelper` accept a username and open a per-user business database.
- [ ] Keep business tables and seed data in the per-user database.
- [ ] Copy existing users from the legacy `campus_task.db` to `campus_auth.db` when possible.

### Task 3: Login Lifecycle

**Files:**
- Modify: `app/src/main/java/com/example/campustask/MainActivity.java`

- [ ] Call `repository.useAccount(username)` after auto-login and successful login/register.
- [ ] Call `repository.clearAccount()` on logout.
- [ ] Rebuild the shell after switching accounts so the bottom service list reloads from the active account database.

### Task 4: Verification And Delivery

**Files:**
- Output: `outputs/CampusHub-account-isolation-debug.apk`
- Output: `outputs/CampusHub-account-isolation-source.zip`

- [ ] Run `testDebugUnitTest assembleDebug`.
- [ ] Confirm tests have 0 failures and 0 errors.
- [ ] Copy the debug APK and source zip to the root `outputs` directory.
