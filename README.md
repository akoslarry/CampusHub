# 北信科有品（CampusHub）

> **校园服务聚合平台** — 移动应用系统课程综合实践项目  
> 北京信息科技大学 · 计算机科学与技术专业  
> 技术栈：Java + Android SDK + SQLite · 纯代码动态 UI（无 XML 布局）

[![Android](https://img.shields.io/badge/Android-14%2B-green?logo=android)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📖 项目简介

**北信科有品**是一个面向大学生的校园服务聚合应用，将课程表管理、校园外卖、教室预约、待办提醒、校园论坛、闲置交易、钱包系统等多项服务整合在一个入口中。项目采用纯代码动态 UI 方案（所有界面由 Java 代码直接构建，不使用 XML 布局文件），全面覆盖 Android 四大组件（Activity、Service、BroadcastReceiver、ContentProvider）。

### ✨ 亮点特性

- **液态玻璃导航**：多层液态玻璃滑动导航栏，5 Tab 等宽分布
- **动态粒子背景**：阳光/樱花粒子特效 + 二次元校园短横幅
- **生成式 3D 图标**：错落服务入口 + 卡片错峰入场动画 + 按钮弹性触控缩放
- **自研轻量级 xlsx 解析器**：不依赖 Apache POI，直接解析 zip+xml 结构导入教务课表
- **完整钱包系统**：虚拟货币充值/提现/交易记录，外卖扣款 + 闲置交易平台费结算
- **分时段教室预约**：跨账户互斥机制，冲突检测，满员教室自动隐藏

---

## 🧭 功能模块

| 模块 | 功能描述 |
|---|---|
| 🏠 **首页服务大厅** | 服务搜索、全部服务入口、顶部状态入口、待办标题展示 |
| 📅 **课程表** | 周课表展示（周一到周日 × 1-16节）、课程增删改、教务系统 xlsx 一键导入、周次切换、16 色课程区分 |
| 🍔 **校园外卖** | 商家列表、菜品展示、购物车、模拟支付下单、订单状态流转、钱包扣款 |
| 🏫 **教室预约** | 空闲教室模拟、分时段预约、跨账户互斥冲突检测、预约记录持久化 |
| ✅ **待办提醒** | 任务增删改查、优先级管理、重复提醒、AlarmManager 精确闹钟、系统通知 |
| 💬 **校园论坛** | 版块管理（管理员创建/删除）、发帖带图片附件、评论回复、权限删帖 |
| 🛒 **闲置交易** | 商品发布（必选图片）、商品详情、竞价、卖家确认售卖、联系方式关联个人信息 |
| 💰 **钱包系统** | 虚拟货币余额、充值/提现、交易流水、外卖扣款、闲置交易 2% 平台费结算 |
| 👤 **个人中心** | 头像/昵称/联系方式/地址/简介编辑、订单/预约/钱包概览、退出登录 |
| 🔐 **登录注册** | 本地账号系统、管理员/普通用户角色区分、SharedPreferences 登录保持 |

---

## 🏗️ 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   UI 层 (Presentation)                │
│          MainActivity 单体承载全部 9 个 Tab           │
│     纯代码动态 UI：Java 直接构建 View 层级            │
├─────────────────────────────────────────────────────┤
│                 业务逻辑层 (Business)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │ 4 Service│ │4 Receiver│ │ 4 ContentProvider    │ │
│  │ 各1前台  │ │ 各1广播  │ │ 各1数据暴露          │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│  ┌──────────────────────────────────────────────────┐│
│  │  CampusTaskRepository  统一数据仓库（44KB+）      ││
│  └──────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────┤
│                   数据层 (Data)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │AuthDb    │ │CampusTask│ │CommunityDbHelper     │ │
│  │Helper    │ │DbHelper  │ │(论坛/闲置/预约)       │ │
│  │(用户认证) │ │(课程/外卖 │ │                      │ │
│  │          │ │ 待办/钱包)│ │                      │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│  ┌──────────────────────────────────────────────────┐│
│  │     SharedPreferences（登录态/个人信息/课表缓存）  ││
│  └──────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### 数据库设计

| 数据库 | 表名 | 用途 |
|---|---|---|
| `auth.db` | `users` | 用户认证（username/password/role） |
| `campustask.db` | `courses` | 课程数据 |
| | `food_orders` | 外卖订单 |
| | `tasks` | 待办任务 |
| | `wallet_transactions` | 钱包交易流水 |
| | `services` | 自定义服务 |
| `campus_community.db` | `classroom_reservations` | 教室预约记录 |
| | `forum_sections` | 论坛版块 |
| | `forum_posts_v2` | 论坛帖子（含附件） |
| | `forum_comments_v2` | 论坛评论 |
| | `marketplace_items` | 闲置商品（含图片） |
| | `market_bids` | 闲置竞价 |

### 四大组件分布

| 组件类型 | 数量 | 说明 |
|---|---|---|
| **Activity** | 1 | MainActivity 单体架构，按 Tab 方法划分归属（成员A：首页+课表 / 成员B：外卖+教室 / 成员C：待办 / 成员D：论坛+闲置+我的） |
| **Service** | 4 | ScheduleSyncService / FoodOrderService / TaskReminderService / MarketSettlementService（每人1个前台 Service） |
| **BroadcastReceiver** | 5 | ScheduleSyncReceiver / FoodOrderReceiver / ReminderReceiver / BootReceiver / MarketSettlementReceiver |
| **ContentProvider** | 4 | CourseProvider / FoodOrderProvider / TaskProvider / CommunityProvider（每人1个） |

---

## 👥 团队分工

| 角色 | 成员 | 负责模块 | 核心贡献 |
|---|---|---|---|
| 🏗️ **架构师/组长** | 成员A | 框架 + 首页 + 课程表 | 项目架构搭建、MainActivity 骨架、登录注册、自研 xlsx 解析器、导航栏、数据库设计、系统集成 |
| 🍔 **外卖/教室** | 成员B | 校园外卖 + 教室预约 | 商家菜品购物车下单、订单状态流转、分时段教室预约、跨账户互斥、预约持久化 |
| ✅ **待办/提醒** | 成员C | 待办提醒 + 通知系统 | 任务 CRUD、AlarmManager 精确调度、通知渠道、开机自启恢复、ReminderScheduler |
| 💬 **社区/交易** | 成员D | 论坛 + 闲置 + 钱包 + 我的 | 版块管理、帖子/评论、闲置竞价售卖、钱包系统、个人信息、2% 平台费结算 |

---

## 🚀 快速开始

### 环境要求

- **Android Studio**（最新稳定版）
- **JDK 17** 或 **JDK 21**
- **Gradle 8.13+**
- Android SDK API 36

### 打开项目

```bash
# 克隆仓库
git clone <your-repo-url>
cd BeiXinKeYouPin-glass-depth-source

# 用 Android Studio 打开，等待 Gradle Sync 完成
```

### 命令行构建

```powershell
# 运行单元测试 + 构建 Debug APK
.\gradlew.bat testDebugUnitTest assembleDebug

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 运行测试

```bash
.\gradlew.bat test
```

测试覆盖：`AuthRulesTest` / `ScheduleRulesTest` / `CourseImportParserTest` / `ServiceCatalogTest` / `FoodOrderRulesTest` / `ClassroomSearchRulesTest` / `ReservationRulesTest` / `TaskRulesTest` / `CommunityRulesTest` / `AccountDatabaseRulesTest`

### 演示流程

1. 启动应用 → 登录页，点击"没有账号，去注册"
2. 输入用户名/密码/确认密码 → 注册成功自动进入首页
3. 首页浏览服务大厅，搜索服务，查看课程表
4. 课程表 → 导入教务系统 xlsx 课表 → 周次切换查看
5. 校园外卖 → 选择商家 → 加入菜品 → 购物车结算 → 钱包扣款
6. 教室预约 → 选择教室 → 选择时间段 → 预约成功
7. 待办提醒 → 新增任务 → 设置提醒时间 → 等待系统通知
8. 校园论坛 → 浏览版块 → 发帖/评论 → 管理员删帖
9. 闲置交易 → 发布商品（带图片）→ 浏览商品 → 出价 → 卖家确认售卖
10. 我的 → 编辑个人信息 → 查看钱包余额 → 交易记录 → 退出登录

> 💡 **预置管理员账号**：`admin` / `admin123` 或 `wyj` / `926495`（可创建/删除论坛版块，删除任意帖子）

---

## 📁 项目结构

```
BeiXinKeYouPin-glass-depth-source/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/campustask/
│   │   │   │   ├── MainActivity.java          # 核心单体 Activity（~2600 行）
│   │   │   │   ├── data/                       # 数据层
│   │   │   │   │   ├── AuthDbHelper.java       # 认证数据库
│   │   │   │   │   ├── CampusTaskDbHelper.java # 业务数据库
│   │   │   │   │   ├── CampusTaskRepository.java # 统一数据仓库
│   │   │   │   │   └── CommunityDbHelper.java  # 社区数据库
│   │   │   │   ├── model/                      # 模型层（27 个类）
│   │   │   │   ├── service/                    # 4 个前台 Service
│   │   │   │   ├── receiver/                   # 3 个 BroadcastReceiver
│   │   │   │   ├── provider/                   # 4 个 ContentProvider
│   │   │   │   ├── reminder/                   # 提醒调度系统
│   │   │   │   └── view/                       # 自定义 View（液态玻璃导航等）
│   │   │   ├── res/                            # 资源文件
│   │   │   └── AndroidManifest.xml             # 应用清单
│   │   └── test/                               # 单元测试（10 个测试类）
│   └── build.gradle                            # 应用构建配置
├── docs/
│   └── 团队分工与四大组件对应文档.md              # 详细分工文档
├── build.gradle                                # 项目构建配置
├── settings.gradle                             # 项目设置
├── gradle.properties                           # Gradle 属性
└── README.md                                   # 本文件
```

---

## 🔧 技术决策记录

| 编号 | 决策 | 理由 |
|---|---|---|
| D001 | 纯代码动态 UI（无 XML 布局） | 团队统一风格，所有界面由 Java 代码构建 |
| D002 | 单体 Activity + Tab 方法分发 | 减少 Activity 跳转开销，9 个 Tab 共享同一 Activity |
| D003 | 每人独立负责一套四大组件 | 确保四人均有 Service/Receiver/Provider 的代码参与 |
| D004 | 自研 xlsx 解析器（zip+XML） | 不引入 Apache POI（30MB+），减小 APK 体积 |
| D005 | 论坛回复用 LinearLayout 动态添加 | 解决 ScrollView 嵌套 RecyclerView 的测量问题 |
| D006 | 预置管理员账号 | 确保管理员功能可直接演示 |
| D007 | 教室预约跨账户互斥 | 数据库持久化预约记录，任意账户不可重复预约同一时段 |
| D008 | 三个独立 SQLite 数据库 | 认证/业务/社区数据分离，便于 ContentProvider 各自暴露 |

---

## 📝 更新记录

- **2026-07-02** 教室预约跨账户互斥 + 导航栏精简 + 论坛版块系统 + 管理员账号
- **2026-07-02** 导航栏固定等宽 + 论坛三级显示 + 我的页面重构 + 个人信息设置
- **2026-07-02** 闲置图片必选 + 论坛头像关联个人信息 + 闲置两级显示 + 上课提醒
- **2026-07-02** 外卖钱包扣款 + 外卖订单区域 + 退出登录状态重置

---

## 📄 License

MIT License — 本项目仅用于课程学习与个人项目经历展示。
