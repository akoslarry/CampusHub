package com.example.campustask.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.campustask.model.Course;
import com.example.campustask.model.CampusService;
import com.example.campustask.model.CommunityRules;
import com.example.campustask.model.Dish;
import com.example.campustask.model.FoodOrder;
import com.example.campustask.model.FoodOrderRules;
import com.example.campustask.model.ForumComment;
import com.example.campustask.model.ForumPost;
import com.example.campustask.model.MarketBid;
import com.example.campustask.model.MarketplaceComment;
import com.example.campustask.model.MarketplaceItem;
import com.example.campustask.model.Merchant;
import com.example.campustask.model.ServiceCatalog;
import com.example.campustask.model.TaskItem;
import com.example.campustask.model.TaskRules;
import com.example.campustask.model.WalletTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CampusTaskRepository {
    private final Context appContext;
    private final AuthDbHelper authDbHelper;
    private final CommunityDbHelper communityDbHelper;
    private CampusTaskDbHelper dbHelper;
    private String activeUsername = "";

    public CampusTaskRepository(Context context) {
        appContext = context.getApplicationContext();
        authDbHelper = new AuthDbHelper(appContext);
        communityDbHelper = new CommunityDbHelper(appContext);
        migrateLegacyUsers();
    }

    public void useAccount(String username) {
        activeUsername = username == null ? "" : username.trim();
        if (dbHelper != null) {
            dbHelper.close();
        }
        dbHelper = new CampusTaskDbHelper(appContext, activeUsername);
    }

    public void clearAccount() {
        activeUsername = "";
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    public boolean registerUser(String username, String password) {
        ContentValues values = new ContentValues();
        values.put("username", username.trim());
        values.put("password", password);
        long result = authDbHelper.getWritableDatabase().insert("users", null, values);
        return result > 0;
    }

    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = authDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "users",
                new String[]{"id"},
                "username=? AND password=?",
                new String[]{username.trim(), password},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = authDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "users",
                new String[]{"role"},
                "username=?",
                new String[]{username == null ? "" : username.trim()},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return "user";
    }

    public boolean isAdmin(String username) {
        return "admin".equals(getUserRole(username));
    }

    // ===== 个人信息（SharedPreferences存储，按用户隔离）=====

    private static String profilePrefsName(String username) {
        return "campus_profile_" + (username == null ? "guest" : username.trim());
    }

    public String getProfileField(String username, String field) {
        return appContext.getSharedPreferences(profilePrefsName(username), Context.MODE_PRIVATE)
                .getString(field, "");
    }

    public void setProfileField(String username, String field, String value) {
        appContext.getSharedPreferences(profilePrefsName(username), Context.MODE_PRIVATE)
                .edit()
                .putString(field, value == null ? "" : value)
                .apply();
    }

    private CampusTaskDbHelper businessDb() {
        if (dbHelper == null) {
            useAccount(activeUsername == null || activeUsername.isEmpty() ? "guest" : activeUsername);
        }
        return dbHelper;
    }

    private void migrateLegacyUsers() {
        SQLiteDatabase legacy = null;
        try {
            legacy = appContext.openOrCreateDatabase("campus_task.db", Context.MODE_PRIVATE, null);
            Cursor cursor = legacy.query("users", new String[]{"username", "password"}, null, null, null, null, null);
            try {
                SQLiteDatabase auth = authDbHelper.getWritableDatabase();
                while (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("username", cursor.getString(cursor.getColumnIndexOrThrow("username")));
                    values.put("password", cursor.getString(cursor.getColumnIndexOrThrow("password")));
                    auth.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                }
            } finally {
                cursor.close();
            }
        } catch (Exception ignored) {
            // Older installs may not have the legacy database or users table.
        } finally {
            if (legacy != null) {
                legacy.close();
            }
        }
    }

    public List<CampusService> getHomeServices() {
        List<CampusService> services = new ArrayList<>(ServiceCatalog.defaultServices());
        services.addAll(getCustomServices());
        return services;
    }

    public long saveCustomService(String name, String category, String description) {
        if (!CommunityRules.canAddService(name, category, description)) {
            return -1;
        }
        String cleanName = name.trim();
        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("category", category.trim());
        values.put("description", description.trim());
        values.put("icon_text", iconForService(cleanName));
        values.put("color", colorForService(cleanName));
        values.put("created_at", System.currentTimeMillis());
        return businessDb().getWritableDatabase().insert("custom_services", null, values);
    }

    public List<CampusService> getCustomServices() {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("custom_services", null, null, null, null, null, "created_at DESC");
        List<CampusService> services = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                services.add(readCustomService(cursor));
            }
        } finally {
            cursor.close();
        }
        return services;
    }

    public CampusService getCustomService(String id) {
        if (!CommunityRules.isCustomServiceId(id)) {
            return null;
        }
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query(
                "custom_services",
                null,
                "id=?",
                new String[]{id.substring("custom_".length())},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst() ? readCustomService(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public boolean deleteCustomService(String id) {
        if (!CommunityRules.isCustomServiceId(id)) {
            return false;
        }
        int deleted = businessDb().getWritableDatabase().delete(
                "custom_services",
                "id=?",
                new String[]{id.substring("custom_".length())}
        );
        return deleted > 0;
    }

    public List<Course> getCourses() {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("courses", null, null, null, null, null, "name ASC");
        List<Course> courses = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                courses.add(readCourse(cursor));
            }
        } finally {
            cursor.close();
        }
        return courses;
    }

    public Course getCourse(long id) {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("courses", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        try {
            return cursor.moveToFirst() ? readCourse(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public long saveCourse(Course course) {
        ContentValues values = new ContentValues();
        values.put("name", course.name.trim());
        values.put("teacher", course.teacher.trim());
        values.put("location", course.location.trim());
        values.put("color", course.color);
        values.put("weekday", course.weekday);
        values.put("start_section", course.startSection);
        values.put("end_section", course.endSection);
        values.put("start_week", course.startWeek);
        values.put("end_week", course.endWeek);
        SQLiteDatabase db = businessDb().getWritableDatabase();
        if (course.id > 0) {
            db.update("courses", values, "id=?", new String[]{String.valueOf(course.id)});
            return course.id;
        }
        return db.insert("courses", null, values);
    }

    public void deleteCourse(long id) {
        SQLiteDatabase db = businessDb().getWritableDatabase();
        db.delete("tasks", "course_id=?", new String[]{String.valueOf(id)});
        db.delete("courses", "id=?", new String[]{String.valueOf(id)});
    }

    public List<TaskItem> getTasks() {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("tasks", null, null, null, null, null, null);
        List<TaskItem> tasks = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                tasks.add(readTask(cursor));
            }
        } finally {
            cursor.close();
        }
        Collections.sort(tasks, TaskRules.dashboardComparator());
        return tasks;
    }

    public TaskItem getTask(long id) {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("tasks", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        try {
            return cursor.moveToFirst() ? readTask(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public long saveTask(TaskItem task) {
        ContentValues values = new ContentValues();
        values.put("course_id", task.courseId);
        values.put("title", task.title.trim());
        values.put("description", task.description.trim());
        values.put("due_at", task.dueAtMillis);
        values.put("remind_at", task.remindAtMillis);
        values.put("repeat_minutes", task.repeatMinutes);
        values.put("priority", task.priority);
        values.put("completed", task.completed ? 1 : 0);
        SQLiteDatabase db = businessDb().getWritableDatabase();
        if (task.id > 0) {
            db.update("tasks", values, "id=?", new String[]{String.valueOf(task.id)});
            return task.id;
        }
        return db.insert("tasks", null, values);
    }

    public void setTaskCompleted(long id, boolean completed) {
        ContentValues values = new ContentValues();
        values.put("completed", completed ? 1 : 0);
        businessDb().getWritableDatabase().update("tasks", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteTask(long id) {
        businessDb().getWritableDatabase().delete("tasks", "id=?", new String[]{String.valueOf(id)});
    }

    public int countUnfinished() {
        int count = 0;
        for (TaskItem task : getTasks()) {
            if (!task.completed) {
                count++;
            }
        }
        return count;
    }

    public List<Merchant> getMerchants() {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("merchants", null, null, null, null, null, "id ASC");
        List<Merchant> merchants = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                merchants.add(new Merchant(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("notice"))
                ));
            }
        } finally {
            cursor.close();
        }
        return merchants;
    }

    public List<Dish> getDishesForMerchant(long merchantId) {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT dishes.id, dishes.merchant_id, merchants.name AS merchant_name, dishes.name, dishes.price " +
                        "FROM dishes INNER JOIN merchants ON dishes.merchant_id = merchants.id " +
                        "WHERE dishes.merchant_id=? ORDER BY dishes.id ASC",
                new String[]{String.valueOf(merchantId)}
        );
        List<Dish> dishes = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                dishes.add(readDish(cursor));
            }
        } finally {
            cursor.close();
        }
        return dishes;
    }

    public long createFoodOrder(List<Dish> dishes) {
        if (!FoodOrderRules.canSubmit(dishes)) {
            return -1;
        }
        String merchantName = dishes.get(0).merchantName;
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < dishes.size(); i++) {
            if (i > 0) {
                items.append("、");
            }
            items.append(dishes.get(i).name);
        }
        ContentValues values = new ContentValues();
        values.put("merchant_name", merchantName);
        values.put("items_summary", items.toString());
        values.put("total_price", FoodOrderRules.total(dishes));
        values.put("status", FoodOrderRules.STATUS_WAITING);
        values.put("created_at", System.currentTimeMillis());
        return businessDb().getWritableDatabase().insert("food_orders", null, values);
    }

    public List<FoodOrder> getFoodOrders() {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("food_orders", null, null, null, null, null, "created_at DESC");
        List<FoodOrder> orders = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                orders.add(readFoodOrder(cursor));
            }
        } finally {
            cursor.close();
        }
        return orders;
    }

    public void updateFoodOrderStatus(long id, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        businessDb().getWritableDatabase().update("food_orders", values, "id=?", new String[]{String.valueOf(id)});
    }

    public long createForumPost(String title, String content, String author) {
        if (!CommunityRules.canPublishPost(title, content)) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("content", content.trim());
        values.put("author", cleanUser(author));
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("forum_posts", null, values);
    }

    public List<ForumPost> getForumPosts() {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("forum_posts", null, null, null, null, null, "created_at DESC");
        List<ForumPost> posts = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                posts.add(readForumPost(cursor));
            }
        } finally {
            cursor.close();
        }
        return posts;
    }

    public boolean deleteForumPost(long postId, String username) {
        ForumPost post = getForumPost(postId);
        if (post == null || !CommunityRules.isOwner(post.author, username)) {
            return false;
        }
        SQLiteDatabase db = communityDbHelper.getWritableDatabase();
        db.delete("forum_comments", "post_id=?", new String[]{String.valueOf(postId)});
        int deleted = db.delete("forum_posts", "id=?", new String[]{String.valueOf(postId)});
        return deleted > 0;
    }

    public long createForumComment(long postId, String content, String author) {
        if (postId <= 0 || content == null || content.trim().isEmpty()) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("post_id", postId);
        values.put("content", content.trim());
        values.put("author", cleanUser(author));
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("forum_comments", null, values);
    }

    public List<ForumComment> getForumComments(long postId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "forum_comments",
                null,
                "post_id=?",
                new String[]{String.valueOf(postId)},
                null,
                null,
                "created_at ASC"
        );
        List<ForumComment> comments = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                comments.add(readForumComment(cursor));
            }
        } finally {
            cursor.close();
        }
        return comments;
    }

    public long createMarketplaceItem(String name, int price, String description, String contact, String seller, String imageUri) {
        if (!CommunityRules.canPublishItem(name, price, description, contact)) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("price", price);
        values.put("description", description.trim());
        values.put("contact", contact.trim());
        values.put("seller", cleanUser(seller));
        values.put("image_uri", imageUri == null ? "" : imageUri);
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("marketplace_items", null, values);
    }

    public List<MarketplaceItem> getMarketplaceItems() {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("marketplace_items", null, null, null, null, null, "created_at DESC");
        List<MarketplaceItem> items = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                items.add(readMarketplaceItem(cursor));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public long createMarketplaceComment(long itemId, String content, String author) {
        if (itemId <= 0 || content == null || content.trim().isEmpty()) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put("content", content.trim());
        values.put("author", cleanUser(author));
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("marketplace_comments", null, values);
    }

    public List<MarketplaceComment> getMarketplaceComments(long itemId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "marketplace_comments",
                null,
                "item_id=?",
                new String[]{String.valueOf(itemId)},
                null,
                null,
                "created_at ASC"
        );
        List<MarketplaceComment> comments = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                comments.add(readMarketplaceComment(cursor));
            }
        } finally {
            cursor.close();
        }
        return comments;
    }

    public long createMarketBid(long itemId, int price, String bidder) {
        MarketplaceItem item = getMarketplaceItem(itemId);
        if (item == null
                || !CommunityRules.canBidOnItem(item.seller, bidder, item.sold, price, item.currentPrice)
                || !CommunityRules.canAffordBid(getWalletBalance(), price)) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put("price", price);
        values.put("bidder", cleanUser(bidder));
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("market_bids", null, values);
    }

    public boolean settleMarketplaceSale(long itemId, long bidId, String seller) {
        MarketplaceItem item = getMarketplaceItem(itemId);
        MarketBid bid = getMarketBid(bidId);
        if (item == null || bid == null || bid.itemId != itemId
                || !CommunityRules.canSettleSale(item.seller, seller, item.sold, item.bidCount)) {
            return false;
        }
        if (!CommunityRules.canCompleteMarketplacePayment(getWalletBalanceForUser(bid.bidder), bid.price)) {
            return false;
        }
        int fee = CommunityRules.platformFee(bid.price);
        int income = CommunityRules.sellerIncome(bid.price);
        ContentValues values = new ContentValues();
        values.put("status", "sold");
        values.put("sold_price", bid.price);
        values.put("buyer", bid.bidder);
        values.put("sold_at", System.currentTimeMillis());
        values.put("platform_fee", fee);
        values.put("seller_income", income);
        int updated = communityDbHelper.getWritableDatabase().update(
                "marketplace_items",
                values,
                "id=? AND status<>?",
                new String[]{String.valueOf(itemId), "sold"}
        );
        if (updated <= 0) {
            return false;
        }
        addWalletTransaction(income, "闲置售卖：" + item.name + "，平台费 " + fee + " 元");
        addWalletTransactionForUser(bid.bidder, -bid.price, "\u95f2\u7f6e\u8d2d\u4e70\uff1a" + item.name);
        return true;
    }

    public List<MarketBid> getMarketBids(long itemId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "market_bids",
                null,
                "item_id=?",
                new String[]{String.valueOf(itemId)},
                null,
                null,
                "price DESC, created_at DESC"
        );
        List<MarketBid> bids = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                bids.add(readMarketBid(cursor));
            }
        } finally {
            cursor.close();
        }
        return bids;
    }

    private MarketplaceItem getMarketplaceItem(long itemId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("marketplace_items", null, "id=?", new String[]{String.valueOf(itemId)}, null, null, null);
        try {
            return cursor.moveToFirst() ? readMarketplaceItem(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    private ForumPost getForumPost(long postId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("forum_posts", null, "id=?", new String[]{String.valueOf(postId)}, null, null, null);
        try {
            return cursor.moveToFirst() ? readForumPost(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    private MarketBid getMarketBid(long bidId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("market_bids", null, "id=?", new String[]{String.valueOf(bidId)}, null, null, null);
        try {
            return cursor.moveToFirst() ? readMarketBid(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public int getWalletBalance() {
        return getWalletBalance(businessDb().getReadableDatabase());
    }

    private int getWalletBalance(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM wallet_transactions", null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public List<WalletTransaction> getWalletTransactions() {
        SQLiteDatabase db = businessDb().getReadableDatabase();
        Cursor cursor = db.query("wallet_transactions", null, null, null, null, null, "created_at DESC");
        List<WalletTransaction> transactions = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                transactions.add(readWalletTransaction(cursor));
            }
        } finally {
            cursor.close();
        }
        return transactions;
    }

    public boolean rechargeWallet(int amount) {
        if (!CommunityRules.canRechargeWallet(amount)) {
            return false;
        }
        addWalletTransaction(amount, "钱包充值");
        return true;
    }

    public boolean withdrawWallet(int amount) {
        if (!CommunityRules.canWithdrawWallet(getWalletBalance(), amount)) {
            return false;
        }
        addWalletTransaction(-amount, "钱包提现");
        return true;
    }

    public void addWalletTransaction(int amount, String description) {
        addWalletTransaction(businessDb().getWritableDatabase(), amount, description);
    }

    private int getWalletBalanceForUser(String username) {
        CampusTaskDbHelper helper = new CampusTaskDbHelper(appContext, username);
        try {
            return getWalletBalance(helper.getReadableDatabase());
        } finally {
            helper.close();
        }
    }

    private void addWalletTransactionForUser(String username, int amount, String description) {
        CampusTaskDbHelper helper = new CampusTaskDbHelper(appContext, username);
        try {
            addWalletTransaction(helper.getWritableDatabase(), amount, description);
        } finally {
            helper.close();
        }
    }

    private void addWalletTransaction(SQLiteDatabase db, int amount, String description) {
        if (amount == 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("description", description);
        values.put("created_at", System.currentTimeMillis());
        db.insert("wallet_transactions", null, values);
    }

    private Course readCourse(Cursor cursor) {
        return new Course(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                cursor.getString(cursor.getColumnIndexOrThrow("location")),
                cursor.getInt(cursor.getColumnIndexOrThrow("color")),
                cursor.getInt(cursor.getColumnIndexOrThrow("weekday")),
                cursor.getInt(cursor.getColumnIndexOrThrow("start_section")),
                cursor.getInt(cursor.getColumnIndexOrThrow("end_section")),
                cursor.getInt(cursor.getColumnIndexOrThrow("start_week")),
                cursor.getInt(cursor.getColumnIndexOrThrow("end_week"))
        );
    }

    private CampusService readCustomService(Cursor cursor) {
        return new CampusService(
                "custom_" + cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getString(cursor.getColumnIndexOrThrow("icon_text")),
                cursor.getInt(cursor.getColumnIndexOrThrow("color")),
                true
        );
    }

    private Dish readDish(Cursor cursor) {
        return new Dish(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("merchant_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("merchant_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getInt(cursor.getColumnIndexOrThrow("price"))
        );
    }

    private FoodOrder readFoodOrder(Cursor cursor) {
        return new FoodOrder(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("merchant_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("items_summary")),
                cursor.getInt(cursor.getColumnIndexOrThrow("total_price")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }

    private ForumPost readForumPost(Cursor cursor) {
        return new ForumPost(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("content")),
                cursor.getString(cursor.getColumnIndexOrThrow("author")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }

    private ForumComment readForumComment(Cursor cursor) {
        return new ForumComment(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("post_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("content")),
                cursor.getString(cursor.getColumnIndexOrThrow("author")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }

    private MarketplaceItem readMarketplaceItem(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        int startingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("price"));
        boolean sold = "sold".equals(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        int soldPrice = cursor.getInt(cursor.getColumnIndexOrThrow("sold_price"));
        int activePrice = Math.max(startingPrice, maxBidPrice(id));
        String imageUri = "";
        try {
            imageUri = cursor.getString(cursor.getColumnIndexOrThrow("image_uri"));
        } catch (Exception ignored) {
        }
        return new MarketplaceItem(
                id,
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                startingPrice,
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("contact")),
                cursor.getString(cursor.getColumnIndexOrThrow("seller")),
                imageUri,
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                sold ? soldPrice : activePrice,
                countRows("market_bids", "item_id=?", new String[]{String.valueOf(id)}),
                sold,
                soldPrice,
                cursor.getString(cursor.getColumnIndexOrThrow("buyer")),
                cursor.getLong(cursor.getColumnIndexOrThrow("sold_at")),
                cursor.getInt(cursor.getColumnIndexOrThrow("platform_fee")),
                cursor.getInt(cursor.getColumnIndexOrThrow("seller_income"))
        );
    }

    private MarketplaceComment readMarketplaceComment(Cursor cursor) {
        return new MarketplaceComment(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("item_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("content")),
                cursor.getString(cursor.getColumnIndexOrThrow("author")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }

    private MarketBid readMarketBid(Cursor cursor) {
        return new MarketBid(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("item_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("price")),
                cursor.getString(cursor.getColumnIndexOrThrow("bidder")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }

    private WalletTransaction readWalletTransaction(Cursor cursor) {
        return new WalletTransaction(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }

    private int maxBidPrice(long itemId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(price) FROM market_bids WHERE item_id=?", new String[]{String.valueOf(itemId)});
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private int countRows(String table, String selection, String[] args) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query(table, new String[]{"COUNT(*)"}, selection, args, null, null, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private TaskItem readTask(Cursor cursor) {
        return new TaskItem(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("course_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("due_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("remind_at")),
                cursor.getInt(cursor.getColumnIndexOrThrow("repeat_minutes")),
                cursor.getInt(cursor.getColumnIndexOrThrow("priority")),
                cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1
        );
    }

    private String cleanUser(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "同学";
        }
        return value.trim();
    }

    private String iconForService(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "服";
        }
        return name.trim().substring(0, 1);
    }

    private int colorForService(String name) {
        int[] palette = new int[]{
                0xFF2563EB,
                0xFF10B981,
                0xFFF59E0B,
                0xFF8B5CF6,
                0xFFEF4444,
                0xFF14B8A6
        };
        long hash = name == null ? 0 : name.hashCode();
        int index = (int) (Math.abs(hash) % palette.length);
        return palette[index];
    }

    // ===== 教室预约（公共数据库，跨账户共享）=====

    public boolean createClassroomReservation(String room, String timeSlot, String username) {
        if (room == null || room.trim().isEmpty() || timeSlot == null || timeSlot.trim().isEmpty()) {
            return false;
        }
        if (isClassroomSlotBooked(room, timeSlot)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("room", room.trim());
        values.put("time_slot", timeSlot.trim());
        values.put("username", cleanUser(username));
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("classroom_reservations", null, values) > 0;
    }

    public boolean isClassroomSlotBooked(String room, String timeSlot) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("classroom_reservations", new String[]{"id"},
                "room=? AND time_slot=?", new String[]{room, timeSlot}, null, null, null);
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public boolean hasUserTimeConflict(String timeSlot, String username) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("classroom_reservations", new String[]{"id"},
                "time_slot=? AND username=?", new String[]{timeSlot, cleanUser(username)}, null, null, null);
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public List<String[]> getUserReservations(String username) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("classroom_reservations", null,
                "username=?", new String[]{cleanUser(username)}, null, null, "created_at DESC");
        List<String[]> result = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String room = cursor.getString(cursor.getColumnIndexOrThrow("room"));
                String timeSlot = cursor.getString(cursor.getColumnIndexOrThrow("time_slot"));
                result.add(new String[]{String.valueOf(id), room, timeSlot});
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public boolean cancelClassroomReservation(long id, String username) {
        SQLiteDatabase db = communityDbHelper.getWritableDatabase();
        int deleted = db.delete("classroom_reservations", "id=? AND username=?",
                new String[]{String.valueOf(id), cleanUser(username)});
        return deleted > 0;
    }

    public List<String> getBookedSlotsForRoom(String room) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("classroom_reservations", new String[]{"time_slot"},
                "room=?", new String[]{room}, null, null, null);
        List<String> slots = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                slots.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return slots;
    }

    // ===== 论坛版块 =====

    public long createForumSection(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("description", description == null ? "" : description.trim());
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("forum_sections", null, values);
    }

    public List<String[]> getForumSections() {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("forum_sections", null, null, null, null, null, "created_at ASC");
        List<String[]> sections = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                int postCount = countForumPostsInSection(id);
                sections.add(new String[]{String.valueOf(id), name, desc, String.valueOf(postCount)});
            }
        } finally {
            cursor.close();
        }
        return sections;
    }

    public boolean deleteForumSection(long sectionId) {
        SQLiteDatabase db = communityDbHelper.getWritableDatabase();
        db.delete("forum_comments_v2", "post_id IN (SELECT id FROM forum_posts_v2 WHERE section_id=?)",
                new String[]{String.valueOf(sectionId)});
        db.delete("forum_posts_v2", "section_id=?", new String[]{String.valueOf(sectionId)});
        int deleted = db.delete("forum_sections", "id=?", new String[]{String.valueOf(sectionId)});
        return deleted > 0;
    }

    // ===== 论坛帖子（新版，带版块和附件）=====

    public long createForumPostV2(long sectionId, String title, String content, String author, String attachment) {
        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("section_id", sectionId);
        values.put("title", title.trim());
        values.put("content", content.trim());
        values.put("author", cleanUser(author));
        values.put("attachment", attachment == null ? "" : attachment);
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("forum_posts_v2", null, values);
    }

    public List<String[]> getForumPostsBySection(long sectionId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("forum_posts_v2", null, "section_id=?",
                new String[]{String.valueOf(sectionId)}, null, null, "created_at DESC");
        List<String[]> posts = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String author = cursor.getString(cursor.getColumnIndexOrThrow("author"));
                String attachment = cursor.getString(cursor.getColumnIndexOrThrow("attachment"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                posts.add(new String[]{String.valueOf(id), title, content, author, attachment, String.valueOf(createdAt)});
            }
        } finally {
            cursor.close();
        }
        return posts;
    }

    public boolean deleteForumPostV2(long postId, String username, boolean isAdmin) {
        SQLiteDatabase db = communityDbHelper.getWritableDatabase();
        String whereClause;
        String[] whereArgs;
        if (isAdmin) {
            whereClause = "id=?";
            whereArgs = new String[]{String.valueOf(postId)};
        } else {
            whereClause = "id=? AND author=?";
            whereArgs = new String[]{String.valueOf(postId), cleanUser(username)};
        }
        db.delete("forum_comments_v2", "post_id=?", new String[]{String.valueOf(postId)});
        int deleted = db.delete("forum_posts_v2", whereClause, whereArgs);
        return deleted > 0;
    }

    public long createForumCommentV2(long postId, String content, String author) {
        if (content == null || content.trim().isEmpty()) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("post_id", postId);
        values.put("content", content.trim());
        values.put("author", cleanUser(author));
        values.put("created_at", System.currentTimeMillis());
        return communityDbHelper.getWritableDatabase().insert("forum_comments_v2", null, values);
    }

    public List<String[]> getForumCommentsV2(long postId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("forum_comments_v2", null, "post_id=?",
                new String[]{String.valueOf(postId)}, null, null, "created_at ASC");
        List<String[]> comments = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String author = cursor.getString(cursor.getColumnIndexOrThrow("author"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                comments.add(new String[]{String.valueOf(id), content, author, String.valueOf(createdAt)});
            }
        } finally {
            cursor.close();
        }
        return comments;
    }

    private int countForumPostsInSection(long sectionId) {
        SQLiteDatabase db = communityDbHelper.getReadableDatabase();
        Cursor cursor = db.query("forum_posts_v2", new String[]{"COUNT(*)"},
                "section_id=?", new String[]{String.valueOf(sectionId)}, null, null, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }
}

