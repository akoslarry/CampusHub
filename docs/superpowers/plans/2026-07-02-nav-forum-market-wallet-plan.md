# Navigation, Forum Delete, Marketplace Wallet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep "我的" as the last navigation item, let forum authors delete their own posts, and add marketplace sale settlement with a 2% platform fee and seller wallet balance.

**Architecture:** Add pure rules to `CommunityRules` for ownership, bidding, and fee calculations, then use them from `CampusTaskRepository`. Public community data remains in `campus_community.db`; per-account wallet transactions live in each user's personal business database.

**Tech Stack:** Java, Android SDK, SQLiteOpenHelper, JUnit 4, Gradle.

---

### Task 1: Rules Tests

**Files:**
- Modify: `app/src/test/java/com/example/campustask/CommunityRulesTest.java`
- Modify: `app/src/main/java/com/example/campustask/model/CommunityRules.java`

- [ ] Add tests for owned deletion, self-bid rejection, sale permission, 2% fee, and 98% seller income.
- [ ] Run `gradle testDebugUnitTest` and confirm the new tests fail because rule methods are missing.
- [ ] Implement the minimal rule methods.
- [ ] Run `gradle testDebugUnitTest` and confirm tests pass.

### Task 2: Database and Repository

**Files:**
- Modify: `app/src/main/java/com/example/campustask/data/CommunityDbHelper.java`
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskDbHelper.java`
- Modify: `app/src/main/java/com/example/campustask/data/CampusTaskRepository.java`
- Modify: `app/src/main/java/com/example/campustask/model/MarketplaceItem.java`
- Create: `app/src/main/java/com/example/campustask/model/WalletTransaction.java`

- [ ] Upgrade community DB to store marketplace sale status, buyer, sold price, platform fee, seller income, and sold time.
- [ ] Upgrade personal DB to store wallet transactions.
- [ ] Add repository methods to delete owned forum posts, reject self-bids, settle a selected bid, and read wallet balance/transactions.

### Task 3: UI Wiring

**Files:**
- Modify: `app/src/main/java/com/example/campustask/MainActivity.java`

- [ ] Move "我的" navigation button after service navigation buttons.
- [ ] Show "删除帖子" only for post authors.
- [ ] Hide bid button for sellers and sold items; show seller-only "确认售卖" on active items with bids.
- [ ] Show virtual currency balance in "我的".

### Task 4: Verification and Artifacts

**Files:**
- Output: `outputs/CampusHub-market-wallet-debug.apk`
- Output: `outputs/CampusHub-market-wallet-source.zip`

- [ ] Run `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- [ ] Export the APK and source zip under the requested `outputs` directory.
