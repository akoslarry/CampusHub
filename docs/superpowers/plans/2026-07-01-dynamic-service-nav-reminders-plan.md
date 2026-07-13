# Dynamic Service Navigation And Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow deleting custom services, show clearer task reminders, and mirror added home services into the bottom navigation area.

**Architecture:** Keep the current Java View-based single-activity app. Add a pure rule for recognizing custom service ids, add repository delete/read helpers for custom services, and convert the bottom navigation row into a horizontal scroll list that includes built-in and user-added services.

**Tech Stack:** Java, Android SDK Views, SQLiteOpenHelper, JUnit 4, Gradle.

---

### Task 1: Custom Service Delete Rule

**Files:**
- Modify: `app/src/test/java/com/example/campustask/CommunityRulesTest.java`
- Modify: `app/src/main/java/com/example/campustask/model/CommunityRules.java`

- [ ] Add a failing test for `CommunityRules.isCustomServiceId`.
- [ ] Run the focused test and confirm it fails.
- [ ] Implement `isCustomServiceId`.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Repository Service Delete Helpers

**Files:**
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskRepository.java`

- [ ] Add `getCustomService(String id)`.
- [ ] Add `deleteCustomService(String id)` that only deletes ids shaped like `custom_<number>`.

### Task 3: Dynamic Bottom Navigation And Delete UI

**Files:**
- Modify: `app/src/main/java/com/example/campustask/MainActivity.java`

- [ ] Wrap bottom navigation in `HorizontalScrollView`.
- [ ] Add forum, market, and custom services to the bottom navigation.
- [ ] Add a custom service detail page with delete action.
- [ ] Add delete action to custom service cards.
- [ ] Show reminder time explicitly on task cards.

### Task 4: Verification And Delivery

**Files:**
- Output: `outputs/CampusHub-dynamic-nav-reminders-debug.apk`
- Output: `outputs/CampusHub-dynamic-nav-reminders-source.zip`

- [ ] Run `testDebugUnitTest assembleDebug`.
- [ ] Confirm tests have 0 failures and 0 errors.
- [ ] Copy the debug APK and source zip to the root `outputs` directory.
