# Community Marketplace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add home-service customization, campus forum posting, and idle-item marketplace posting to the Java Android campus app.

**Architecture:** Keep the app native Java and SQLite-based. Add small model/rule classes for testable behavior, extend `CampusTaskRepository` for persistence, and reuse `MainActivity` dynamic card UI for the three new surfaces.

**Tech Stack:** Java, Android SDK Views, SQLiteOpenHelper, JUnit 4, Gradle.

---

### Task 1: Validation Rules

**Files:**
- Create: `app/src/main/java/com/example/campustask/model/CommunityRules.java`
- Test: `app/src/test/java/com/example/campustask/CommunityRulesTest.java`

- [ ] Write tests for custom service, forum post, and marketplace item validation.
- [ ] Run `testDebugUnitTest --tests com.example.campustask.CommunityRulesTest` and confirm it fails because `CommunityRules` does not exist.
- [ ] Implement minimal validation rules.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Data Models And Persistence

**Files:**
- Create: `ForumPost.java`
- Create: `MarketplaceItem.java`
- Modify: `CampusTaskDbHelper.java`
- Modify: `CampusTaskRepository.java`

- [ ] Add SQLite tables `custom_services`, `forum_posts`, and `marketplace_items`.
- [ ] Bump DB version and create new tables during upgrade without dropping existing user data.
- [ ] Add repository create/list methods ordered by latest first.

### Task 3: UI Integration

**Files:**
- Modify: `MainActivity.java`

- [ ] Add forum and marketplace service cards to the home grid.
- [ ] Add an "添加服务" action on the home page.
- [ ] Add forum page with publish dialog and post cards.
- [ ] Add marketplace page with publish dialog and item cards.
- [ ] Keep bottom navigation unchanged and use service cards to enter the new pages.

### Task 4: Verification And Delivery

**Files:**
- Output: `outputs/CampusHub-community-market-debug.apk`
- Output: `outputs/CampusHub-community-market-source.zip`

- [ ] Run `testDebugUnitTest assembleDebug`.
- [ ] Confirm test report has 0 failures and 0 errors.
- [ ] Copy debug APK and source zip to the root `outputs` directory.
