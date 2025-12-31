# 南粤风炊火 - 野炊教学AR应用

[![Android CI](https://github.com/YOUR_USERNAME/CampcookingAR/workflows/Android%20CI/badge.svg)](https://github.com/YOUR_USERNAME/CampcookingAR/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](http://developer.android.com/index.html)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-blue.svg?logo=kotlin)](http://kotlinlang.org)

<div align="center">
  <img src="fengmian.png" alt="南粤风炊火" width="600"/>
</div>

## 📱 项目简介

这是一款专为10-11寸Android平板横向使用设计的野炊教学应用。应用提供野炊技巧、户外烹饪指南等教学内容。

### ✨ 特色功能

- 🎨 **精美封面页** - 南粤风格设计，展现野炊文化
- 📱 **横屏优化** - 专为平板横向使用优化
- 🎬 **流畅动画** - 现代化的UI动画效果
- 🖼️ **沉浸体验** - 全屏沉浸式界面
- ⚡ **快速启动** - 3秒自动进入或点击快进
- 🎯 **现代架构** - Kotlin + ViewBinding + Material Design

## 技术栈

- **开发语言**: Kotlin
- **最低SDK**: API 24 (Android 7.0)
- **目标SDK**: API 34 (Android 14)
- **构建工具**: Android Gradle Plugin 8.2.0
- **UI框架**: Material Design Components
- **视图绑定**: ViewBinding

## 项目结构

```
CampcookingAR/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/campcooking/ar/
│   │       │   ├── SplashActivity.kt       # 封面页Activity
│   │       │   └── MainActivity.kt          # 主Activity
│   │       ├── res/
│   │       │   ├── layout/                  # 布局文件
│   │       │   │   ├── activity_splash.xml
│   │       │   │   ├── activity_main.xml
│   │       │   │   └── layout-land/        # 横屏专用布局
│   │       │   ├── values/                  # 资源值
│   │       │   │   ├── strings.xml
│   │       │   │   ├── colors.xml
│   │       │   │   ├── themes.xml
│   │       │   │   └── dimens.xml
│   │       │   ├── values-sw600dp/         # 7寸平板适配
│   │       │   ├── values-sw720dp/         # 10寸平板适配
│   │       │   ├── drawable/                # 图片资源
│   │       │   │   └── fengmian.png        # 封面图片
│   │       │   ├── anim/                    # 动画资源
│   │       │   └── xml/                     # 设备配置
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 设备要求

- **屏幕尺寸**: 10-11寸平板
- **屏幕方向**: 横屏 (Landscape)
- **分辨率**: 建议 1920x1200 或更高
- **Android版本**: 7.0 (API 24) 及以上

## 主要特性

### 当前版本 (v1.0)

- ✅ 精美的封面启动页
- ✅ 横屏模式优化
- ✅ 平板大屏适配 (10-11寸)
- ✅ 流畅的动画效果
- ✅ 全屏沉浸式体验

### 计划功能

- 📋 野炊教学内容库
- 📋 AR增强现实功能
- 📋 视频教程播放
- 📋 互动式烹饪指南
- 📋 食材识别
- 📋 安全提示系统

## 构建说明

### 环境配置

1. 安装 Android Studio (推荐最新版)
2. 确保已安装 Android SDK API 34
3. 配置 Kotlin 插件

### 编译步骤

```bash
# 1. 克隆或打开项目
cd CampcookingAR

# 2. 清理并构建项目
./gradlew clean build

# 3. 生成Debug APK
./gradlew assembleDebug

# 4. 生成Release APK
./gradlew assembleRelease
```

### 在设备上运行

1. 连接Android平板到电脑
2. 启用开发者选项和USB调试
3. 在Android Studio中点击运行按钮
4. 或使用命令: `./gradlew installDebug`

## 使用说明

1. **启动应用**: 点击应用图标启动
2. **封面页**: 显示3秒后自动进入主界面，或点击屏幕任意位置快速进入
3. **主界面**: 待后续功能开发

## 开发指南

### 添加新Activity

```kotlin
// 1. 创建Activity类
class NewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

// 2. 在AndroidManifest.xml中注册
<activity
    android:name=".NewActivity"
    android:screenOrientation="landscape" />
```

### 适配不同屏幕尺寸

- `values/`: 默认资源 (手机)
- `values-sw600dp/`: 7寸平板
- `values-sw720dp/`: 10寸及以上平板
- `layout-land/`: 横屏专用布局

## 性能优化

- 使用 ViewBinding 替代 findViewById
- 图片资源经过优化压缩
- 启用 ProGuard 代码混淆
- 延迟加载非必要资源

## 版本历史

### v1.0 (当前版本)
- 初始版本发布
- 实现封面启动页
- 完成横屏平板适配

## 许可证

Copyright © 2025 CampCooking AR Team

## 🤝 贡献

欢迎贡献！请查看 [贡献指南](CONTRIBUTING.md) 了解如何参与项目。

### 贡献者

感谢所有为这个项目做出贡献的开发者！

<!-- 
如果使用GitHub，可以自动显示贡献者：
<a href="https://github.com/YOUR_USERNAME/CampcookingAR/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=YOUR_USERNAME/CampcookingAR" />
</a>
-->

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系方式

- 💬 [创建Issue](https://github.com/YOUR_USERNAME/CampcookingAR/issues/new)
- 📧 联系邮箱: your.email@example.com
- 🌐 项目主页: https://github.com/YOUR_USERNAME/CampcookingAR

## 🙏 致谢

- 感谢所有贡献者的付出
- 感谢开源社区的支持
- 特别感谢Android开发团队

## ⭐ Star History

如果这个项目对您有帮助，请给我们一个Star ⭐

[![Star History Chart](https://api.star-history.com/svg?repos=YOUR_USERNAME/CampcookingAR&type=Date)](https://star-history.com/#YOUR_USERNAME/CampcookingAR&Date)

---

**注意**: 本应用专为横屏平板设计，在手机上可能显示效果不佳。

Made with ❤️ by CampCooking AR Team

