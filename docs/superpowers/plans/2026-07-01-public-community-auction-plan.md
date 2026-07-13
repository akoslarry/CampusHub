# Public Community Auction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move forum and marketplace data to a public local community database, then add comments and auction bidding.

**Architecture:** Keep personal data in per-account business databases, but add a shared `campus_community.db` managed by `CommunityDbHelper`. `CampusTaskRepository` will keep account-specific helpers for courses/tasks/orders/services and use the community helper for forum posts, forum comments, marketplace items, marketplace comments, and bids.

**Tech Stack:** Java, Android SDK Views, SQLiteOpenHelper, JUnit 4, Gradle.

---

### Task 1: Auction Rule

**Files:**
- Modify: `app/src/test/java/com/example/campustask/CommunityRulesTest.java`
- Modify: `app/src/main/java/com/example/campustask/model/CommunityRules.java`

- [ ] Add a failing test for `canPlaceBid`.
- [ ] Run the focused test and confirm it fails because the method does not exist.
- [ ] Implement `canPlaceBid`.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Public Community Storage

**Files:**
- Create: `app/src/main/java/com/example/campustask/data/CommunityDbHelper.java`
- Create: `app/src/main/java/com/example/campustask/model/ForumComment.java`
- Create: `app/src/main/java/com/example/campustask/model/MarketplaceComment.java`
- Create: `app/src/main/java/com/example/campustask/model/MarketBid.java`
- Modify: `MarketplaceItem.java`
- Modify: `CampusTaskRepository.java`

- [ ] Add public tables for posts, comments, marketplace items, marketplace comments, and bids.
- [ ] Use `CommunityDbHelper` for all forum/marketplace repository methods.
- [ ] Compute current marketplace price from highest bid or starting price.

### Task 3: UI

**Files:**
- Modify: `MainActivity.java`

- [ ] Show forum comments under each post and add comment action.
- [ ] Show marketplace comments and bids under each item.
- [ ] Add bid action that requires a higher bid than the current price.
- [ ] Keep author names tied to the current logged-in account.

### Task 4: Verification And Delivery

**Files:**
- Output: `outputs/CampusHub-public-community-auction-debug.apk`
- Output: `outputs/CampusHub-public-community-auction-source.zip`

- [ ] Run `testDebugUnitTest assembleDebug`.
- [ ] Confirm tests have 0 failures and 0 errors.
- [ ] Copy the debug APK and source zip to the root `outputs` directory.
