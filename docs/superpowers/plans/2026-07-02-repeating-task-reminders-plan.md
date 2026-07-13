# Repeating Task Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a repeat reminder interval to tasks so Android notifications can reappear every N minutes until the task is completed, deleted, or edited.

**Architecture:** Store `repeat_minutes` in each account's task table and in `TaskItem`. `ReminderScheduler` passes the interval to `ReminderReceiver`; after showing a notification, the receiver schedules the next one when the interval is positive. Existing cancel paths keep stopping future reminders.

**Tech Stack:** Java, Android SDK `AlarmManager`, SQLiteOpenHelper, JUnit 4, Gradle.

---

### Task 1: Rule Tests

**Files:**
- Modify: `app/src/test/java/com/example/campustask/TaskRulesTest.java`
- Modify: `app/src/main/java/com/example/campustask/model/TaskRules.java`

- [ ] Add tests for repeat interval normalization and next reminder time calculation.
- [ ] Run focused tests and confirm they fail because methods are missing.
- [ ] Implement `normalizeRepeatMinutes` and `nextRepeatReminderAt`.
- [ ] Run focused tests and confirm they pass.

### Task 2: Data Model and Persistence

**Files:**
- Modify: `app/src/main/java/com/example/campustask/model/TaskItem.java`
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskDbHelper.java`
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskRepository.java`

- [ ] Add `repeatMinutes` to `TaskItem`.
- [ ] Add `repeat_minutes INTEGER NOT NULL DEFAULT 0` to task table creation and migration.
- [ ] Save and read `repeat_minutes` in the repository.

### Task 3: Notification Rescheduling

**Files:**
- Modify: `app/src/main/java/com/example/campustask/reminder/ReminderScheduler.java`
- Modify: `app/src/main/java/com/example/campustask/reminder/ReminderReceiver.java`

- [ ] Include repeat minutes in scheduled alarm intents.
- [ ] After notification delivery, schedule the next notification when repeat minutes are positive.
- [ ] Keep existing cancel behavior for completed/deleted tasks.

### Task 4: Task UI

**Files:**
- Modify: `app/src/main/java/com/example/campustask/MainActivity.java`

- [ ] Add repeat interval input to the task dialog.
- [ ] Show repeat interval on task cards.
- [ ] Persist the interval when creating/editing tasks.

### Task 5: Verification and Artifacts

**Files:**
- Output: `outputs/CampusHub-repeat-reminders-debug.apk`
- Output: `outputs/CampusHub-repeat-reminders-source.zip`

- [ ] Run `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- [ ] Export the APK and source zip under the requested `outputs` directory.
