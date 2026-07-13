package com.example.campustask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.campustask.data.CampusTaskRepository;
import com.example.campustask.model.AuthRules;
import com.example.campustask.model.CampusService;
import com.example.campustask.model.ClassroomSearchRules;
import com.example.campustask.model.ClassroomSlot;
import com.example.campustask.model.CommunityRules;
import com.example.campustask.model.Course;
import com.example.campustask.model.CourseImportParser;
import com.example.campustask.model.Dish;
import com.example.campustask.model.FoodOrder;
import com.example.campustask.model.FoodOrderRules;
import com.example.campustask.model.ForumComment;
import com.example.campustask.model.ForumPost;
import com.example.campustask.model.MarketBid;
import com.example.campustask.model.MarketplaceComment;
import com.example.campustask.model.MarketplaceItem;
import com.example.campustask.model.Merchant;
import com.example.campustask.model.ReservationRules;
import com.example.campustask.model.ScheduleRules;
import com.example.campustask.model.ScheduleExcelParser;
import com.example.campustask.model.ServiceCatalog;
import com.example.campustask.model.TaskItem;
import com.example.campustask.model.TaskRules;
import com.example.campustask.model.WalletTransaction;
import com.example.campustask.model.XlsxScheduleParser;
import com.example.campustask.reminder.CourseReminderScheduler;
import com.example.campustask.reminder.ReminderScheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "campus_hub_auth";
    private static final String PREF_USERNAME = "username";
    private static final String ACADEMIC_SYSTEM_URL = "https://jwxt.bistu.edu.cn/jwapp/sys/homeapp/home/index.html?av=&contextPath=/jwapp#/";
    private static final int TAB_HOME = 0;
    private static final int TAB_SCHEDULE = 1;
    private static final int TAB_FOOD = 2;
    private static final int TAB_CLASSROOM = 3;
    private static final int TAB_TASKS = 4;
    private static final int TAB_MINE = 5;
    private static final int TAB_FORUM = 6;
    private static final int TAB_MARKET = 7;
    private static final int TAB_CUSTOM_SERVICE = 8;
    private static final int MAX_WEEK = 20;
    private static final int SECTION_COUNT = 16;
    private static final int REQUEST_CODE_IMPORT_EXCEL = 1001;
    private static final int REQUEST_CODE_PICK_FORUM_IMAGE = 1002;
    private static final int REQUEST_CODE_PICK_AVATAR = 1003;
    private static final int REQUEST_CODE_PICK_MARKET_IMAGE = 1004;
    private static final int COLOR_BG = 0xFFF4F8FF;
    private static final int COLOR_SURFACE = 0xFFFFFFFF;
    private static final int COLOR_SURFACE_TINT = 0xFFF8FBFF;
    private static final int COLOR_TEXT = 0xFF102033;
    private static final int COLOR_MUTED = 0xFF66758A;
    private static final int COLOR_LINE = 0xFFE4ECF7;
    private static final int COLOR_BRAND = 0xFF1677FF;
    private static final int COLOR_BRAND_DEEP = 0xFF0052D9;
    private static final int COLOR_BRAND_SOFT = 0xFF69B1FF;
    private static final int COLOR_BRAND_MIST = 0xFFEAF3FF;
    private static final int COLOR_CYAN = 0xFF13C2C2;
    private static final int COLOR_SUCCESS = 0xFF10B981;
    private static final int COLOR_WARNING = 0xFFF59E0B;
    private static final int RADIUS_CARD = 24;
    private static final int RADIUS_CONTROL = 16;
    private static final int PAGE_PADDING = 16;

    private final SimpleDateFormat dateFormat = TaskRules.campusDateFormat();
    private final List<Dish> cart = new ArrayList<>();
    private final List<String> reservations = new ArrayList<>();
    private final List<Button> navButtons = new ArrayList<>();

    private CampusTaskRepository repository;
    private ReminderScheduler reminderScheduler;
    private CourseReminderScheduler courseReminderScheduler;
    private LinearLayout content;
    private TextView titleView;
    private TextView subtitleView;
    private String currentUsername;
    private String classroomQuery = "";
    private String selectedCustomServiceId = "";
    private long selectedForumSectionId = 0;
    private long selectedForumPostId = 0;
    private long selectedMarketItemId = 0;
    private String pendingAttachmentPath = "";
    private String pendingAvatarPath = "";
    private String pendingMarketImage = "";
    private int currentTab = TAB_HOME;
    private int currentWeek = 1;
    private int animationIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        repository = new CampusTaskRepository(this);
        reminderScheduler = new ReminderScheduler(this);
        courseReminderScheduler = new CourseReminderScheduler(this);
        requestNotificationPermission();
        currentUsername = getPrefs().getString(PREF_USERNAME, "");
        if (currentUsername == null || currentUsername.isEmpty()) {
            renderAuthScreen(false);
        } else {
            repository.useAccount(currentUsername);
            buildShell();
            render();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_EXCEL && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importExcelSchedule(data.getData());
        }
        if (requestCode == REQUEST_CODE_PICK_FORUM_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pendingAttachmentPath = data.getData().toString();
            toast("图片已选择");
        }
        if (requestCode == REQUEST_CODE_PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pendingAvatarPath = data.getData().toString();
            toast("头像已选择");
        }
        if (requestCode == REQUEST_CODE_PICK_MARKET_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pendingMarketImage = data.getData().toString();
            toast("物品图片已选择");
        }
    }

    private void importExcelSchedule(Uri uri) {
        try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                toast("无法读取选中的文件");
                return;
            }
            List<List<String>> rows = XlsxScheduleParser.parse(input);
            List<Course> courses = ScheduleExcelParser.parse(rows, repository.getCourses().size());
            for (Course imported : courses) {
                repository.saveCourse(imported);
            }
            courseReminderScheduler.scheduleAll(currentUsername);
            currentTab = TAB_SCHEDULE;
            toast("已导入 " + courses.size() + " 门课程，已设置上课提醒");
            render();
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
        } catch (Exception e) {
            toast("导入失败：" + e.getMessage());
        }
    }

    private void configureWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BRAND);
            getWindow().setNavigationBarColor(Color.WHITE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    private void renderAuthScreen(boolean registerMode) {
        FrameLayout shell = new FrameLayout(this);
        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.campus_login_bg);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        shell.addView(background, new FrameLayout.LayoutParams(-1, -1));

        View scrim = new View(this);
        scrim.setBackground(gradient(Color.argb(120, 0, 82, 217), Color.argb(238, 244, 248, 255), 0));
        shell.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        shell.addView(new AmbientBackdropView(this), new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = vertical();
        root.setPadding(dp(24), dp(34), dp(24), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);

        LinearLayout heroCopy = vertical();
        heroCopy.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCopy.addView(schoolLogoBadge(dp(64), true), marginBottom(-2, -2, dp(8)));
        TextView school = text("北京信息科技大学", 22, Color.WHITE, true);
        school.setGravity(Gravity.CENTER);
        heroCopy.addView(school, marginBottom(-1, -2, dp(2)));
        TextView brand = text("北信科有品", 31, Color.WHITE, true);
        brand.setGravity(Gravity.CENTER);
        heroCopy.addView(brand, marginBottom(-1, -2, dp(4)));
        TextView subtitle = text("一站式校园服务平台", 15, Color.argb(232, 255, 255, 255), false);
        subtitle.setGravity(Gravity.CENTER);
        heroCopy.addView(subtitle);
        TextView trust = text("安全 · 便捷 · 智慧校园", 13, Color.argb(215, 255, 255, 255), true);
        trust.setGravity(Gravity.CENTER);
        trust.setPadding(0, dp(8), 0, 0);
        heroCopy.addView(trust);
        root.addView(heroCopy, marginBottom(-1, -2, dp(58)));

        LinearLayout card = glassCard();
        TextView formTitle = text(registerMode ? "创建校园账号" : "账号登录", 22, COLOR_TEXT, true);
        formTitle.setGravity(Gravity.CENTER);
        card.addView(formTitle, marginBottom(-1, -2, dp(4)));
        TextView formHint = text(registerMode ? "注册后立即进入你的校园服务大厅" : "使用本地账号进入服务大厅", 14, COLOR_MUTED, false);
        formHint.setGravity(Gravity.CENTER);
        card.addView(formHint, marginBottom(-1, -2, dp(18)));

        EditText username = input("用户名（至少 3 位）", "");
        EditText password = input("密码（至少 6 位）", "");
        password.setInputType(0x00000081);
        EditText confirm = input("确认密码", "");
        confirm.setInputType(0x00000081);
        card.addView(username, marginBottom(-1, -2, dp(10)));
        card.addView(password, marginBottom(-1, -2, dp(10)));
        if (registerMode) {
            card.addView(confirm, marginBottom(-1, -2, dp(10)));
        }
        Button primary = primaryButton(registerMode ? "注册并登录" : "登录");
        primary.setOnClickListener(v -> {
            String name = username.getText().toString().trim();
            String pass = password.getText().toString();
            if (!AuthRules.isValidUsername(name)) {
                toast("用户名至少 3 位");
                return;
            }
            if (!AuthRules.isValidPassword(pass)) {
                toast("密码至少 6 位");
                return;
            }
            if (registerMode) {
                if (!AuthRules.passwordsMatch(pass, confirm.getText().toString())) {
                    toast("两次密码不一致");
                    return;
                }
                if (!repository.registerUser(name, pass)) {
                    toast("用户名已存在，请直接登录");
                    return;
                }
                loginSuccess(name);
            } else if (repository.loginUser(name, pass)) {
                loginSuccess(name);
            } else {
                toast("用户名或密码错误");
            }
        });
        card.addView(primary, marginTop(-1, dp(54), dp(8)));
        Button switchMode = smallButton(registerMode ? "已有账号，去登录" : "没有账号，去注册");
        switchMode.setOnClickListener(v -> renderAuthScreen(!registerMode));
        card.addView(switchMode, marginTop(-1, -2, dp(6)));
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
        animateIn(card);
    }

    private void loginSuccess(String username) {
        currentUsername = username;
        repository.useAccount(username);
        getPrefs().edit().putString(PREF_USERNAME, username).apply();
        courseReminderScheduler.setCurrentWeek(currentWeek);
        buildShell();
        render();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void buildShell() {
        navButtons.clear();
        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundColor(COLOR_BG);
        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.app_clean_bg);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background.setAlpha(0.72f);
        shell.addView(background, new FrameLayout.LayoutParams(-1, -1));
        shell.addView(new AmbientBackdropView(this), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = vertical();
        root.setBackgroundColor(Color.TRANSPARENT);
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);

        titleView = text("", 1, Color.TRANSPARENT, false);
        subtitleView = text("", 1, Color.TRANSPARENT, false);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);
        content = vertical();
        content.setPadding(dp(PAGE_PADDING), dp(10), dp(PAGE_PADDING), dp(18));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = horizontal();
        nav.setPadding(dp(5), dp(4), dp(5), dp(4));
        nav.setBackground(gradient(Color.argb(214, 255, 255, 255), Color.argb(138, 235, 245, 255),
                dp(24), Color.argb(154, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nav.setElevation(dp(9));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        nav.addView(navButton("首页", TAB_HOME), new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(navButton("外卖", TAB_FOOD), new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(navButton("闲置", TAB_MARKET), new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(navButton("论坛", TAB_FORUM), new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(navButton("我的", TAB_MINE), new LinearLayout.LayoutParams(0, -2, 1));
    }

    private Button navButton(String label, int tab) {
        Button button = chipButton(label);
        button.setTag(tab);
        button.setTextSize(10);
        button.setPadding(0, dp(2), 0, 0);
        setTopIcon(button, navIconForTab(tab), 20);
        button.setOnClickListener(v -> {
            currentTab = tab;
            selectedCustomServiceId = "";
            render();
        });
        navButtons.add(button);
        return button;
    }

    private Button serviceNavButton(CampusService service) {
        Button button = chipButton(service.name);
        button.setTag(service.id);
        button.setTextSize(10);
        button.setPadding(dp(3), dp(2), dp(3), 0);
        setTopIcon(button, serviceIconForId(service.id), 20);
        button.setOnClickListener(v -> openService(service.id));
        navButtons.add(button);
        return button;
    }

    private void setTopIcon(Button button, int drawableRes, int sizeDp) {
        Drawable icon = getResources().getDrawable(drawableRes);
        icon.setBounds(0, 0, dp(sizeDp), dp(sizeDp));
        button.setCompoundDrawables(null, icon, null, null);
        button.setCompoundDrawablePadding(0);
    }

    private void setLeftIcon(Button button, int drawableRes, int sizeDp) {
        Drawable icon = getResources().getDrawable(drawableRes);
        icon.setBounds(0, 0, dp(sizeDp), dp(sizeDp));
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding(dp(4));
    }

    private int navIconForTab(int tab) {
        if (tab == TAB_SCHEDULE) {
            return R.drawable.nav_schedule;
        }
        if (tab == TAB_FOOD) {
            return R.drawable.nav_food;
        }
        if (tab == TAB_CLASSROOM) {
            return R.drawable.nav_classroom;
        }
        if (tab == TAB_TASKS) {
            return R.drawable.nav_tasks;
        }
        if (tab == TAB_FORUM) {
            return R.drawable.nav_forum;
        }
        if (tab == TAB_MARKET) {
            return R.drawable.nav_market;
        }
        if (tab == TAB_MINE) {
            return R.drawable.nav_mine;
        }
        return R.drawable.nav_home;
    }

    private int serviceIconForId(String id) {
        if ("schedule".equals(id)) {
            return R.drawable.nav_schedule;
        }
        if ("food".equals(id)) {
            return R.drawable.nav_food;
        }
        if ("classroom".equals(id) || "library".equals(id)) {
            return R.drawable.nav_classroom;
        }
        if ("tasks".equals(id) || "repair".equals(id)) {
            return R.drawable.nav_tasks;
        }
        if ("forum".equals(id)) {
            return R.drawable.nav_forum;
        }
        if ("market".equals(id)) {
            return R.drawable.nav_market;
        }
        if (id != null && id.startsWith("custom_")) {
            return R.drawable.nav_search;
        }
        return R.drawable.nav_home;
    }

    private int headerImageForService(String id) {
        if ("schedule".equals(id)) {
            return R.drawable.anime_schedule_header;
        }
        if ("food".equals(id)) {
            return R.drawable.anime_food_header;
        }
        if ("classroom".equals(id) || "library".equals(id)) {
            return R.drawable.anime_classroom_header;
        }
        if ("tasks".equals(id) || "repair".equals(id)) {
            return R.drawable.anime_tasks_header;
        }
        if ("forum".equals(id)) {
            return R.drawable.anime_forum_header;
        }
        if ("market".equals(id)) {
            return R.drawable.anime_market_header;
        }
        if (id != null && id.startsWith("custom_")) {
            return R.drawable.anime_mine_header;
        }
        return R.drawable.anime_home_header;
    }

    private void render() {
        content.removeAllViews();
        animationIndex = 0;
        updateNavButtons();
        if (currentTab == TAB_HOME) {
            titleView.setText("北信科有品");
            subtitleView.setText("一个入口，装下课程、外卖、教室和提醒");
            renderHome();
        } else if (currentTab == TAB_SCHEDULE) {
            titleView.setText("课程表");
            subtitleView.setText("第 " + currentWeek + " 周 · " + repository.getCourses().size() + " 门课程");
            renderSchedule();
        } else if (currentTab == TAB_FOOD) {
            titleView.setText("校园外卖");
            subtitleView.setText("校园商家、购物车、下单和商家接单");
            renderFood();
        } else if (currentTab == TAB_CLASSROOM) {
            titleView.setText("教室预约");
            subtitleView.setText("选择空闲教室和时间段");
            renderClassroom();
        } else if (currentTab == TAB_TASKS) {
            titleView.setText("待办提醒");
            subtitleView.setText("未完成 " + repository.countUnfinished() + " 项");
            renderTasks();
        } else if (currentTab == TAB_FORUM) {
            titleView.setText("校园论坛");
            subtitleView.setText("发布帖子，交流校园生活");
            renderForum();
        } else if (currentTab == TAB_MARKET) {
            titleView.setText("闲置交易");
            subtitleView.setText("发布闲置物品，校内自助联系");
            renderMarketplace();
        } else if (currentTab == TAB_CUSTOM_SERVICE) {
            renderCustomService();
        } else {
            titleView.setText("我的");
            subtitleView.setText("订单、预约、数据概览");
            renderMine();
        }
    }

    private void updateNavButtons() {
        for (Button button : navButtons) {
            Object tag = button.getTag();
            boolean selected = tag instanceof Integer && ((Integer) tag) == currentTab;
            if (tag instanceof String) {
                String id = (String) tag;
                selected = ("forum".equals(id) && currentTab == TAB_FORUM)
                        || ("market".equals(id) && currentTab == TAB_MARKET)
                        || (id.equals(selectedCustomServiceId) && currentTab == TAB_CUSTOM_SERVICE);
            }
            button.setTextColor(selected ? Color.WHITE : Color.rgb(94, 106, 126));
            button.setBackground(selected
                    ? gradient(COLOR_BRAND_DEEP, COLOR_BRAND_SOFT, dp(18), Color.argb(118, 255, 255, 255))
                    : rounded(Color.argb(22, 255, 255, 255), dp(18), Color.TRANSPARENT));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                button.setElevation(selected ? dp(3) : 0);
            }
            button.animate()
                    .scaleX(selected ? 1.035f : 1f)
                    .scaleY(selected ? 1.035f : 1f)
                    .translationY(selected ? -dp(1) : 0)
                    .alpha(selected ? 1f : 0.84f)
                    .setDuration(220)
                    .setInterpolator(new OvershootInterpolator(1.18f))
                    .start();
        }
    }

    private void renderHome() {
        List<Course> homeCourses = repository.getCourses();
        List<CampusService> homeServices = repository.getHomeServices();
        int unfinishedCount = repository.countUnfinished();
        int foodCount = activeFoodCount();
        content.addView(homeHero(homeCourses.size(), foodCount, repository.getUserReservations(currentUsername).size(), unfinishedCount), marginBottom(-1, -2, dp(8)));
        if (unfinishedCount > 0) {
            content.addView(homeTodoStrip(repository.getTasks(), unfinishedCount), marginBottom(-1, -2, dp(8)));
        }

        LinearLayout searchCard = horizontal();
        searchCard.setPadding(dp(10), dp(8), dp(8), dp(8));
        searchCard.setBackground(gradient(Color.argb(226, 255, 255, 255), Color.argb(176, 234, 246, 255),
                dp(24), Color.argb(150, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchCard.setElevation(dp(3));
        }
        EditText search = input("搜索服务、应用、资讯", "");
        search.setSingleLine(true);
        searchCard.addView(search, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button searchButton = primaryButton("搜索");
        setLeftIcon(searchButton, R.drawable.nav_search, 19);
        searchButton.setOnClickListener(v -> renderServiceResults(search.getText().toString()));
        LinearLayout.LayoutParams searchButtonParams = new LinearLayout.LayoutParams(dp(94), dp(46));
        searchButtonParams.setMargins(dp(8), 0, 0, 0);
        searchCard.addView(searchButton, searchButtonParams);
        content.addView(searchCard, marginBottom(-1, -2, dp(10)));

        addSectionHeader("常用服务", repository.isAdmin(currentUsername) ? "编辑" : "");
        View lastHeaderAction = content.getChildAt(content.getChildCount() - 1);
        if (repository.isAdmin(currentUsername)) {
            lastHeaderAction.setOnClickListener(v -> showCustomServiceDialog());
        }
        addServiceGrid(homeServices, false);
    }

    private View homeHero(int courseCount, int foodCount, int reservationCount, int unfinishedCount) {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(gradient(COLOR_BRAND_DEEP, COLOR_CYAN, dp(30)));
        hero.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hero.setElevation(dp(6));
        }

        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.anime_home_header);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setAlpha(0.74f);
        hero.addView(image, new FrameLayout.LayoutParams(-1, -1));

        View veil = new View(this);
        veil.setBackground(gradient(Color.argb(148, 0, 82, 217), Color.argb(24, 255, 255, 255), dp(30)));
        hero.addView(veil, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout copy = vertical();
        copy.setPadding(dp(15), dp(10), dp(15), dp(9));
        copy.addView(text("北信科有品", 24, Color.WHITE, true));
        TextView sub = text("北京信息科技大学 · 一站式校园服务", 13, Color.argb(235, 255, 255, 255), false);
        sub.setPadding(0, dp(2), 0, dp(7));
        copy.addView(sub);
        copy.addView(flowMetrics(courseCount, foodCount, reservationCount, unfinishedCount), marginTop(-1, -2, dp(7)));
        hero.addView(copy, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(136));
        hero.setLayoutParams(params);
        animateIn(hero);
        return hero;
    }

    private View pageHero(String title, String subtitle, int imageRes, int startColor, int endColor, String chipText) {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(gradient(startColor, endColor, dp(24)));
        hero.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hero.setElevation(dp(3));
        }

        ImageView art = new ImageView(this);
        art.setImageResource(imageRes);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setAlpha(0.76f);
        hero.addView(art, new FrameLayout.LayoutParams(-1, -1));

        View veil = new View(this);
        veil.setBackground(gradient(Color.argb(150, 0, 82, 217), Color.argb(24, 255, 255, 255), dp(24)));
        hero.addView(veil, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout copy = vertical();
        copy.setPadding(dp(13), dp(8), dp(72), dp(7));
        TextView titleView = text(title, 19, Color.WHITE, true);
        titleView.setPadding(0, 0, 0, dp(1));
        copy.addView(titleView);
        TextView sub = text(subtitle, 11, Color.argb(226, 255, 255, 255), false);
        sub.setMaxLines(2);
        copy.addView(sub);
        hero.addView(copy, new FrameLayout.LayoutParams(-1, -1));

        TextView chip = text(chipText, 12, Color.WHITE, true);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(rounded(Color.argb(56, 255, 255, 255), dp(999), Color.argb(98, 255, 255, 255)));
        FrameLayout.LayoutParams chipParams = new FrameLayout.LayoutParams(dp(56), dp(24),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        chipParams.setMargins(0, 0, dp(12), 0);
        hero.addView(chip, chipParams);

        hero.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(78)));
        animateIn(hero);
        return hero;
    }

    private View schoolLogoBadge(int size, boolean light) {
        FrameLayout badge = new FrameLayout(this);
        int fill = light ? Color.argb(232, 255, 255, 255) : COLOR_BRAND_MIST;
        int stroke = light ? Color.argb(190, 255, 255, 255) : COLOR_BRAND_SOFT;
        badge.setBackground(rounded(fill, size / 2, stroke));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            badge.setElevation(dp(8));
        }

        ImageView emblem = new ImageView(this);
        emblem.setImageResource(R.drawable.bistu_emblem);
        emblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams emblemParams = new FrameLayout.LayoutParams(size - dp(14), size - dp(14), Gravity.CENTER);
        badge.addView(emblem, emblemParams);
        badge.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return badge;
    }

    private View iconBubble(int drawableRes, int sizeDp, int startColor, int endColor) {
        FrameLayout bubble = new FrameLayout(this);
        bubble.setBackground(gradient(startColor, endColor, dp(Math.max(16, sizeDp / 2)),
                Color.argb(110, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bubble.setElevation(dp(3));
        }
        ImageView image = new ImageView(this);
        image.setImageResource(drawableRes);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int inner = dp(Math.max(28, sizeDp - 12));
        bubble.addView(image, new FrameLayout.LayoutParams(inner, inner, Gravity.CENTER));
        return bubble;
    }

    private LinearLayout flowMetrics() {
        return flowMetrics(repository.getCourses().size(),
                activeFoodCount(),
                repository.getUserReservations(currentUsername).size(),
                repository.countUnfinished());
    }

    private int activeFoodCount() {
        int count = cart.size();
        for (FoodOrder order : repository.getFoodOrders()) {
            if (!FoodOrderRules.STATUS_COMPLETED.equals(order.status)
                    && !FoodOrderRules.STATUS_CANCELED.equals(order.status)) {
                count++;
            }
        }
        return count;
    }

    private LinearLayout flowMetrics(int courseCount, int foodCount, int reservationCount, int unfinishedCount) {
        LinearLayout row = horizontal();
        row.addView(metricPill("课程", courseCount + " 门", TAB_SCHEDULE));
        row.addView(metricPill("外卖", foodCount + " 件", TAB_FOOD));
        row.addView(metricPill("预约", reservationCount + " 个", TAB_CLASSROOM));
        row.addView(metricPill("待办", unfinishedCount + " 项", TAB_TASKS));
        return row;
    }

    private TextView metricPill(String label, String value, int targetTab) {
        TextView pill = text(label + "\n" + value, 11, Color.WHITE, true);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(gradient(Color.argb(58, 255, 255, 255), Color.argb(28, 255, 255, 255),
                dp(14), Color.argb(88, 255, 255, 255)));
        pill.setPadding(dp(4), dp(4), dp(4), dp(4));
        pill.setOnClickListener(v -> {
            currentTab = targetTab;
            selectedCustomServiceId = "";
            render();
        });
        addPressFeedback(pill);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        pill.setLayoutParams(params);
        return pill;
    }

    private View homeTodoStrip(List<TaskItem> tasks, int unfinishedCount) {
        String firstTitle = "查看待办提醒";
        for (TaskItem task : tasks) {
            if (!task.completed) {
                firstTitle = task.title;
                break;
            }
        }
        LinearLayout strip = horizontal();
        strip.setPadding(dp(10), dp(8), dp(12), dp(8));
        strip.setBackground(gradient(Color.argb(226, 255, 255, 255), Color.argb(160, 255, 240, 250),
                dp(22), Color.argb(145, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            strip.setElevation(dp(2));
        }
        strip.addView(iconBubble(R.drawable.nav_tasks, 36, 0xFF7C3AED, COLOR_BRAND),
                new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout copy = vertical();
        copy.setPadding(dp(10), 0, 0, 0);
        TextView title = text(firstTitle, 15, COLOR_TEXT, true);
        title.setSingleLine(true);
        copy.addView(title);
        copy.addView(text("还有 " + unfinishedCount + " 项未完成，点击进入处理", 12, COLOR_MUTED, false));
        strip.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        strip.setOnClickListener(v -> {
            currentTab = TAB_TASKS;
            selectedCustomServiceId = "";
            render();
        });
        addPressFeedback(strip);
        animateIn(strip);
        return strip;
    }

    private View insightMosaic(int courseCount, int unfinishedCount) {
        LinearLayout panel = vertical();
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(gradient(Color.argb(216, 255, 255, 255), Color.argb(150, 232, 247, 255),
                dp(26), Color.argb(126, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            panel.setElevation(dp(2));
        }

        LinearLayout top = horizontal();
        top.addView(infoTile("课程", "第 " + currentWeek + " 周", courseCount + " 门", COLOR_BRAND), weightParams());
        top.addView(infoTile("待办", "需要处理", unfinishedCount + " 项", 0xFF7C3AED), weightParams());
        panel.addView(top);

        TextView rhythm = text("校园节奏  课程、外卖、教室、待办在一个入口里联动，服务越用越顺手。", 13, COLOR_TEXT, true);
        rhythm.setPadding(dp(12), dp(10), dp(12), dp(10));
        rhythm.setBackground(gradient(Color.argb(176, 248, 251, 255), Color.argb(92, 255, 255, 255),
                dp(18), Color.argb(120, 255, 255, 255)));
        panel.addView(rhythm, marginTop(-1, -2, dp(10)));
        animateIn(panel);
        return panel;
    }

    private View infoTile(String title, String label, String value, int color) {
        LinearLayout tile = vertical();
        tile.setPadding(dp(12), dp(10), dp(12), dp(10));
        tile.setBackground(gradient(Color.argb(216, 255, 255, 255), blend(color, Color.WHITE, 0.86f),
                dp(20), blend(color, Color.WHITE, 0.62f)));
        TextView titleView = text(title, 13, color, true);
        tile.addView(titleView);
        TextView valueView = text(value, 22, COLOR_TEXT, true);
        valueView.setPadding(0, dp(6), 0, dp(3));
        tile.addView(valueView);
        tile.addView(text(label, 12, COLOR_MUTED, false));
        return tile;
    }

    private void renderServiceResults(String query) {
        content.removeAllViews();
        animationIndex = 0;
        updateNavButtons();
        titleView.setText("服务搜索");
        subtitleView.setText("关键词：" + (query == null || query.trim().isEmpty() ? "全部" : query.trim()));
        content.addView(pageHero("服务搜索", "关键词：" + (query == null || query.trim().isEmpty() ? "全部" : query.trim()),
                R.drawable.anime_home_header, COLOR_BRAND_DEEP, COLOR_BRAND_SOFT, "搜索"), marginBottom(-1, -2, dp(10)));
        addServiceGrid(ServiceCatalog.search(repository.getHomeServices(), query), false);
    }

    private void addServiceGrid(List<CampusService> services, boolean pinnedOnly) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(serviceColumnCount());
        int visibleIndex = 0;
        for (CampusService service : services) {
            if (pinnedOnly && !service.pinned) {
                continue;
            }
            boolean featured = visibleIndex == 0;
            grid.addView(serviceCard(service, featured), gridParams(featured));
            visibleIndex++;
        }
        content.addView(grid);
    }

    private View serviceCard(CampusService service, boolean featured) {
        LinearLayout card = featured ? horizontal() : vertical();
        card.setGravity(featured ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL);
        card.setPadding(featured ? dp(12) : dp(9), featured ? dp(8) : dp(9),
                featured ? dp(12) : dp(9), featured ? dp(8) : dp(9));
        card.setMinimumHeight(dp(featured ? 78 : 98));
        card.setBackground(gradient(Color.argb(220, 255, 255, 255), blend(service.color, Color.WHITE, 0.86f),
                dp(featured ? 28 : 24), blend(service.color, Color.WHITE, 0.58f)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(featured ? dp(4) : dp(2));
        }

        View icon = iconBubble(serviceIconForId(service.id), featured ? 52 : 44, service.color,
                blend(service.color, COLOR_BRAND_SOFT, 0.32f));
        card.addView(icon, new LinearLayout.LayoutParams(dp(featured ? 56 : 48), dp(featured ? 56 : 48)));

        LinearLayout copy = vertical();
        copy.setGravity(featured ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL);
        copy.setPadding(featured ? dp(13) : 0, featured ? 0 : dp(7), 0, 0);
        TextView name = text(service.name, featured ? 16 : 15, COLOR_TEXT, true);
        name.setGravity(featured ? Gravity.LEFT : Gravity.CENTER);
        copy.addView(name);
        TextView category = text(service.category, 12, COLOR_MUTED, false);
        category.setGravity(featured ? Gravity.LEFT : Gravity.CENTER);
        category.setSingleLine(true);
        category.setPadding(0, dp(2), 0, 0);
        copy.addView(category);
        if (featured) {
            TextView hint = text(service.description, 12, Color.rgb(72, 84, 101), false);
            hint.setMaxLines(2);
            hint.setPadding(0, dp(3), 0, 0);
            copy.addView(hint);
        }
        if (featured) {
            card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        } else {
            card.addView(copy, new LinearLayout.LayoutParams(-1, -2));
        }
        if (CommunityRules.isCustomServiceId(service.id)) {
            Button delete = smallButton("删除");
            delete.setOnClickListener(v -> deleteCustomService(service));
            card.addView(delete, marginTop(-1, -2, dp(8)));
        }
        card.setOnClickListener(v -> openService(service.id));
        addPressFeedback(card);
        animateIn(card);
        return card;
    }

    private void openService(String id) {
        selectedCustomServiceId = "";
        if ("schedule".equals(id)) {
            currentTab = TAB_SCHEDULE;
        } else if ("food".equals(id)) {
            currentTab = TAB_FOOD;
        } else if ("classroom".equals(id)) {
            currentTab = TAB_CLASSROOM;
        } else if ("tasks".equals(id)) {
            currentTab = TAB_TASKS;
        } else if ("forum".equals(id)) {
            currentTab = TAB_FORUM;
        } else if ("market".equals(id)) {
            currentTab = TAB_MARKET;
        } else if (id != null && id.startsWith("custom_")) {
            selectedCustomServiceId = id;
            currentTab = TAB_CUSTOM_SERVICE;
        } else {
            toast("该服务是预留入口，可继续扩展");
            return;
        }
        render();
    }

    private void renderCustomService() {
        CampusService service = repository.getCustomService(selectedCustomServiceId);
        if (service == null) {
            currentTab = TAB_HOME;
            selectedCustomServiceId = "";
            toast("服务不存在或已删除");
            render();
            return;
        }
        titleView.setText(service.name);
        subtitleView.setText(service.category + " · 自定义服务");
        content.addView(pageHero(service.name, service.category + " · 自定义服务", headerImageForService(service.id),
                service.color, blend(service.color, COLOR_BRAND, 0.35f), "自定义"), marginBottom(-1, -2, dp(10)));
        LinearLayout detail = card();
        detail.addView(iconBubble(serviceIconForId(service.id), 56, service.color,
                blend(service.color, COLOR_BRAND_SOFT, 0.28f)), new LinearLayout.LayoutParams(dp(58), dp(58)));
        TextView name = text(service.name, 22, COLOR_TEXT, true);
        name.setPadding(0, dp(10), 0, dp(4));
        detail.addView(name);
        detail.addView(text(service.description, 15, Color.rgb(51, 65, 85), false));
        detail.addView(text("这个入口已加入首页和底部导航，可后续继续接入真实功能。", 13, COLOR_MUTED, false), marginTop(-1, -2, dp(10)));
        content.addView(detail);

        LinearLayout actions = horizontal();
        Button back = smallButton("返回首页");
        back.setOnClickListener(v -> {
            currentTab = TAB_HOME;
            selectedCustomServiceId = "";
            render();
        });
        Button delete = primaryButton("删除服务");
        delete.setOnClickListener(v -> deleteCustomService(service));
        actions.addView(back, weightParams());
        actions.addView(delete, weightParams());
        content.addView(actions, marginBottom(-1, -2, dp(10)));
    }

    private void deleteCustomService(CampusService service) {
        confirm("删除服务", "确定删除“" + service.name + "”吗？", () -> {
            if (repository.deleteCustomService(service.id)) {
                toast("服务已删除");
            }
            currentTab = TAB_HOME;
            selectedCustomServiceId = "";
            buildShell();
            render();
        });
    }

    private void renderForum() {
        if (selectedForumPostId > 0) {
            renderForumPostDetail();
        } else if (selectedForumSectionId > 0) {
            renderForumPosts();
        } else {
            renderForumSections();
        }
    }

    private void renderForumSections() {
        content.addView(pageHero("校园论坛", "选择版块，参与校园交流", R.drawable.anime_forum_header,
                0xFF06B6D4, 0xFF7C3AED, "社区"), marginBottom(-1, -2, dp(10)));
        boolean isAdmin = repository.isAdmin(currentUsername);
        if (isAdmin) {
            LinearLayout actions = horizontal();
            Button createSection = primaryButton("创建版块");
            createSection.setOnClickListener(v -> showCreateSectionDialog());
            actions.addView(createSection, weightParams());
            content.addView(actions, marginBottom(-1, -2, dp(10)));
        }
        addSectionTitle("论坛版块");
        List<String[]> sections;
        try {
            sections = repository.getForumSections();
        } catch (Exception e) {
            sections = new ArrayList<>();
        }
        if (sections.isEmpty()) {
            addEmpty(isAdmin ? "还没有版块，点击\"创建版块\"添加。" : "管理员尚未创建任何版块。");
        } else {
            for (String[] section : sections) {
                content.addView(forumSectionCard(Long.parseLong(section[0]), section[1], section[2], Integer.parseInt(section[3]), isAdmin));
            }
        }
    }

    private View forumSectionCard(long id, String name, String description, int postCount, boolean isAdmin) {
        LinearLayout card = horizontal();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(gradient(0xFFFFFFFF, 0xFFF0FDFA, dp(24), 0xFFCCFBF1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout iconBox = vertical();
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackground(gradient(COLOR_CYAN, COLOR_BRAND, dp(18)));
        TextView icon = text(name.substring(0, 1), 22, Color.WHITE, true);
        icon.setGravity(Gravity.CENTER);
        iconBox.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));
        card.addView(iconBox, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout body = vertical();
        body.setPadding(dp(14), 0, 0, 0);
        body.addView(text(name, 18, COLOR_TEXT, true));
        if (description != null && !description.isEmpty()) {
            TextView desc = text(description, 13, COLOR_MUTED, false);
            desc.setPadding(0, dp(4), 0, 0);
            body.addView(desc);
        }
        body.addView(text(postCount + " 帖", 12, COLOR_BRAND, true), marginTop(-1, -2, dp(4)));
        card.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        card.setOnClickListener(v -> {
            selectedForumSectionId = id;
            render();
        });
        if (isAdmin) {
            Button del = smallButton("删除");
            del.setOnClickListener(v -> confirm("删除版块", "删除\"" + name + "\"及其所有帖子？", () -> {
                repository.deleteForumSection(id);
                toast("版块已删除");
                render();
            }));
            card.addView(del, new LinearLayout.LayoutParams(dp(60), dp(40)));
        }
        animateIn(card);
        return card;
    }

    private void showCreateSectionDialog() {
        LinearLayout form = dialogForm();
        form.addView(label("版块名称"));
        EditText name = input("例如：求助", "");
        form.addView(name);
        form.addView(label("版块描述"));
        EditText desc = input("例如：遇到问题在这里求助", "");
        desc.setMinLines(2);
        form.addView(desc);
        new AlertDialog.Builder(this)
                .setTitle("创建版块")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", (d, w) -> {
                    long id = repository.createForumSection(name.getText().toString(), desc.getText().toString());
                    if (id <= 0) {
                        toast("版块名称不能为空");
                        return;
                    }
                    toast("版块已创建");
                    render();
                })
                .show();
    }

    private void renderForumPosts() {
        List<String[]> sections = repository.getForumSections();
        String sectionName = "";
        for (String[] s : sections) {
            if (Long.parseLong(s[0]) == selectedForumSectionId) {
                sectionName = s[1];
                break;
            }
        }
        content.addView(pageHero(sectionName, "浏览帖子或发布新帖", R.drawable.anime_forum_header,
                0xFF06B6D4, 0xFF7C3AED, "社区"), marginBottom(-1, -2, dp(10)));
        LinearLayout actions = horizontal();
        Button back = smallButton("返回版块");
        back.setOnClickListener(v -> {
            selectedForumSectionId = 0;
            render();
        });
        Button publish = primaryButton("发帖");
        publish.setOnClickListener(v -> {
            pendingAttachmentPath = "";
            showForumPostDialogV2(selectedForumSectionId);
        });
        actions.addView(back, weightParams());
        actions.addView(publish, weightParams());
        content.addView(actions, marginBottom(-1, -2, dp(10)));

        List<String[]> posts;
        try {
            posts = repository.getForumPostsBySection(selectedForumSectionId);
        } catch (Exception e) {
            posts = new ArrayList<>();
        }
        if (posts.isEmpty()) {
            addEmpty("还没有帖子，发布第一条吧。");
            return;
        }
        for (String[] post : posts) {
            content.addView(forumPostListCard(
                    Long.parseLong(post[0]), post[1], post[2], post[3], post[4],
                    Long.parseLong(post[5])));
        }
    }

    private View forumPostListCard(long id, String title, String content, String author,
                                    String attachment, long createdAt) {
        LinearLayout card = vertical();
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(gradient(0xFFFFFFFF, 0xFFF0FDFA, dp(24), 0xFFCCFBF1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout titleRow = horizontal();
        titleRow.addView(createAvatarView(author, 42), new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout copy = vertical();
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(title, 18, COLOR_TEXT, true));
        TextView meta = text(author + " \u00b7 " + dateFormat.format(new Date(createdAt)), 12, COLOR_MUTED, false);
        meta.setPadding(0, dp(4), 0, dp(8));
        copy.addView(meta);
        titleRow.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(titleRow);
        TextView bubble = text(content, 14, Color.rgb(51, 65, 85), false);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        bubble.setBackground(rounded(Color.WHITE, dp(18), 0));
        bubble.setMaxLines(3);
        card.addView(bubble, marginTop(-1, -2, dp(8)));
        if (attachment != null && !attachment.isEmpty()) {
            ImageView attachImage = new ImageView(this);
            try {
                safeSetImageUri(attachImage, attachment);
            } catch (Exception e) {
                // ignore
            }
            attachImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            attachImage.setAdjustViewBounds(true);
            attachImage.setMaxHeight(dp(150));
            attachImage.setBackground(rounded(0xFFF3F4F6, dp(16), COLOR_LINE));
            card.addView(attachImage, new LinearLayout.LayoutParams(-1, dp(150)));
        }
        int commentCount = repository.getForumCommentsV2(id).size();
        TextView count = text(commentCount + " 回帖", 12, COLOR_MUTED, true);
        count.setPadding(0, dp(8), 0, 0);
        card.addView(count);
        card.setOnClickListener(v -> {
            selectedForumPostId = id;
            render();
        });
        animateIn(card);
        return card;
    }

    private void renderForumPostDetail() {
        List<String[]> posts = repository.getForumPostsBySection(selectedForumSectionId);
        String[] targetPost = null;
        for (String[] p : posts) {
            if (Long.parseLong(p[0]) == selectedForumPostId) {
                targetPost = p;
                break;
            }
        }
        if (targetPost == null) {
            selectedForumPostId = 0;
            render();
            return;
        }
        long postId = Long.parseLong(targetPost[0]);
        String title = targetPost[1];
        String postContent = targetPost[2];
        String author = targetPost[3];
        String attachment = targetPost[4];
        long createdAt = Long.parseLong(targetPost[5]);
        boolean isAdmin = repository.isAdmin(currentUsername);

        content.addView(pageHero(title, author + " \u00b7 " + dateFormat.format(new Date(createdAt)),
                R.drawable.anime_forum_header, 0xFF06B6D4, 0xFF7C3AED, "帖子"), marginBottom(-1, -2, dp(10)));
        LinearLayout actions = horizontal();
        Button back = smallButton("返回列表");
        back.setOnClickListener(v -> {
            selectedForumPostId = 0;
            render();
        });
        actions.addView(back, weightParams());
        content.addView(actions, marginBottom(-1, -2, dp(10)));

        // 帖子正文
        content.addView(forumPostDetailCard(postId, title, postContent, author, attachment, createdAt, isAdmin));

        // 评论区
        addSectionTitle("回帖");
        List<String[]> comments = repository.getForumCommentsV2(postId);
        if (comments.isEmpty()) {
            addEmpty("还没有回帖，发表第一条评论吧。");
        } else {
            for (String[] comment : comments) {
                content.addView(commentLine(comment[2], comment[1], Long.parseLong(comment[3])));
            }
        }
        Button commentBtn = primaryButton("发表回帖");
        commentBtn.setOnClickListener(v -> showForumCommentDialogV2(postId));
        content.addView(commentBtn, marginTop(-1, -2, dp(8)));
    }

    private View forumPostDetailCard(long id, String title, String content, String author,
                                      String attachment, long createdAt, boolean isAdmin) {
        LinearLayout card = vertical();
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(gradient(0xFFFFFFFF, 0xFFF0FDFA, dp(24), 0xFFCCFBF1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout titleRow = horizontal();
        titleRow.addView(createAvatarView(author, 42), new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout copy = vertical();
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(title, 18, COLOR_TEXT, true));
        TextView meta = text(author + " \u00b7 " + dateFormat.format(new Date(createdAt)), 12, COLOR_MUTED, false);
        meta.setPadding(0, dp(4), 0, dp(8));
        copy.addView(meta);
        titleRow.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(titleRow);
        TextView bubble = text(content, 14, Color.rgb(51, 65, 85), false);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        bubble.setBackground(rounded(Color.WHITE, dp(18), 0));
        card.addView(bubble, marginTop(-1, -2, dp(8)));
        if (attachment != null && !attachment.isEmpty()) {
            LinearLayout attachBox = vertical();
            attachBox.setPadding(dp(12), dp(10), dp(12), dp(10));
            attachBox.setBackground(rounded(COLOR_BRAND_MIST, dp(16), 0));
            TextView attachLabel = text("[图片]", 13, COLOR_BRAND, true);
            attachBox.addView(attachLabel);
            try {
                ImageView img = new ImageView(this);
                safeSetImageUri(img, attachment);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setAdjustViewBounds(true);
                img.setMaxHeight(dp(200));
                attachBox.addView(img, new LinearLayout.LayoutParams(-1, -2));
            } catch (Exception e) {
                attachBox.addView(text("图片路径：" + attachment, 12, COLOR_MUTED, false));
            }
            card.addView(attachBox, marginTop(-1, -2, dp(6)));
        }
        if (isAdmin || CommunityRules.isOwner(author, currentUsername)) {
            Button delete = smallButton(isAdmin && !CommunityRules.isOwner(author, currentUsername) ? "管理员删除" : "删除帖子");
            delete.setOnClickListener(v -> confirm("删除帖子", "确定删除这条帖子？", () -> {
                if (repository.deleteForumPostV2(id, currentUsername, isAdmin)) {
                    toast("帖子已删除");
                    selectedForumPostId = 0;
                    render();
                } else {
                    toast("删除失败");
                }
            }));
            card.addView(delete, marginTop(-1, -2, dp(6)));
        }
        animateIn(card);
        return card;
    }

    private void showForumPostDialogV2(long sectionId) {
        LinearLayout form = dialogForm();
        form.addView(label("帖子标题"));
        EditText title = input("标题", "");
        form.addView(title);
        form.addView(label("帖子内容"));
        EditText contentText = input("写下你想交流的内容", "");
        contentText.setMinLines(5);
        contentText.setGravity(Gravity.TOP | Gravity.START);
        form.addView(contentText);
        form.addView(label("添加图片（选填）"));
        LinearLayout imageRow = horizontal();
        Button pickImage = smallButton("选择图片");
        pickImage.setOnClickListener(v -> {
            Intent imgIntent = new Intent(Intent.ACTION_GET_CONTENT);
            imgIntent.setType("image/*");
            imgIntent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(imgIntent, "选择图片"), REQUEST_CODE_PICK_FORUM_IMAGE);
            } catch (Exception e) {
                toast("无法打开图片选择器");
            }
        });
        imageRow.addView(pickImage, weightParams());
        form.addView(imageRow);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(form);
        new AlertDialog.Builder(this)
                .setTitle("发布帖子")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("发布", (d, w) -> {
                    long id = repository.createForumPostV2(sectionId,
                            title.getText().toString(),
                            contentText.getText().toString(),
                            currentUsername,
                            pendingAttachmentPath);
                    if (id <= 0) {
                        toast("标题和内容都要填写");
                        return;
                    }
                    pendingAttachmentPath = "";
                    toast("帖子已发布");
                    render();
                })
                .show();
    }

    private void showForumCommentDialogV2(long postId) {
        LinearLayout form = dialogForm();
        form.addView(label("评论内容"));
        EditText comment = input("写下你的看法", "");
        comment.setMinLines(3);
        comment.setGravity(Gravity.TOP | Gravity.START);
        form.addView(comment);
        new AlertDialog.Builder(this)
                .setTitle("评论帖子")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("发布", (d, w) -> {
                    long id = repository.createForumCommentV2(postId, comment.getText().toString(), currentUsername);
                    if (id <= 0) {
                        toast("评论内容不能为空");
                        return;
                    }
                    toast("评论已发布");
                    render();
                })
                .show();
    }

    private void renderMarketplace() {
        if (selectedMarketItemId > 0) {
            renderMarketplaceDetail();
        } else {
            renderMarketplaceList();
        }
    }

    private void renderMarketplaceList() {
        content.addView(pageHero("闲置交易", "发布闲置物品，校内自助联系", R.drawable.anime_market_header,
                0xFFF97316, 0xFF1677FF, "市场"), marginBottom(-1, -2, dp(10)));
        LinearLayout actions = horizontal();
        Button publish = primaryButton("发布闲置");
        publish.setOnClickListener(v -> showMarketplaceItemDialog());
        actions.addView(publish, weightParams());
        content.addView(actions, marginBottom(-1, -2, dp(10)));

        addSectionTitle("闲置物品");
        List<MarketplaceItem> items;
        try {
            items = repository.getMarketplaceItems();
        } catch (Exception e) {
            items = new ArrayList<>();
        }
        if (items.isEmpty()) {
            addEmpty("还没有闲置物品，发布你的第一件闲置吧。");
            return;
        }
        for (MarketplaceItem item : items) {
            content.addView(marketplaceListCard(item));
        }
    }

    private View marketplaceListCard(MarketplaceItem item) {
        LinearLayout card = vertical();
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(gradient(0xFFFFFFFF, 0xFFFFF7ED, dp(24), 0xFFFED7AA));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout head = horizontal();
        head.addView(text(item.name, 18, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView price = text(item.currentPrice + " 元", 16, Color.rgb(234, 88, 12), true);
        price.setGravity(Gravity.CENTER);
        price.setBackground(rounded(0xFFFFEDD5, dp(999), 0));
        head.addView(price, new LinearLayout.LayoutParams(dp(92), dp(34)));
        card.addView(head);
        // 物品图片
        if (item.imageUri != null && !item.imageUri.isEmpty()) {
            ImageView itemImage = new ImageView(this);
            safeSetImageUri(itemImage, item.imageUri);
            itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            itemImage.setAdjustViewBounds(true);
            itemImage.setMaxHeight(dp(160));
            itemImage.setBackground(rounded(0xFFF3F4F6, dp(16), COLOR_LINE));
            card.addView(itemImage, new LinearLayout.LayoutParams(-1, dp(160)));
        }
        TextView meta = text(item.seller + " · " + dateFormat.format(new Date(item.createdAt)), 12, COLOR_MUTED, false);
        meta.setPadding(0, dp(4), 0, dp(4));
        card.addView(text(item.description, 14, Color.rgb(51, 65, 85), false));
        TextView auction = text("出价 " + item.bidCount + " 次" + (item.sold ? " · 已售出" : ""), 13, item.sold ? COLOR_SUCCESS : COLOR_MUTED, true);
        auction.setPadding(0, dp(6), 0, 0);
        card.addView(auction);
        card.setOnClickListener(v -> {
            selectedMarketItemId = item.id;
            render();
        });
        animateIn(card);
        return card;
    }

    private void renderMarketplaceDetail() {
        List<MarketplaceItem> items = repository.getMarketplaceItems();
        MarketplaceItem target = null;
        for (MarketplaceItem item : items) {
            if (item.id == selectedMarketItemId) {
                target = item;
                break;
            }
        }
        if (target == null) {
            selectedMarketItemId = 0;
            render();
            return;
        }
        content.addView(pageHero(target.name, target.currentPrice + " 元", R.drawable.anime_market_header,
                0xFFF97316, 0xFF1677FF, "闲置"), marginBottom(-1, -2, dp(10)));
        LinearLayout actions = horizontal();
        Button back = smallButton("返回列表");
        back.setOnClickListener(v -> {
            selectedMarketItemId = 0;
            render();
        });
        actions.addView(back, weightParams());
        content.addView(actions, marginBottom(-1, -2, dp(10)));
        content.addView(marketplaceItemCard(target));
    }

    private View marketplaceItemCard(MarketplaceItem item) {
        LinearLayout card = vertical();
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(gradient(0xFFFFFFFF, 0xFFFFF7ED, dp(24), 0xFFFED7AA));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout head = horizontal();
        head.addView(text(item.name, 18, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView price = text(item.currentPrice + " 元", 16, Color.rgb(234, 88, 12), true);
        price.setGravity(Gravity.CENTER);
        price.setBackground(rounded(0xFFFFEDD5, dp(999), 0));
        head.addView(price, new LinearLayout.LayoutParams(dp(92), dp(34)));
        card.addView(head);
        // 物品图片
        if (item.imageUri != null && !item.imageUri.isEmpty()) {
            ImageView itemImage = new ImageView(this);
            safeSetImageUri(itemImage, item.imageUri);
            itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            itemImage.setAdjustViewBounds(true);
            itemImage.setMaxHeight(dp(200));
            itemImage.setBackground(rounded(0xFFF3F4F6, dp(16), COLOR_LINE));
            card.addView(itemImage, new LinearLayout.LayoutParams(-1, dp(200)));
        }
        TextView meta = text(item.seller + " · " + dateFormat.format(new Date(item.createdAt)), 12, COLOR_MUTED, false);
        meta.setPadding(0, dp(4), 0, dp(8));
        card.addView(meta);
        card.addView(text(item.description, 14, Color.rgb(51, 65, 85), false));
        TextView auction = text("起拍价：" + item.price + " 元 · 出价 " + item.bidCount + " 次", 13, COLOR_MUTED, false);
        auction.setPadding(0, dp(8), 0, 0);
        card.addView(auction);
        if (item.sold) {
            TextView sold = text("已售出：买家 " + item.buyer + " · 成交 " + item.soldPrice
                    + " 元 · 平台费 " + item.platformFee + " 元 · 到账 " + item.sellerIncome + " 元",
                    13, COLOR_SUCCESS, true);
            sold.setPadding(0, dp(8), 0, 0);
            card.addView(sold);
        }
        TextView contact = text("联系方式：" + item.contact, 13, COLOR_BRAND, true);
        contact.setPadding(0, dp(8), 0, 0);
        card.addView(contact);
        List<MarketBid> bids = repository.getMarketBids(item.id);
        if (!bids.isEmpty()) {
            TextView bidTitle = text("当前出价", 12, COLOR_MUTED, true);
            bidTitle.setPadding(0, dp(10), 0, dp(4));
            card.addView(bidTitle);
            int shown = Math.min(3, bids.size());
            for (int i = 0; i < shown; i++) {
                MarketBid bid = bids.get(i);
                card.addView(text(bid.bidder + " · " + bid.price + " 元 · " + dateFormat.format(new Date(bid.createdAt)), 12, Color.rgb(51, 65, 85), false));
            }
        }
        List<MarketplaceComment> comments = repository.getMarketplaceComments(item.id);
        TextView count = text(comments.size() + " 条评论", 12, COLOR_MUTED, true);
        count.setPadding(0, dp(10), 0, dp(4));
        card.addView(count);
        for (MarketplaceComment comment : comments) {
            card.addView(commentLine(comment.author, comment.content, comment.createdAt));
        }
        LinearLayout actions = horizontal();
        Button comment = smallButton("评论");
        comment.setOnClickListener(v -> showMarketplaceCommentDialog(item));
        Button bid = primaryButton("出价");
        bid.setOnClickListener(v -> showMarketBidDialog(item));
        if (item.sold || CommunityRules.isOwner(item.seller, currentUsername)) {
            bid.setVisibility(View.GONE);
        }
        actions.addView(comment, weightParams());
        actions.addView(bid, weightParams());
        if (!item.sold && CommunityRules.isOwner(item.seller, currentUsername) && !bids.isEmpty()) {
            Button sell = primaryButton("确认售卖");
            sell.setOnClickListener(v -> showMarketplaceSettleDialog(item, bids));
            actions.addView(sell, weightParams());
        }
        card.addView(actions, marginTop(-1, -2, dp(8)));
        return card;
    }

    private void renderSchedule() {
        content.addView(pageHero("课程表", "第 " + currentWeek + " 周 · " + repository.getCourses().size() + " 门课程",
                R.drawable.anime_schedule_header, COLOR_BRAND_DEEP, COLOR_CYAN, "学习"), marginBottom(-1, -2, dp(10)));
        LinearLayout weekBar = horizontal();
        weekBar.setPadding(dp(12), dp(12), dp(12), dp(12));
        weekBar.setBackground(gradient(0xFFFFFFFF, 0xFFEAF7FF, dp(28), COLOR_LINE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            weekBar.setElevation(dp(3));
        }
        Button prev = smallButton("‹");
        Button next = smallButton("›");
        TextView week = text("第 " + currentWeek + " 周", 20, COLOR_TEXT, true);
        week.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> {
            currentWeek = Math.max(1, currentWeek - 1);
            courseReminderScheduler.setCurrentWeek(currentWeek);
            render();
        });
        next.setOnClickListener(v -> {
            currentWeek = Math.min(MAX_WEEK, currentWeek + 1);
            courseReminderScheduler.setCurrentWeek(currentWeek);
            render();
        });
        weekBar.addView(prev, new LinearLayout.LayoutParams(dp(54), -2));
        weekBar.addView(week, new LinearLayout.LayoutParams(0, -2, 1));
        weekBar.addView(next, new LinearLayout.LayoutParams(dp(54), -2));
        content.addView(weekBar, marginBottom(-1, -2, dp(10)));
        LinearLayout courseActions = horizontal();
        Button addCourse = primaryButton("添加课程");
        addCourse.setOnClickListener(v -> showCourseDialog(null));
        Button importCourse = smallButton("教务导入");
        importCourse.setOnClickListener(v -> showCourseImportDialog());
        courseActions.addView(addCourse, weightParams());
        courseActions.addView(importCourse, weightParams());
        content.addView(courseActions, marginBottom(-1, -2, dp(10)));

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        horizontalScrollView.setBackground(rounded(0xFFFFFFFF, dp(RADIUS_CARD), COLOR_LINE));
        horizontalScrollView.setPadding(dp(6), dp(6), dp(6), dp(6));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(8);
        grid.setRowCount(SECTION_COUNT + 1);
        buildScheduleGrid(grid);
        horizontalScrollView.addView(grid);
        content.addView(horizontalScrollView);
    }

    private void buildScheduleGrid(GridLayout grid) {
        int sectionWidth = dp(38);
        int dayWidth = dp(90);
        int rowHeight = dp(64);
        boolean[][] occupied = new boolean[SECTION_COUNT + 1][8];
        addGridCell(grid, "", 0, 0, 1, sectionWidth, dp(42), Color.TRANSPARENT, COLOR_MUTED, false);
        for (int day = 1; day <= 7; day++) {
            addGridCell(grid, ScheduleRules.weekdayLabel(day), 0, day, 1, dayWidth, dp(42), COLOR_SURFACE, COLOR_TEXT, true);
        }
        for (int section = 1; section <= SECTION_COUNT; section++) {
            addGridCell(grid, String.valueOf(section), section, 0, 1, sectionWidth, rowHeight, Color.TRANSPARENT, 0xFF94A3B8, true);
        }
        for (Course course : repository.getCourses()) {
            if (!ScheduleRules.isVisibleInWeek(course, currentWeek)) {
                continue;
            }
            int span = Math.min(ScheduleRules.sectionSpan(course), SECTION_COUNT - course.startSection + 1);
            addCourseBlock(grid, course, course.startSection, course.weekday, span, dayWidth, rowHeight);
            for (int i = 0; i < span; i++) {
                occupied[course.startSection + i][course.weekday] = true;
            }
        }
        for (int section = 1; section <= SECTION_COUNT; section++) {
            for (int day = 1; day <= 7; day++) {
                if (!occupied[section][day]) {
                    addGridCell(grid, "", section, day, 1, dayWidth, rowHeight, COLOR_SURFACE_TINT, 0xFFCBD5E1, false);
                }
            }
        }
    }

    private void addCourseBlock(GridLayout grid, Course course, int row, int column, int rowSpan, int width, int rowHeight) {
        LinearLayout block = vertical();
        block.setPadding(dp(8), dp(8), dp(8), dp(8));
        block.setGravity(Gravity.CENTER);
        block.setBackground(gradient(course.color, blend(course.color, Color.WHITE, 0.2f), dp(RADIUS_CONTROL)));
        TextView name = text(course.name, 12, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        TextView place = text(course.location, 10, Color.argb(230, 255, 255, 255), false);
        place.setGravity(Gravity.CENTER);
        place.setMaxLines(1);
        block.addView(name);
        block.addView(place);
        block.setOnClickListener(v -> showCourseDialog(course));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(row, rowSpan), GridLayout.spec(column));
        params.width = width;
        params.height = rowHeight * rowSpan;
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        grid.addView(block, params);
    }

    private void addGridCell(GridLayout grid, String value, int row, int column, int rowSpan, int width, int height, int fill, int textColor, boolean bold) {
        TextView cell = text(value, 12, textColor, bold);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(rounded(fill, dp(10), fill == Color.TRANSPARENT ? 0 : COLOR_LINE));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(row, rowSpan), GridLayout.spec(column));
        params.width = width;
        params.height = height * rowSpan;
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        grid.addView(cell, params);
    }

    private void renderFood() {
        content.addView(pageHero("校园外卖", "校园商家、购物车、模拟下单", R.drawable.anime_food_header,
                0xFFF97316, 0xFFEF4444, "生活"), marginBottom(-1, -2, dp(10)));
        addSectionTitle("学生点餐");
        for (Merchant merchant : repository.getMerchants()) {
            content.addView(merchantCard(merchant));
            for (Dish dish : repository.getDishesForMerchant(merchant.id)) {
                content.addView(foodCard(dish));
            }
        }
        addSectionTitle("购物车");
        content.addView(summaryCard("已选 " + cart.size() + " 件", cartSummary() + "\n合计 " + FoodOrderRules.total(cart) + " 元"));
        for (int i = 0; i < cart.size(); i++) {
            content.addView(cartItemCard(cart.get(i), i));
        }
        Button submit = primaryButton("模拟支付并下单");
        setLeftIcon(submit, R.drawable.nav_food, 18);
        submit.setOnClickListener(v -> {
            if (!FoodOrderRules.canSubmit(cart)) {
                toast("购物车为空");
                return;
            }
            int total = FoodOrderRules.total(cart);
            int balance = repository.getWalletBalance();
            if (total > balance) {
                toast("校园币不足，当前余额 " + balance + "，需要 " + total);
                return;
            }
            long orderId = repository.createFoodOrder(new ArrayList<>(cart));
            if (orderId <= 0) {
                toast("下单失败，请重试");
                return;
            }
            repository.addWalletTransaction(-total, "外卖支出：" + cartSummary());
            cart.clear();
            toast("支付成功，订单已提交");
            render();
        });
        content.addView(submit, marginBottom(-1, -2, dp(8)));

        addSectionTitle("订单");
        List<FoodOrder> foodOrders = repository.getFoodOrders();
        if (foodOrders.isEmpty()) {
            addEmpty("暂无订单");
        } else {
            for (FoodOrder order : foodOrders) {
                content.addView(paidOrderCard(order));
            }
        }
    }

    private View paidOrderCard(FoodOrder order) {
        LinearLayout card = horizontal();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(gradient(0xFFFFFFFF, 0xFFF0F9FF, dp(22), COLOR_LINE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout info = vertical();
        info.addView(text(order.merchantName, 16, COLOR_TEXT, true));
        TextView items = text(order.itemsSummary, 13, COLOR_MUTED, false);
        items.setPadding(0, dp(4), 0, dp(4));
        info.addView(items);
        info.addView(text("合计 " + order.totalPrice + " 元", 14, 0xFFEA580C, true));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = text("处理中", 12, Color.WHITE, true);
        status.setGravity(Gravity.CENTER);
        status.setBackground(rounded(COLOR_WARNING, dp(999), 0));
        card.addView(status, new LinearLayout.LayoutParams(dp(62), dp(30)));
        animateIn(card);
        return card;
    }

    private View merchantCard(Merchant merchant) {
        LinearLayout card = horizontal();
        card.setPadding(dp(12), dp(10), dp(14), dp(10));
        card.setBackground(gradient(Color.argb(226, 255, 247, 237), Color.argb(174, 255, 255, 255),
                dp(24), 0xFFFED7AA));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        card.addView(iconBubble(R.drawable.nav_food, 48, 0xFFF97316, 0xFFEF4444),
                new LinearLayout.LayoutParams(dp(50), dp(50)));
        LinearLayout copy = vertical();
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(merchant.name, 18, COLOR_TEXT, true));
        TextView notice = text(merchant.notice, 13, COLOR_MUTED, false);
        notice.setPadding(0, dp(4), 0, 0);
        copy.addView(notice);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        animateIn(card);
        return card;
    }

    private View foodCard(Dish dish) {
        LinearLayout card = horizontal();
        card.setPadding(dp(12), dp(10), dp(10), dp(10));
        card.setBackground(gradient(Color.argb(226, 255, 255, 255), Color.argb(160, 255, 247, 237),
                dp(22), 0xFFFFEDD5));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        card.addView(iconBubble(R.drawable.nav_food, 44, Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)),
                new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout names = vertical();
        names.setPadding(dp(12), 0, dp(8), 0);
        names.addView(text(dish.merchantName, 13, Color.rgb(100, 116, 139), false));
        names.addView(text(dish.name, 18, Color.rgb(15, 23, 42), true));
        TextView price = text(dish.price + " 元", 15, 0xFFEA580C, true);
        price.setPadding(0, dp(4), 0, 0);
        names.addView(price);
        card.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        Button add = primaryButton("加购");
        add.setTextSize(13);
        setLeftIcon(add, R.drawable.nav_food, 15);
        add.setOnClickListener(v -> {
            if (!cart.isEmpty() && !cart.get(0).merchantName.equals(dish.merchantName)) {
                toast("一次订单只能选择同一家商家");
                return;
            }
            cart.add(dish);
            toast("已加入购物车");
            render();
        });
        card.addView(add, new LinearLayout.LayoutParams(dp(62), dp(44)));
        animateIn(card);
        return card;
    }

    private View cartItemCard(Dish dish, int index) {
        LinearLayout card = horizontal();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(0xFFFFFBEB, dp(18), 0xFFFDE68A));
        LinearLayout row = horizontal();
        LinearLayout info = vertical();
        info.addView(text(dish.name, 16, COLOR_TEXT, true));
        info.addView(text(dish.merchantName + " · " + dish.price + " 元", 13, COLOR_MUTED, false));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        Button remove = smallButton("删除");
        remove.setOnClickListener(v -> {
            if (FoodOrderRules.removeAt(cart, index)) {
                toast("已从购物车删除");
            }
            render();
        });
        card.addView(remove, new LinearLayout.LayoutParams(dp(70), dp(42)));
        animateIn(card);
        return card;
    }

    private View foodOrderCard(FoodOrder order) {
        LinearLayout card = card();
        card.setBackground(gradient(0xFFFFFFFF, 0xFFF0F9FF, dp(22), COLOR_LINE));
        LinearLayout head = horizontal();
        head.addView(text(order.merchantName, 18, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = text(order.status, 12, Color.WHITE, true);
        status.setGravity(Gravity.CENTER);
        status.setBackground(rounded(orderStatusColor(order.status), dp(999), 0));
        head.addView(status, new LinearLayout.LayoutParams(dp(68), dp(30)));
        card.addView(head);
        card.addView(text(order.itemsSummary, 14, COLOR_MUTED, false));
        card.addView(text("合计 " + order.totalPrice + " 元", 15, Color.rgb(239, 68, 68), true));

        LinearLayout buttons = horizontal();
        if (FoodOrderRules.STATUS_WAITING.equals(order.status)
                || FoodOrderRules.STATUS_MAKING.equals(order.status)
                || FoodOrderRules.STATUS_READY.equals(order.status)) {
            Button next = primaryButton(orderActionLabel(order.status));
            next.setOnClickListener(v -> {
                repository.updateFoodOrderStatus(order.id, FoodOrderRules.nextStatus(order.status));
                toast("订单状态已更新");
                render();
            });
            buttons.addView(next, weightParams());
        }
        if (FoodOrderRules.STATUS_WAITING.equals(order.status) || FoodOrderRules.STATUS_MAKING.equals(order.status)) {
            Button cancel = smallButton("取消订单");
            cancel.setOnClickListener(v -> {
                repository.updateFoodOrderStatus(order.id, FoodOrderRules.STATUS_CANCELED);
                toast("订单已取消");
                render();
            });
            buttons.addView(cancel, weightParams());
        }
        if (buttons.getChildCount() > 0) {
            card.addView(buttons, marginTop(-1, -2, dp(8)));
        }
        animateIn(card);
        return card;
    }

    private String cartSummary() {
        if (cart.isEmpty()) {
            return "暂未选择菜品";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cart.size(); i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(cart.get(i).name);
        }
        return builder.toString();
    }

    private String orderActionLabel(String status) {
        if (FoodOrderRules.STATUS_WAITING.equals(status)) {
            return "商家接单";
        }
        if (FoodOrderRules.STATUS_MAKING.equals(status)) {
            return "制作完成";
        }
        return "顾客已取餐";
    }

    private int orderStatusColor(String status) {
        if (FoodOrderRules.STATUS_WAITING.equals(status)) {
            return Color.rgb(245, 158, 11);
        }
        if (FoodOrderRules.STATUS_MAKING.equals(status)) {
            return COLOR_BRAND;
        }
        if (FoodOrderRules.STATUS_READY.equals(status)) {
            return COLOR_SUCCESS;
        }
        if (FoodOrderRules.STATUS_CANCELED.equals(status)) {
            return Color.rgb(100, 116, 139);
        }
        return Color.rgb(20, 184, 166);
    }

    private void renderClassroom() {
        content.addView(pageHero("教室预约", "选择空闲教室和时间段", R.drawable.anime_classroom_header,
                0xFF10B981, COLOR_BRAND, "空间"), marginBottom(-1, -2, dp(10)));
        content.addView(classroomSearchCard(), marginBottom(-1, -2, dp(10)));
        addSectionTitle("可预约教室");
        List<ClassroomSlot> allSlots = classroomSlots();
        List<ClassroomSlot> slots = ClassroomSearchRules.filter(allSlots, classroomQuery);
        List<ClassroomSlot> availableSlots = new ArrayList<>();
        for (ClassroomSlot slot : slots) {
            List<String> bookedSlots = repository.getBookedSlotsForRoom(slot.room);
            if (!bookedSlots.containsAll(slot.subSlots())) {
                availableSlots.add(slot);
            }
        }
        if (availableSlots.isEmpty()) {
            addEmpty("没有找到可预约的教室，可换个关键词或稍后再试。");
        } else {
            for (ClassroomSlot slot : availableSlots) {
                content.addView(classroomCard(slot));
            }
        }
        addSectionTitle("我的预约");
        List<String[]> userReservations = repository.getUserReservations(currentUsername);
        if (userReservations.isEmpty()) {
            addEmpty("暂无预约记录。");
        } else {
            for (String[] res : userReservations) {
                content.addView(reservationCardDb(Long.parseLong(res[0]), res[1], res[2]));
            }
        }
    }

    private View classroomSearchCard() {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(gradient(Color.argb(224, 255, 255, 255), Color.argb(162, 239, 253, 251),
                dp(24), 0xFFCCFBF1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(3));
        }
        TextView title = text("快速搜索", 18, COLOR_TEXT, true);
        card.addView(title);
        TextView hint = text("按教室、容量或时间查找空闲教室", 13, COLOR_MUTED, false);
        hint.setPadding(0, dp(4), 0, dp(10));
        card.addView(hint);

        EditText search = input("例如：A101、图书馆、80、19:00", classroomQuery);
        search.setSingleLine(true);
        card.addView(search, marginBottom(-1, -2, dp(10)));

        LinearLayout actions = horizontal();
        Button apply = primaryButton("搜索");
        setLeftIcon(apply, R.drawable.nav_search, 18);
        apply.setOnClickListener(v -> {
            classroomQuery = search.getText().toString().trim();
            hideKeyboard(search);
            render();
        });
        Button clear = smallButton("清空");
        clear.setOnClickListener(v -> {
            classroomQuery = "";
            hideKeyboard(search);
            render();
        });
        actions.addView(apply, weightParams());
        actions.addView(clear, weightParams());
        card.addView(actions);
        animateIn(card);
        return card;
    }

    private List<ClassroomSlot> classroomSlots() {
        List<ClassroomSlot> slots = new ArrayList<>();
        slots.add(new ClassroomSlot("A101 智慧教室", "80 人", "08:00-10:00", 8, 10));
        slots.add(new ClassroomSlot("B204 研讨室", "24 人", "14:00-16:00", 14, 16));
        slots.add(new ClassroomSlot("图书馆 302", "12 人", "19:00-21:00", 19, 21));
        slots.add(new ClassroomSlot("实验楼 C506", "48 人", "10:00-12:00", 10, 12));
        slots.add(new ClassroomSlot("主楼 120 阶梯教室", "120 人", "18:00-20:00", 18, 20));
        return slots;
    }

    private View classroomCard(ClassroomSlot slot) {
        LinearLayout card = horizontal();
        card.setPadding(dp(0), dp(0), dp(14), dp(0));
        card.setBackground(gradient(Color.argb(226, 255, 255, 255), Color.argb(162, 240, 253, 244),
                dp(22), 0xFFD1FAE5));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout rail = vertical();
        rail.setGravity(Gravity.CENTER);
        rail.setBackground(gradient(0xFF10B981, COLOR_CYAN, dp(22)));
        rail.addView(iconBubble(R.drawable.nav_classroom, 42, 0xFF10B981, COLOR_CYAN),
                new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView railLabel = text("可约", 11, Color.WHITE, true);
        railLabel.setGravity(Gravity.CENTER);
        rail.addView(railLabel, marginTop(-1, -2, dp(2)));
        card.addView(rail, new LinearLayout.LayoutParams(dp(60), -1));
        LinearLayout body = vertical();
        body.setPadding(dp(14), dp(12), dp(0), dp(12));
        LinearLayout head = horizontal();
        TextView status = text("可约", 12, Color.rgb(22, 163, 74), true);
        status.setGravity(Gravity.CENTER);
        status.setBackground(rounded(0xFFDCFCE7, dp(999), 0));
        head.addView(text(slot.room, 18, Color.rgb(15, 23, 42), true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(status, new LinearLayout.LayoutParams(dp(54), dp(30)));
        body.addView(head);
        TextView meta = text(slot.capacity + " · " + slot.time + " 可预约", 14, COLOR_MUTED, false);
        meta.setPadding(0, dp(6), 0, dp(2));
        body.addView(meta);

        // 显示可预约的子时段按钮
        List<String> subSlots = slot.subSlots();
        List<String> bookedSlots = repository.getBookedSlotsForRoom(slot.room);
        LinearLayout slotRow = horizontal();
        slotRow.setPadding(0, dp(4), 0, dp(4));
        for (String subSlot : subSlots) {
            boolean booked = bookedSlots.contains(subSlot);
            Button slotBtn = booked ? smallButton(subSlot + " 已约") : smallButton(subSlot);
            slotBtn.setEnabled(!booked);
            if (booked) {
                slotBtn.setTextColor(Color.rgb(156, 163, 175));
                slotBtn.setBackground(rounded(0xFFF3F4F6, dp(12), 0xFFE5E7EB));
            }
            if (!booked) {
                slotBtn.setOnClickListener(v -> showReservationConfirm(slot.room, subSlot));
            }
            slotRow.addView(slotBtn, weightParams());
        }
        body.addView(slotRow, marginTop(-1, -2, dp(4)));
        card.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        animateIn(card);
        return card;
    }

    private void showReservationConfirm(String room, String timeSlot) {
        if (repository.hasUserTimeConflict(timeSlot, currentUsername)) {
            toast("该时段你已预约了其他教室，无法同时预约");
            return;
        }
        confirm("预约确认", room + "\n" + timeSlot, () -> {
            if (repository.createClassroomReservation(room, timeSlot, currentUsername)) {
                toast("预约成功");
            } else {
                toast("预约失败，该时段可能已被预约");
            }
            render();
        });
    }

    private View reservationCardDb(long id, String room, String timeSlot) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(gradient(0xFFF0FDF4, 0xFFFFFFFF, dp(22), 0xFFBBF7D0));
        LinearLayout head = horizontal();
        head.addView(text("预约成功", 18, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = text("已预约", 12, Color.WHITE, true);
        status.setGravity(Gravity.CENTER);
        status.setBackground(rounded(COLOR_SUCCESS, dp(999), 0));
        head.addView(status, new LinearLayout.LayoutParams(dp(68), dp(30)));
        card.addView(head);
        TextView detail = text(room + " \u00b7 " + timeSlot, 14, COLOR_MUTED, false);
        detail.setPadding(0, dp(6), 0, dp(8));
        card.addView(detail);
        Button cancel = smallButton("取消预约");
        cancel.setOnClickListener(v -> confirm("取消预约", "确定取消\"" + room + " " + timeSlot + "\"吗？", () -> {
            if (repository.cancelClassroomReservation(id, currentUsername)) {
                toast("预约已取消");
            } else {
                toast("取消失败");
            }
            render();
        }));
        card.addView(cancel);
        animateIn(card);
        return card;
    }

    private View reservationCard(String reservation, int index) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(gradient(0xFFF0FDF4, 0xFFFFFFFF, dp(22), 0xFFBBF7D0));
        LinearLayout head = horizontal();
        head.addView(text("预约成功", 18, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = text("已预约", 12, Color.WHITE, true);
        status.setGravity(Gravity.CENTER);
        status.setBackground(rounded(COLOR_SUCCESS, dp(999), 0));
        head.addView(status, new LinearLayout.LayoutParams(dp(68), dp(30)));
        card.addView(head);
        TextView detail = text(reservation, 14, COLOR_MUTED, false);
        detail.setPadding(0, dp(6), 0, dp(8));
        card.addView(detail);
        Button cancel = smallButton("取消预约");
        cancel.setOnClickListener(v -> confirm("取消预约", "确定取消“" + reservation + "”吗？", () -> {
            if (ReservationRules.removeAt(reservations, index)) {
                toast("预约已取消");
            }
            render();
        }));
        card.addView(cancel);
        return card;
    }

    private void renderTasks() {
        content.addView(pageHero("待办提醒", "未完成 " + repository.countUnfinished() + " 项", R.drawable.anime_tasks_header,
                0xFF7C3AED, COLOR_BRAND, "提醒"), marginBottom(-1, -2, dp(10)));
        Button addTask = primaryButton("新增待办");
        setLeftIcon(addTask, R.drawable.nav_tasks, 18);
        addTask.setOnClickListener(v -> showTaskDialog(null));
        content.addView(addTask, marginBottom(-1, -2, dp(10)));
        content.addView(summaryCard("消息提醒", "新增或编辑待办时填写提醒时间，到点后会通过系统通知提醒你。"));
        addSectionTitle("作业与提醒");
        List<TaskItem> tasks = repository.getTasks();
        if (tasks.isEmpty()) {
            addEmpty("暂无作业待办。");
            return;
        }
        for (TaskItem task : tasks) {
            content.addView(taskCard(task));
        }
    }

    private void renderMine() {
        String nickname = repository.getProfileField(currentUsername, "nickname");
        String contact = repository.getProfileField(currentUsername, "contact");
        String address = repository.getProfileField(currentUsername, "address");
        String bio = repository.getProfileField(currentUsername, "bio");
        String avatarUri = repository.getProfileField(currentUsername, "avatar");
        boolean isAdmin = repository.isAdmin(currentUsername);
        String displayName = nickname == null || nickname.isEmpty() ? currentUsername : nickname;

        // 个人信息卡片（含头像和编辑入口）
        LinearLayout profileCard = vertical();
        profileCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        profileCard.setBackground(gradient(Color.argb(238, 15, 23, 42), Color.argb(230, 20, 184, 166), dp(26), Color.argb(70, 255, 255, 255)));
        LinearLayout profileHead = horizontal();
        // 头像
        if (avatarUri != null && !avatarUri.isEmpty()) {
            ImageView avatar = new ImageView(this);
            safeSetImageUri(avatar, avatarUri);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setBackground(rounded(Color.WHITE, dp(999), 0));
            profileHead.addView(avatar, new LinearLayout.LayoutParams(dp(64), dp(64)));
        } else {
            TextView avatarText = text(displayName == null || displayName.isEmpty() ? "我" : displayName.substring(0, 1), 28, Color.WHITE, true);
            avatarText.setGravity(Gravity.CENTER);
            avatarText.setBackground(gradient(COLOR_BRAND, COLOR_CYAN, dp(999)));
            profileHead.addView(avatarText, new LinearLayout.LayoutParams(dp(64), dp(64)));
        }
        LinearLayout profileInfo = vertical();
        profileInfo.setPadding(dp(14), 0, 0, 0);
        profileInfo.addView(text(displayName, 22, Color.WHITE, true));
        profileInfo.addView(text(isAdmin ? "管理员" : "普通用户", 13, Color.rgb(204, 251, 241), false), marginTop(-1, -2, dp(4)));
        profileHead.addView(profileInfo, new LinearLayout.LayoutParams(0, -2, 1));
        profileCard.addView(profileHead);
        content.addView(profileCard, marginBottom(-1, -2, dp(12)));

        Button editProfile = primaryButton("编辑个人信息");
        editProfile.setOnClickListener(v -> showProfileEditDialog(nickname, contact, address, bio, avatarUri));
        content.addView(editProfile, marginTop(-1, -2, dp(10)));

        // 钱包
        int walletBalance = repository.getWalletBalance();
        content.addView(walletCard(walletBalance), marginTop(-1, -2, dp(10)));
        List<WalletTransaction> walletTransactions = repository.getWalletTransactions();
        addSectionTitle("钱包流水");
        if (walletTransactions.isEmpty()) {
            addEmpty("暂无虚拟货币流水。");
        } else {
            int shown = Math.min(3, walletTransactions.size());
            for (int i = 0; i < shown; i++) {
                WalletTransaction transaction = walletTransactions.get(i);
                String amountText = transaction.amount > 0 ? "+" + transaction.amount : String.valueOf(transaction.amount);
                content.addView(summaryCard(amountText + " 校园币",
                        transaction.description + " \u00b7 " + dateFormat.format(new Date(transaction.createdAt))));
            }
        }

        Button logout = primaryButton("退出登录");
        logout.setOnClickListener(v -> {
            getPrefs().edit().remove(PREF_USERNAME).apply();
            repository.clearAccount();
            currentUsername = "";
            currentTab = TAB_HOME;
            currentWeek = 1;
            classroomQuery = "";
            selectedCustomServiceId = "";
            selectedForumSectionId = 0;
            selectedForumPostId = 0;
            selectedMarketItemId = 0;
            cart.clear();
            pendingAttachmentPath = "";
            pendingAvatarPath = "";
            pendingMarketImage = "";
            renderAuthScreen(false);
        });
        content.addView(logout, marginTop(-1, -2, dp(8)));
    }

    private View profileRow(String label, String value) {
        LinearLayout row = horizontal();
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(gradient(0xFFFFFFFF, 0xFFF8FBFF, dp(18), COLOR_LINE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            row.setElevation(dp(1));
        }
        row.addView(text(label, 14, COLOR_MUTED, false), new LinearLayout.LayoutParams(dp(80), -2));
        row.addView(text(value, 15, COLOR_TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
        animateIn(row);
        return row;
    }

    /**
     * 安全地设置ImageView的图片URI，防止SecurityException导致崩溃。
     * Android 13+ Photo Picker返回的URI需要通过ContentResolver访问。
     */
    private void safeSetImageUri(ImageView imageView, String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            return;
        }
        try {
            android.net.Uri uri = android.net.Uri.parse(uriString);
            android.graphics.Bitmap bitmap = null;
            if (uriString.startsWith("content://")) {
                try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input != null) {
                        bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    }
                }
            } else if (uriString.startsWith("file://") || uriString.startsWith("/")) {
                String path = uriString.startsWith("file://") ? uri.getPath() : uriString;
                bitmap = android.graphics.BitmapFactory.decodeFile(path);
            }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        } catch (SecurityException e) {
            // URI权限不足，忽略
        } catch (Exception e) {
            // 其他异常，忽略
        }
    }

    /**
     * 创建用户头像视图，优先使用个人信息中设置的头像图片，否则用首字母。
     */
    private View createAvatarView(String username, int sizeDp) {
        String avatarUri = repository.getProfileField(username, "avatar");
        if (avatarUri != null && !avatarUri.isEmpty()) {
            ImageView avatar = new ImageView(this);
            safeSetImageUri(avatar, avatarUri);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setBackground(rounded(Color.WHITE, dp(sizeDp / 2), 0));
            avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
            return avatar;
        }
        String displayName = repository.getProfileField(username, "nickname");
        String label = (displayName != null && !displayName.isEmpty()) ? displayName
                : (username != null && !username.isEmpty() ? username : "同");
        if (label.length() > 1) {
            label = label.substring(0, 1);
        }
        TextView avatarText = text(label, 15, Color.WHITE, true);
        avatarText.setGravity(Gravity.CENTER);
        avatarText.setBackground(gradient(COLOR_CYAN, COLOR_BRAND, dp(sizeDp / 2)));
        avatarText.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return avatarText;
    }

    private void showProfileEditDialog(String nickname, String contact, String address, String bio, String avatarUri) {
        LinearLayout form = dialogForm();
        form.addView(label("头像"));
        LinearLayout avatarRow = horizontal();
        Button pickAvatar = smallButton("选择头像图片");
        pickAvatar.setOnClickListener(v -> {
            Intent imgIntent = new Intent(Intent.ACTION_GET_CONTENT);
            imgIntent.setType("image/*");
            imgIntent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(imgIntent, "选择头像"), REQUEST_CODE_PICK_AVATAR);
            } catch (Exception e) {
                toast("无法打开图片选择器");
            }
        });
        avatarRow.addView(pickAvatar, weightParams());
        form.addView(avatarRow);
        form.addView(label("昵称"));
        EditText nicknameInput = input("昵称", nickname == null ? "" : nickname);
        form.addView(nicknameInput);
        form.addView(label("联系方式"));
        EditText contactInput = input("手机号或邮箱", contact == null ? "" : contact);
        form.addView(contactInput);
        form.addView(label("地址"));
        EditText addressInput = input("地址", address == null ? "" : address);
        form.addView(addressInput);
        form.addView(label("个人简介"));
        EditText bioInput = input("一句话介绍自己", bio == null ? "" : bio);
        bioInput.setMinLines(3);
        bioInput.setGravity(Gravity.TOP | Gravity.START);
        form.addView(bioInput);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(form);
        new AlertDialog.Builder(this)
                .setTitle("编辑个人信息")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    repository.setProfileField(currentUsername, "nickname", nicknameInput.getText().toString().trim());
                    repository.setProfileField(currentUsername, "contact", contactInput.getText().toString().trim());
                    repository.setProfileField(currentUsername, "address", addressInput.getText().toString().trim());
                    repository.setProfileField(currentUsername, "bio", bioInput.getText().toString().trim());
                    if (pendingAvatarPath != null && !pendingAvatarPath.isEmpty()) {
                        repository.setProfileField(currentUsername, "avatar", pendingAvatarPath);
                        pendingAvatarPath = "";
                    }
                    toast("个人信息已保存");
                    render();
                })
                .show();
    }

    private View walletCard(int walletBalance) {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(gradient(0xFF0052D9, 0xFF13C2C2, dp(26)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(5));
        }
        card.addView(text("\u865a\u62df\u8d27\u5e01", 17, Color.argb(230, 255, 255, 255), true));
        TextView balance = text(walletBalance + " \u6821\u56ed\u5e01", 28, Color.WHITE, true);
        balance.setPadding(0, dp(6), 0, dp(12));
        card.addView(balance);
        LinearLayout actions = horizontal();
        Button recharge = primaryButton("\u5145\u503c");
        recharge.setOnClickListener(v -> showWalletChangeDialog(true));
        Button withdraw = smallButton("\u63d0\u73b0");
        withdraw.setOnClickListener(v -> showWalletChangeDialog(false));
        actions.addView(recharge, weightParams());
        actions.addView(withdraw, weightParams());
        card.addView(actions);
        animateIn(card);
        return card;
    }

    private View taskCard(TaskItem task) {
        int priorityColor = task.priority >= 3 ? 0xFFEF4444 : (task.priority == 2 ? COLOR_WARNING : COLOR_SUCCESS);
        LinearLayout card = horizontal();
        card.setPadding(0, 0, dp(14), 0);
        card.setBackground(gradient(Color.argb(226, 255, 255, 255), blend(priorityColor, Color.WHITE, 0.92f),
                dp(22), blend(priorityColor, Color.WHITE, 0.72f)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        LinearLayout rail = vertical();
        rail.setGravity(Gravity.CENTER);
        rail.setBackground(gradient(priorityColor, blend(priorityColor, COLOR_BRAND, 0.25f), dp(22)));
        rail.addView(iconBubble(R.drawable.nav_tasks, 40, priorityColor, blend(priorityColor, COLOR_BRAND, 0.25f)),
                new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView priority = text(priorityLabel(task.priority), 11, Color.WHITE, true);
        priority.setGravity(Gravity.CENTER);
        rail.addView(priority, marginTop(-1, -2, dp(2)));
        card.addView(rail, new LinearLayout.LayoutParams(dp(58), -1));
        LinearLayout body = vertical();
        body.setPadding(dp(12), dp(12), 0, dp(12));
        Course course = repository.getCourse(task.courseId);
        String courseName = course == null ? "未分配课程" : course.name;
        LinearLayout top = horizontal();
        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(task.completed);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repository.setTaskCompleted(task.id, isChecked);
            if (isChecked) {
                reminderScheduler.cancel(task.id);
                toast("\u5df2\u6807\u8bb0\u5b8c\u6210\uff0c\u5f85\u529e\u63d0\u9192\u5df2\u505c\u6b62");
            } else {
                TaskItem activeTask = repository.getTask(task.id);
                reminderScheduler.schedule(activeTask);
                if (activeTask != null && TaskRules.canScheduleReminder(activeTask.remindAtMillis, System.currentTimeMillis())) {
                    toast("\u5df2\u6062\u590d\u5f85\u529e\u63d0\u9192");
                } else {
                    toast("\u63d0\u9192\u65f6\u95f4\u5df2\u8fc7\uff0c\u8bf7\u7f16\u8f91\u5f85\u529e\u91cd\u65b0\u8bbe\u7f6e");
                }
            }
            render();
        });
        top.addView(checkBox);
        top.addView(text(task.title, 18, task.completed ? Color.GRAY : COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(top);
        body.addView(text(courseName + " · 优先级 " + priorityLabel(task.priority), 13, COLOR_MUTED, false));
        body.addView(text("截止时间：" + dateFormat.format(new Date(task.dueAtMillis)), 13, COLOR_MUTED, false));
        body.addView(text("提醒时间：" + dateFormat.format(new Date(task.remindAtMillis)), 13, task.completed ? COLOR_MUTED : COLOR_BRAND, true));
        body.addView(text(task.repeatMinutes > 0 ? "重复提醒：每 " + task.repeatMinutes + " 分钟" : "重复提醒：不重复", 13, COLOR_MUTED, false));
        if (task.completed) {
            body.addView(text("\u5df2\u5b8c\u6210\uff1a\u7cfb\u7edf\u4e0d\u518d\u53d1\u9001\u8fd9\u6761\u5f85\u529e\u7684\u63d0\u9192", 13, Color.rgb(185, 28, 28), true));
        }
        if (!task.description.isEmpty()) {
            TextView desc = text(task.description, 14, 0xFF334155, false);
            desc.setPadding(0, dp(6), 0, 0);
            body.addView(desc);
        }
        if (TaskRules.isOverdue(task, System.currentTimeMillis())) {
            body.addView(text("已逾期", 13, Color.rgb(185, 28, 28), true));
        }
        LinearLayout buttons = horizontal();
        Button edit = smallButton("编辑");
        edit.setOnClickListener(v -> showTaskDialog(task));
        Button delete = smallButton("删除");
        delete.setOnClickListener(v -> confirm("删除待办", "确定删除“" + task.title + "”吗？", () -> {
            reminderScheduler.cancel(task.id);
            repository.deleteTask(task.id);
            render();
        }));
        buttons.addView(edit, weightParams());
        buttons.addView(delete, weightParams());
        body.addView(buttons);
        card.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        animateIn(card);
        return card;
    }

    private void showCustomServiceDialog() {
        LinearLayout form = dialogForm();
        form.addView(label("服务名称"));
        EditText name = input("例如：校车查询", "");
        form.addView(name);
        form.addView(label("服务分类"));
        EditText category = input("例如：交通", "");
        form.addView(category);
        form.addView(label("服务说明"));
        EditText description = input("例如：查询校车线路和发车时间", "");
        description.setMinLines(3);
        description.setGravity(Gravity.TOP | Gravity.START);
        form.addView(description);

        new AlertDialog.Builder(this)
                .setTitle("添加常用服务")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("添加", (dialog, which) -> {
                    long id = repository.saveCustomService(
                            name.getText().toString(),
                            category.getText().toString(),
                            description.getText().toString()
                    );
                    if (id <= 0) {
                        toast("服务名称、分类和说明都要填写");
                        return;
                    }
                    hideKeyboard(name);
                    toast("服务已添加到首页");
                    currentTab = TAB_HOME;
                    selectedCustomServiceId = "";
                    buildShell();
                    render();
                })
                .show();
    }

    private void showForumPostDialog() {
        LinearLayout form = dialogForm();
        form.addView(label("帖子标题"));
        EditText title = input("例如：求推荐安静自习室", "");
        form.addView(title);
        form.addView(label("帖子内容"));
        EditText contentText = input("写下你想和同学交流的内容", "");
        contentText.setMinLines(5);
        contentText.setGravity(Gravity.TOP | Gravity.START);
        form.addView(contentText);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(form);
        new AlertDialog.Builder(this)
                .setTitle("发布帖子")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("发布", (dialog, which) -> {
                    long id = repository.createForumPost(
                            title.getText().toString(),
                            contentText.getText().toString(),
                            currentUsername
                    );
                    if (id <= 0) {
                        toast("标题和内容都要填写");
                        return;
                    }
                    hideKeyboard(title);
                    toast("帖子已发布");
                    currentTab = TAB_FORUM;
                    render();
                })
                .show();
    }

    private void showForumCommentDialog(ForumPost post) {
        LinearLayout form = dialogForm();
        form.addView(label("评论内容"));
        EditText comment = input("写下你的看法", "");
        comment.setMinLines(3);
        comment.setGravity(Gravity.TOP | Gravity.START);
        form.addView(comment);
        new AlertDialog.Builder(this)
                .setTitle("评论帖子")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("发布", (dialog, which) -> {
                    long id = repository.createForumComment(post.id, comment.getText().toString(), currentUsername);
                    if (id <= 0) {
                        toast("评论内容不能为空");
                        return;
                    }
                    hideKeyboard(comment);
                    toast("评论已发布");
                    currentTab = TAB_FORUM;
                    render();
                })
                .show();
    }

    private void deleteForumPost(ForumPost post) {
        confirm("删除帖子", "确定删除这条帖子和它的评论吗？", () -> {
            if (repository.deleteForumPost(post.id, currentUsername)) {
                toast("帖子已删除");
            } else {
                toast("只能删除自己发布的帖子");
            }
            currentTab = TAB_FORUM;
            render();
        });
    }

    private void showMarketplaceItemDialog() {
        pendingMarketImage = "";
        String profileContact = repository.getProfileField(currentUsername, "contact");
        LinearLayout form = dialogForm();
        form.addView(label("物品名称"));
        EditText name = input("例如：二手台灯", "");
        form.addView(name);
        form.addView(label("价格"));
        EditText price = input("例如：25", "");
        price.setInputType(0x00000002);
        form.addView(price);
        form.addView(label("物品描述"));
        EditText description = input("例如：九成新，可宿舍楼下自提", "");
        description.setMinLines(4);
        description.setGravity(Gravity.TOP | Gravity.START);
        form.addView(description);
        form.addView(label("物品图片（必选）"));
        LinearLayout imageRow = horizontal();
        Button pickImage = smallButton("选择图片");
        pickImage.setOnClickListener(v -> {
            Intent imgIntent = new Intent(Intent.ACTION_GET_CONTENT);
            imgIntent.setType("image/*");
            imgIntent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(imgIntent, "选择物品图片"), REQUEST_CODE_PICK_MARKET_IMAGE);
            } catch (Exception e) {
                toast("无法打开图片选择器");
            }
        });
        imageRow.addView(pickImage, weightParams());
        form.addView(imageRow);
        form.addView(label("联系方式"));
        EditText contact = input("从个人信息自动关联，可修改", profileContact == null ? "" : profileContact);
        form.addView(contact);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(form);
        new AlertDialog.Builder(this)
                .setTitle("发布闲置")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("发布", (dialog, which) -> {
                    if (pendingMarketImage == null || pendingMarketImage.isEmpty()) {
                        toast("请选择物品图片");
                        return;
                    }
                    try {
                        long id = repository.createMarketplaceItem(
                                name.getText().toString(),
                                Integer.parseInt(price.getText().toString().trim()),
                                description.getText().toString(),
                                contact.getText().toString(),
                                currentUsername,
                                pendingMarketImage
                        );
                        if (id <= 0) {
                            toast("名称、价格、描述和联系方式都要填写");
                            return;
                        }
                        hideKeyboard(name);
                        toast("闲置物品已发布");
                        currentTab = TAB_MARKET;
                        render();
                    } catch (NumberFormatException e) {
                        toast("价格需要填写数字");
                    }
                })
                .show();
    }

    private void showMarketplaceCommentDialog(MarketplaceItem item) {
        LinearLayout form = dialogForm();
        form.addView(label("评论内容"));
        EditText comment = input("询问成色、取货地点或补充信息", "");
        comment.setMinLines(3);
        comment.setGravity(Gravity.TOP | Gravity.START);
        form.addView(comment);
        new AlertDialog.Builder(this)
                .setTitle("评论闲置")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("发布", (dialog, which) -> {
                    long id = repository.createMarketplaceComment(item.id, comment.getText().toString(), currentUsername);
                    if (id <= 0) {
                        toast("评论内容不能为空");
                        return;
                    }
                    hideKeyboard(comment);
                    toast("评论已发布");
                    currentTab = TAB_MARKET;
                    render();
                })
                .show();
    }

    private void showWalletChangeDialog(boolean recharge) {
        int walletBalance = repository.getWalletBalance();
        LinearLayout form = dialogForm();
        form.addView(label(recharge ? "\u5145\u503c\u91d1\u989d" : "\u63d0\u73b0\u91d1\u989d"));
        EditText amount = input(recharge ? "\u8f93\u5165\u8981\u5145\u503c\u7684\u6821\u56ed\u5e01" : "\u8f93\u5165\u8981\u63d0\u73b0\u7684\u6821\u56ed\u5e01", "");
        amount.setInputType(0x00000002);
        form.addView(amount);
        TextView tip = text("\u5f53\u524d\u4f59\u989d\uff1a" + walletBalance + " \u6821\u56ed\u5e01", 13, COLOR_MUTED, false);
        tip.setPadding(0, dp(8), 0, 0);
        form.addView(tip);
        new AlertDialog.Builder(this)
                .setTitle(recharge ? "\u865a\u62df\u8d27\u5e01\u5145\u503c" : "\u865a\u62df\u8d27\u5e01\u63d0\u73b0")
                .setView(form)
                .setNegativeButton("\u53d6\u6d88", null)
                .setPositiveButton(recharge ? "\u786e\u8ba4\u5145\u503c" : "\u786e\u8ba4\u63d0\u73b0", (dialog, which) -> {
                    try {
                        int value = Integer.parseInt(amount.getText().toString().trim());
                        boolean ok = recharge ? repository.rechargeWallet(value) : repository.withdrawWallet(value);
                        if (!ok) {
                            toast(recharge ? "\u5145\u503c\u91d1\u989d\u9700\u5927\u4e8e 0" : "\u4f59\u989d\u4e0d\u8db3\u6216\u63d0\u73b0\u91d1\u989d\u65e0\u6548");
                            return;
                        }
                        hideKeyboard(amount);
                        toast(recharge ? "\u5145\u503c\u6210\u529f" : "\u63d0\u73b0\u6210\u529f");
                        currentTab = TAB_MINE;
                        render();
                    } catch (NumberFormatException e) {
                        toast("\u8bf7\u586b\u5199\u6b63\u786e\u7684\u91d1\u989d\u6570\u5b57");
                    }
                })
                .show();
    }

    private void showMarketBidDialog(MarketplaceItem item) {
        int walletBalance = repository.getWalletBalance();
        LinearLayout form = dialogForm();
        form.addView(label("出价金额"));
        EditText bidPrice = input("需高于当前价 " + item.currentPrice + " 元", "");
        bidPrice.setInputType(0x00000002);
        form.addView(bidPrice);
        TextView tips = text("当前价：" + item.currentPrice + " 元，起拍价：" + item.price + " 元", 13, COLOR_MUTED, false);
        tips.setPadding(0, dp(8), 0, 0);
        form.addView(tips);
        TextView balanceTip = text("\u6211\u7684\u4f59\u989d\uff1a" + walletBalance + " \u6821\u56ed\u5e01", 13, COLOR_BRAND, true);
        balanceTip.setPadding(0, dp(6), 0, 0);
        form.addView(balanceTip);
        new AlertDialog.Builder(this)
                .setTitle("我要出价")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("确认出价", (dialog, which) -> {
                    try {
                        int price = Integer.parseInt(bidPrice.getText().toString().trim());
                        if (!CommunityRules.canAffordBid(repository.getWalletBalance(), price)) {
                            toast("\u4f59\u989d\u4e0d\u8db3\uff0c\u51fa\u4ef7\u5931\u8d25");
                            return;
                        }
                        long id = repository.createMarketBid(item.id, price, currentUsername);
                        if (id <= 0) {
                            toast("出价必须高于当前价");
                            return;
                        }
                        hideKeyboard(bidPrice);
                        toast("出价成功");
                        currentTab = TAB_MARKET;
                        render();
                    } catch (NumberFormatException e) {
                        toast("出价需要填写数字");
                    }
                })
                .show();
    }

    private void showMarketplaceSettleDialog(MarketplaceItem item, List<MarketBid> bids) {
        if (bids.isEmpty()) {
            toast("暂无出价，不能售卖");
            return;
        }
        CharSequence[] options = new CharSequence[bids.size()];
        for (int i = 0; i < bids.size(); i++) {
            MarketBid bid = bids.get(i);
            options[i] = bid.bidder + " · " + bid.price + " 元 · 到账 "
                    + CommunityRules.sellerIncome(bid.price) + " 元";
        }
        new AlertDialog.Builder(this)
                .setTitle("选择出价成交")
                .setItems(options, (dialog, which) -> {
                    MarketBid bid = bids.get(which);
                    if (repository.settleMarketplaceSale(item.id, bid.id, currentUsername)) {
                        toast("售卖成功，虚拟货币已到账");
                    } else {
                        toast("只能由发布者选择有效出价成交");
                    }
                    currentTab = TAB_MARKET;
                    render();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCourseDialog(Course course) {
        LinearLayout form = dialogForm();
        EditText name = input("课程名称", course == null ? "" : course.name);
        EditText teacher = input("教师", course == null ? "" : course.teacher);
        EditText location = input("地点", course == null ? "" : course.location);
        EditText weekday = input("星期几（1-7）", course == null ? "" : String.valueOf(course.weekday));
        EditText startSection = input("开始节次", course == null ? "" : String.valueOf(course.startSection));
        EditText endSection = input("结束节次", course == null ? "" : String.valueOf(course.endSection));
        EditText startWeek = input("开始周", course == null ? "" : String.valueOf(course.startWeek));
        EditText endWeek = input("结束周", course == null ? "" : String.valueOf(course.endWeek));
        form.addView(name);
        form.addView(teacher);
        form.addView(location);
        form.addView(weekday);
        form.addView(startSection);
        form.addView(endSection);
        form.addView(startWeek);
        form.addView(endWeek);
        new AlertDialog.Builder(this)
                .setTitle(course == null ? "新增课程" : "编辑课程")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    try {
                        String courseName = name.getText().toString().trim();
                        if (courseName.isEmpty()) {
                            toast("课程名称不能为空");
                            return;
                        }
                        int day = clamp(parseInt(weekday), 1, 7);
                        int start = clamp(parseInt(startSection), 1, SECTION_COUNT);
                        int end = clamp(parseInt(endSection), start, SECTION_COUNT);
                        int firstWeek = clamp(parseInt(startWeek), 1, MAX_WEEK);
                        int lastWeek = clamp(parseInt(endWeek), firstWeek, MAX_WEEK);
                        int color = course == null ? palette(repository.getCourses().size()) : course.color;
                        long id = course == null ? 0 : course.id;
                        repository.saveCourse(new Course(id, courseName, teacher.getText().toString(), location.getText().toString(), color, day, start, end, firstWeek, lastWeek));
                        hideKeyboard(name);
                        courseReminderScheduler.scheduleAll(currentUsername);
                        currentTab = TAB_SCHEDULE;
                        render();
                    } catch (NumberFormatException e) {
                        toast("星期、节次、周次都要填写数字");
                    }
                })
                .show();
    }

    private void showCourseImportDialog() {
        LinearLayout form = dialogForm();
        TextView tip = text("登陆北信科教务系统，点击课表查询-我的课表，选择课表打印excel，将下载的excel上传到app", 14, COLOR_MUTED, false);
        tip.setPadding(0, 0, 0, dp(10));
        form.addView(tip);

        Button openAcademicSystem = smallButton("打开北信科教务系统");
        openAcademicSystem.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ACADEMIC_SYSTEM_URL)));
            } catch (Exception e) {
                toast("无法打开教务系统链接");
            }
        });
        form.addView(openAcademicSystem, marginBottom(-1, -2, dp(10)));

        Button uploadExcel = primaryButton("上传课表");
        uploadExcel.setOnClickListener(v -> {
            Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(fileIntent, "选择课表文件"), REQUEST_CODE_IMPORT_EXCEL);
            } catch (Exception e) {
                toast("无法打开文件选择器");
            }
        });
        form.addView(uploadExcel, marginBottom(-1, -2, dp(10)));

        ScrollView dialogScroll = new ScrollView(this);
        dialogScroll.addView(form);

        new AlertDialog.Builder(this)
                .setTitle("从教务系统导入")
                .setView(dialogScroll)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showTaskDialog(TaskItem task) {
        List<Course> courses = repository.getCourses();
        LinearLayout form = dialogForm();
        Spinner courseSpinner = new Spinner(this);
        List<String> courseNameList = new ArrayList<>();
        courseNameList.add("无");
        for (Course c : courses) {
            courseNameList.add(c.name);
        }
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNameList);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(courseAdapter);
        if (task != null) {
            if (task.courseId == 0) {
                courseSpinner.setSelection(0);
            } else {
                int idx = indexOfCourse(courses, task.courseId);
                courseSpinner.setSelection(idx < 0 ? 0 : idx + 1);
            }
        }
        EditText title = input("待办标题", task == null ? "" : task.title);
        EditText description = input("备注", task == null ? "" : task.description);
        EditText due = input("截止时间 yyyy-MM-dd HH:mm", task == null ? dateFormat.format(new Date(System.currentTimeMillis() + 86400000L)) : dateFormat.format(new Date(task.dueAtMillis)));
        EditText remind = input("提醒时间 yyyy-MM-dd HH:mm", task == null ? dateFormat.format(new Date(System.currentTimeMillis() + 3600000L)) : dateFormat.format(new Date(task.remindAtMillis)));
        EditText repeat = input("每隔几分钟提醒，0 表示不重复", task == null ? "0" : String.valueOf(task.repeatMinutes));
        repeat.setInputType(0x00000002);
        Spinner prioritySpinner = new Spinner(this);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"低", "中", "高"});
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(task == null ? 1 : Math.max(0, Math.min(2, task.priority - 1)));
        form.addView(label("所属课程"));
        form.addView(courseSpinner);
        form.addView(title);
        form.addView(description);
        form.addView(due);
        form.addView(remind);
        form.addView(label("重复提醒"));
        form.addView(repeat);
        form.addView(label("优先级"));
        form.addView(prioritySpinner);
        new AlertDialog.Builder(this)
                .setTitle(task == null ? "新增待办" : "编辑待办")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String taskTitle = title.getText().toString();
                    if (!TaskRules.isValidTitle(taskTitle)) {
                        toast("待办标题不能为空");
                        return;
                    }
                    try {
                        long dueAt = parseDate(due.getText().toString());
                        long remindAt = parseDate(remind.getText().toString());
                        if (!TaskRules.canScheduleReminder(remindAt, System.currentTimeMillis())) {
                            toast("\u63d0\u9192\u65f6\u95f4\u9700\u8981\u665a\u4e8e\u5f53\u524d\u65f6\u95f4");
                            return;
                        }
                        if (!hasNotificationPermission()) {
                            requestNotificationPermission();
                            toast("\u8bf7\u5141\u8bb8\u901a\u77e5\u6743\u9650\uff0c\u5426\u5219\u6536\u4e0d\u5230\u5f85\u529e\u63d0\u9192");
                        }
                        int selectedPos = courseSpinner.getSelectedItemPosition();
                        long courseId = selectedPos == 0 ? 0 : courses.get(selectedPos - 1).id;
                        int priority = prioritySpinner.getSelectedItemPosition() + 1;
                        int repeatMinutes = TaskRules.normalizeRepeatMinutes(parseOptionalInt(repeat.getText().toString()));
                        long id = task == null ? 0 : task.id;
                        boolean completed = task != null && task.completed;
                        TaskItem saved = new TaskItem(id, courseId, taskTitle, description.getText().toString(), dueAt, remindAt, repeatMinutes, priority, completed);
                        long savedId = repository.saveTask(saved);
                        reminderScheduler.schedule(new TaskItem(savedId, saved.courseId, saved.title, saved.description, saved.dueAtMillis, saved.remindAtMillis, saved.repeatMinutes, saved.priority, saved.completed));
                        hideKeyboard(title);
                        render();
                    } catch (ParseException e) {
                        toast("时间格式应为 yyyy-MM-dd HH:mm");
                    } catch (NumberFormatException e) {
                        toast("重复提醒间隔需要填写数字");
                    }
                })
                .show();
    }

    private View summaryCard(String title, String body) {
        LinearLayout card = card();
        LinearLayout row = horizontal();
        row.addView(iconBubble(summaryIconForTitle(title), 44, COLOR_BRAND, COLOR_BRAND_SOFT),
                new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout copy = vertical();
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(title, 17, COLOR_TEXT, true));
        TextView bodyView = text(body, 14, COLOR_MUTED, false);
        bodyView.setPadding(0, dp(5), 0, 0);
        copy.addView(bodyView);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row);
        return card;
    }

    private int summaryIconForTitle(String title) {
        String safeTitle = title == null ? "" : title;
        if (safeTitle.contains("消息") || safeTitle.contains("提醒")) {
            return R.drawable.nav_tasks;
        }
        if (safeTitle.contains("已选") || safeTitle.contains("外卖")) {
            return R.drawable.nav_food;
        }
        if (safeTitle.contains("校园币") || safeTitle.contains("交易")) {
            return R.drawable.nav_market;
        }
        if (safeTitle.contains("账号")) {
            return R.drawable.nav_mine;
        }
        return R.drawable.nav_home;
    }

    private void addSectionTitle(String value) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(18), 0, dp(10));
        TextView mark = text("", 1, COLOR_BRAND, true);
        mark.setBackground(rounded(COLOR_BRAND, dp(999), 0));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(4), dp(18));
        markParams.setMargins(0, 0, dp(8), 0);
        row.addView(mark, markParams);
        TextView view = text(value, 17, COLOR_TEXT, true);
        row.addView(view, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(row);
    }

    private void addSectionHeader(String title, String action) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(18), 0, dp(10));
        TextView view = text(title, 19, COLOR_TEXT, true);
        row.addView(view, new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null && !action.isEmpty()) {
            TextView actionView = text(action, 13, COLOR_BRAND, true);
            actionView.setGravity(Gravity.CENTER);
            actionView.setBackground(rounded(COLOR_BRAND_MIST, dp(999), 0));
            row.addView(actionView, new LinearLayout.LayoutParams(dp(60), dp(32)));
        }
        content.addView(row);
    }

    private void addEmpty(String value) {
        TextView empty = text(value, 15, COLOR_MUTED, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(36), dp(20), dp(36));
        empty.setBackground(gradient(COLOR_BRAND_MIST, 0xFFFFFFFF, dp(RADIUS_CARD), COLOR_LINE));
        content.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        animateIn(empty);
    }

    private TextView label(String value) {
        TextView label = text(value, 13, COLOR_MUTED, true);
        label.setPadding(0, dp(12), 0, dp(4));
        return label;
    }

    private EditText input(String hint, String value) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setSingleLine(false);
        editText.setTextSize(15);
        editText.setTextColor(COLOR_TEXT);
        editText.setHintTextColor(0xFF94A3B8);
        editText.setMinHeight(dp(48));
        editText.setPadding(dp(15), dp(9), dp(15), dp(9));
        editText.setBackground(gradient(Color.argb(238, 247, 250, 255), Color.argb(206, 255, 255, 255),
                dp(RADIUS_CONTROL), Color.argb(150, 226, 236, 247)));
        return editText;
    }

    private LinearLayout dialogForm() {
        LinearLayout form = vertical();
        form.setPadding(dp(18), dp(8), dp(18), 0);
        return form;
    }

    private Button primaryButton(String text) {
        Button button = chipButton(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(gradient(COLOR_BRAND_DEEP, COLOR_BRAND_SOFT, dp(RADIUS_CONTROL),
                Color.argb(92, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(dp(5));
        }
        return button;
    }

    private Button smallButton(String text) {
        Button button = chipButton(text);
        button.setTextColor(COLOR_BRAND);
        button.setBackground(gradient(Color.argb(210, 234, 243, 255), Color.argb(120, 255, 255, 255),
                dp(RADIUS_CONTROL), Color.argb(126, 22, 119, 255)));
        return button;
    }

    private Button chipButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(COLOR_TEXT);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(false);
        button.setMinHeight(dp(46));
        button.setMinimumHeight(dp(46));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackground(gradient(Color.argb(226, 241, 246, 255), Color.argb(150, 255, 255, 255),
                dp(RADIUS_CONTROL), Color.argb(90, 255, 255, 255)));
        addPressFeedback(button);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = vertical();
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(layeredSurface(dp(RADIUS_CARD)));
        LinearLayout.LayoutParams params = marginBottom(-1, -2, dp(9));
        card.setLayoutParams(params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        animateIn(card);
        return card;
    }

    private LinearLayout glassCard() {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(gradient(Color.argb(238, 255, 255, 255), Color.argb(190, 236, 246, 255),
                dp(30), Color.argb(175, 255, 255, 255)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(12));
        }
        return card;
    }

    private View commentLine(String author, String content, long createdAt) {
        LinearLayout row = horizontal();
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(rounded(0xFFF3F8FF, dp(12), 0));
        row.addView(createAvatarView(author, 28), new LinearLayout.LayoutParams(dp(28), dp(28)));
        LinearLayout textCol = vertical();
        textCol.setPadding(dp(8), 0, 0, 0);
        TextView meta = text(author + " · " + dateFormat.format(new Date(createdAt)), 11, COLOR_MUTED, false);
        textCol.addView(meta);
        textCol.addView(text(content, 13, Color.rgb(51, 65, 85), false));
        row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1));
        row.setLayoutParams(marginBottom(-1, -2, dp(6)));
        return row;
    }

    private GradientDrawable rounded(int fill, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        return gradient(start, end, radius, 0);
    }

    private GradientDrawable gradient(int start, int end, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private Drawable layeredSurface(int radius) {
        GradientDrawable shadowTint = rounded(Color.argb(116, 105, 177, 255), radius + dp(4), 0);
        GradientDrawable surface = gradient(Color.argb(224, 255, 255, 255), Color.argb(164, 237, 246, 255),
                radius, Color.argb(142, 255, 255, 255));
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{shadowTint, surface});
        layerDrawable.setLayerInset(0, dp(1), dp(5), dp(1), 0);
        layerDrawable.setLayerInset(1, 0, 0, 0, dp(4));
        return layerDrawable;
    }

    private void addPressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.965f).scaleY(0.965f).alpha(0.92f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(230)
                        .setInterpolator(new OvershootInterpolator(1.8f))
                        .start();
            }
            return false;
        });
    }

    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.985f);
        view.setScaleY(0.985f);
        view.setTranslationY(dp(16));
        long delay = Math.min(animationIndex, 10) * 34L;
        view.post(() -> view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delay)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .start());
        animationIndex++;
    }

    private void breathe(View view) {
        ValueAnimator animator = ValueAnimator.ofFloat(0.98f, 1f);
        animator.setDuration(520);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            view.setScaleX(scale);
            view.setScaleY(scale);
        });
        animator.start();
    }

    private int blend(int from, int to, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int red = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * clamped);
        int green = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * clamped);
        int blue = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * clamped);
        return Color.rgb(red, green, blue);
    }

    @SuppressLint("WrongConstant")
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value == null ? "" : value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setLineSpacing(dp(1), 1.08f);
        textView.setIncludeFontPadding(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
            textView.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        } else {
            textView.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
            textView.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        }
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private GridLayout.LayoutParams gridParams() {
        return gridParams(false);
    }

    private GridLayout.LayoutParams gridParams(boolean featured) {
        int columns = serviceColumnCount();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int availableWidth = screenWidth - dp(PAGE_PADDING * 2) - dp(10 * columns);
        int span = featured ? Math.min(2, columns) : 1;
        int cellWidth = availableWidth / columns;
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, span);
        params.width = (cellWidth * span) + dp(10 * (span - 1));
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(dp(5), dp(5), dp(5), dp(9));
        return params;
    }

    private int serviceColumnCount() {
        int width = getResources().getDisplayMetrics().widthPixels;
        if (width < dp(420)) {
            return 2;
        }
        return width < dp(720) ? 3 : 4;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private LinearLayout.LayoutParams navItemParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(66), dp(50));
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private LinearLayout.LayoutParams marginBottom(int width, int height, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, 0, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams marginTop(int width, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, top, 0, 0);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private List<String> courseNames(List<Course> courses) {
        List<String> names = new ArrayList<>();
        for (Course course : courses) {
            names.add(course.name);
        }
        return names;
    }

    private int indexOfCourse(List<Course> courses, long courseId) {
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).id == courseId) {
                return i;
            }
        }
        return 0;
    }

    private long parseDate(String value) throws ParseException {
        Date date = dateFormat.parse(value.trim());
        if (date == null) {
            throw new ParseException(value, 0);
        }
        return date.getTime();
    }

    private int parseInt(EditText editText) {
        return Integer.parseInt(editText.getText().toString().trim());
    }

    private int parseOptionalInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int palette(int index) {
        int[] colors = {
                Color.rgb(59, 130, 246),
                Color.rgb(16, 185, 129),
                Color.rgb(245, 158, 11),
                Color.rgb(236, 72, 153),
                Color.rgb(139, 92, 246),
                Color.rgb(20, 184, 166),
                Color.rgb(239, 68, 68)
        };
        return colors[Math.abs(index) % colors.length];
    }

    private String priorityLabel(int priority) {
        if (priority >= 3) {
            return "高";
        }
        return priority == 2 ? "中" : "低";
    }

    private void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> action.run())
                .show();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void requestNotificationPermission() {
        if (!hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static class LiquidGlassNavScrollView extends HorizontalScrollView {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float slideEnergy = 0.35f;

        LiquidGlassNavScrollView(Context context) {
            super(context);
            setWillNotDraw(false);
            setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            slideEnergy = Math.min(1f, slideEnergy + Math.abs(l - oldl) / 90f);
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                slideEnergy = Math.min(1f, slideEnergy + 0.18f);
                invalidate();
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            long now = System.currentTimeMillis();
            float phase = ((getScrollX() * 0.62f) + (now % 1800L) / 1800f * width) % width;

            super.dispatchDraw(canvas);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int) (28 + 42 * slideEnergy), 255, 255, 255));
            canvas.drawRoundRect(phase - 62, 5, phase + 62, height - 7, height / 2f, height / 2f, paint);

            paint.setColor(Color.argb((int) (18 + 28 * slideEnergy), 105, 177, 255));
            canvas.drawCircle((phase + width * 0.34f) % width, height * 0.50f, height * 0.34f, paint);

            paint.setColor(Color.argb(44, 255, 255, 255));
            canvas.drawRoundRect(12, 5, width - 12, 7, 8, 8, paint);
            paint.setColor(Color.argb(30, 22, 119, 255));
            canvas.drawRoundRect(14, height - 9, width - 14, height - 8, 8, 8, paint);

            slideEnergy *= 0.90f;
            if (slideEnergy > 0.04f) {
                postInvalidateDelayed(32);
            }
        }
    }

    private static class AmbientBackdropView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] seeds = new float[42];

        AmbientBackdropView(Context context) {
            super(context);
            for (int i = 0; i < seeds.length; i++) {
                seeds[i] = (float) ((i * 37 % 101) / 101.0);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            canvas.drawColor(Color.argb(28, 244, 248, 255));
            long now = System.currentTimeMillis();

            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 7; i++) {
                float pulse = (float) (0.68 + Math.sin((now / 900.0) + i) * 0.12);
                float x = (0.10f + seeds[i] * 0.80f) * width;
                float y = (0.10f + seeds[(i + 5) % seeds.length] * 0.62f) * height;
                float radius = (42 + (i % 4) * 18) * pulse;
                paint.setColor(Color.argb(10 + (i % 3) * 5, i % 2 == 0 ? 105 : 255, i % 2 == 0 ? 177 : 214, 255));
                canvas.drawCircle(x, y, radius, paint);
            }

            for (int i = 0; i < 18; i++) {
                float x = (seeds[i] * width + (now % 9000L) / 9000f * width * (0.08f + i * 0.002f)) % width;
                float y = (0.08f + seeds[(i + 9) % seeds.length] * 0.42f) * height;
                float radius = 2.2f + (i % 4);
                paint.setColor(Color.argb(30 + (i % 3) * 12, 255, 224, 150));
                canvas.drawCircle(x, y, radius, paint);
            }

            for (int i = 18; i < seeds.length; i++) {
                float drift = (now % 12000L) / 12000f;
                float x = (seeds[i] * width + drift * width * (0.18f + (i % 5) * 0.02f)) % width;
                float y = ((seeds[(i + 13) % seeds.length] + drift * (0.16f + (i % 4) * 0.018f)) % 1f) * height;
                canvas.save();
                canvas.rotate(-18 + (i % 7) * 8, x, y);
                paint.setColor(Color.argb(34 + (i % 4) * 8, 255, 158, 193));
                canvas.drawOval(x - 5, y - 2, x + 5, y + 2, paint);
                canvas.restore();
            }

            postInvalidateDelayed(48);
        }
    }
}
