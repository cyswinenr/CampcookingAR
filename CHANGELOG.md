# 更新日志

本文档记录项目的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 计划中
- AR增强现实功能
- 野炊教学内容库
- 视频教程播放
- 用户交互优化

## [1.0.0] - 2025-12-31

### 新增
- ✨ 精美的封面启动页
  - 展示"南粤风炊火"主题设计
  - 流畅的淡入、缩放动画效果
  - 自动3秒跳转或点击快速进入
  - 全屏沉浸式体验

- 📱 横屏平板优化
  - 强制横屏模式锁定
  - 专为10-11寸平板设计的布局
  - 响应式尺寸适配（手机、7寸、10寸平板）
  - 自动适配不同DPI

- 🎨 现代化UI设计
  - Material Design组件
  - 野炊主题配色方案
  - 优雅的背景渐变
  - 阴影和高光效果

- 🏗️ 项目架构
  - Kotlin语言开发
  - ViewBinding视图绑定
  - MVVM架构准备
  - 模块化设计

- 📚 完整文档
  - README项目说明
  - 快速开始指南
  - 构建说明文档
  - 项目文件清单
  - 贡献指南

- 🔧 GitHub集成
  - CI/CD自动化构建
  - Issue和PR模板
  - 自动化标签
  - Release自动发布
  - 代码安全扫描

### 技术规格
- 最低SDK: API 24 (Android 7.0)
- 目标SDK: API 34 (Android 14)
- 编译SDK: API 34
- Kotlin版本: 1.9.20
- Gradle版本: 8.2.0
- AGP版本: 8.2.0

### 依赖
- AndroidX Core KTX: 1.12.0
- AppCompat: 1.6.1
- Material Components: 1.11.0
- ConstraintLayout: 2.1.4
- Lifecycle Runtime KTX: 2.7.0

### 文件结构
```
├── app/                      # 应用模块
│   ├── src/main/
│   │   ├── java/            # Kotlin源代码
│   │   ├── res/             # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── .github/                 # GitHub配置
│   ├── workflows/          # CI/CD工作流
│   └── ISSUE_TEMPLATE/     # Issue模板
├── gradle/                  # Gradle wrapper
├── docs/                    # 文档（中文）
├── README.md               # 项目说明
├── CONTRIBUTING.md         # 贡献指南
├── LICENSE                 # MIT许可证
└── CHANGELOG.md            # 本文件
```

### 已知问题
- 主界面功能待开发
- 缺少应用图标（使用默认图标）
- 暂无数据持久化

### 安全
- 无已知安全问题
- 代码通过静态分析
- 依赖项均为最新稳定版

## [0.1.0] - 2025-12-31

### 新增
- 项目初始化
- 基本项目结构
- Gradle配置

---

## 版本说明

### 版本格式
主版本号.次版本号.修订号 (MAJOR.MINOR.PATCH)

- **主版本号**：不兼容的API修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

### 变更类型

- `新增` - 新功能
- `变更` - 现有功能的变更
- `弃用` - 即将移除的功能
- `移除` - 已移除的功能
- `修复` - Bug修复
- `安全` - 安全问题修复

---

[Unreleased]: https://github.com/YOUR_USERNAME/CampcookingAR/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/YOUR_USERNAME/CampcookingAR/releases/tag/v1.0.0
[0.1.0]: https://github.com/YOUR_USERNAME/CampcookingAR/releases/tag/v0.1.0

