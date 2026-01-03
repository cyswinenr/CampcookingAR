# 教师端App - 使用说明

## 项目结构

教师端App位于 `CampcookingAR-Teacher/` 文件夹中，与学生端App完全独立，使用相同的编译架构和样式。

## 需要手动复制的资源文件

由于某些资源文件是二进制格式，需要手动从学生端复制：

### 1. 封面图片
- **源文件**: `app/src/main/res/drawable/fengmian.png`
- **目标位置**: `CampcookingAR-Teacher/app/src/main/res/drawable/fengmian.png`
- **说明**: 这是首页显示的封面图片

### 2. 应用图标（可选）
如果需要自定义图标，从学生端复制以下文件：
- `app/src/main/res/mipmap-*/ic_launcher.png`
- `app/src/main/res/mipmap-*/ic_launcher_round.png`

或者使用Android Studio的图标生成工具创建新图标。

## 项目配置

- **包名**: `com.campcooking.teacher`
- **应用ID**: `com.campcooking.teacher`
- **最低SDK**: 24
- **目标SDK**: 34
- **编译SDK**: 34
- **屏幕方向**: 横屏（landscape）

## 功能说明

### 首页（SplashActivity）
- 样式完全参考学生端
- 添加了"教师端"标识
- 移除了清理数据功能
- 点击"进入应用"跳转到MainActivity

### 主页面（MainActivity）
- 当前为占位页面
- 后续将实现WebView功能，显示服务器管理界面

## 构建和运行

1. 在Android Studio中打开 `CampcookingAR-Teacher` 文件夹
2. 同步Gradle文件
3. 确保已复制封面图片资源
4. 运行应用

## 注意事项

- 确保已复制 `fengmian.png` 到 `app/src/main/res/drawable/` 目录
- 如果没有图标资源，Android Studio会使用默认图标
- 网络配置已允许HTTP连接（用于本地服务器）

