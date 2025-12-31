# 贡献指南

感谢您对"南粤风炊火"项目的关注！我们欢迎各种形式的贡献。

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [问题反馈](#问题反馈)

## 行为准则

### 我们的承诺

为了营造开放和友好的环境，我们承诺：
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员保持同理心

### 不可接受的行为

- 使用性化的语言或图像
- 人身攻击或贬损性评论
- 骚扰行为
- 未经许可发布他人私人信息
- 其他不道德或不专业的行为

## 如何贡献

### 报告Bug

发现Bug？请创建Issue并包含：
1. 问题的清晰描述
2. 复现步骤
3. 期望行为vs实际行为
4. 设备信息（型号、Android版本、屏幕尺寸）
5. 相关截图或日志

### 建议新功能

有好点子？请创建Feature Request并说明：
1. 功能描述
2. 使用场景
3. 预期效果
4. 可能的实现方案

### 提交代码

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m '添加某个功能'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 开发流程

### 环境设置

```bash
# 1. 克隆仓库
git clone https://github.com/你的用户名/CampcookingAR.git
cd CampcookingAR

# 2. 在Android Studio中打开项目

# 3. 等待Gradle同步完成

# 4. 连接设备或启动模拟器

# 5. 运行应用
./gradlew installDebug
```

### 分支策略

- `main` - 主分支，保持稳定
- `develop` - 开发分支
- `feature/*` - 新功能分支
- `bugfix/*` - Bug修复分支
- `release/*` - 发布准备分支

### 开发步骤

1. **从develop分支创建特性分支**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/your-feature-name
   ```

2. **进行开发**
   - 遵循代码规范
   - 添加必要的注释
   - 编写测试用例

3. **本地测试**
   ```bash
   ./gradlew test
   ./gradlew lintDebug
   ```

4. **提交代码**
   ```bash
   git add .
   git commit -m "类型: 简短描述"
   ```

5. **推送并创建PR**
   ```bash
   git push origin feature/your-feature-name
   ```

## 代码规范

### Kotlin编码规范

遵循[官方Kotlin编码规范](https://kotlinlang.org/docs/coding-conventions.html)

**命名约定：**
```kotlin
// 类名：大驼峰
class SplashActivity

// 函数名：小驼峰
fun navigateToMain()

// 变量名：小驼峰
val userName: String

// 常量：全大写下划线分隔
const val MAX_COUNT = 100

// 资源ID：小写下划线分隔
R.id.button_submit
```

**代码格式：**
```kotlin
// ✅ 推荐
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

// ❌ 不推荐
class MainActivity:AppCompatActivity(){
    var binding:ActivityMainBinding?=null
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
    }
}
```

### XML编码规范

**布局文件：**
```xml
<!-- ✅ 推荐：属性按顺序排列 -->
<TextView
    android:id="@+id/text_title"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:text="@string/title"
    android:textColor="@color/primary"
    android:textSize="24sp"
    app:layout_constraintTop_toTopOf="parent" />
```

**命名规范：**
- 布局文件：`activity_*.xml`, `fragment_*.xml`
- 控件ID：`类型_用途` 如 `button_submit`, `text_title`
- 颜色：语义化命名 `primary`, `accent`, `error`
- 字符串：`模块_用途` 如 `splash_title`, `error_network`

### 注释规范

```kotlin
/**
 * Activity类注释
 * 
 * 描述这个Activity的主要功能
 * 
 * @author 作者名
 * @since 版本号
 */
class SplashActivity : AppCompatActivity() {
    
    /**
     * 函数功能说明
     * 
     * @param parameter 参数说明
     * @return 返回值说明
     */
    fun someFunction(parameter: String): Boolean {
        // 单行注释：解释复杂逻辑
        return true
    }
}
```

## 提交规范

遵循[约定式提交](https://www.conventionalcommits.org/)规范：

```
类型(范围): 简短描述

详细描述（可选）

相关Issue（可选）
```

**类型：**
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例：**
```bash
feat(splash): 添加封面页动画效果

- 添加标题缩放动画
- 添加图片淡入效果
- 优化动画时长

关闭 #123
```

## 问题反馈

### 创建Issue前

1. 搜索现有Issue，避免重复
2. 确认是否已在最新版本修复
3. 准备完整的重现步骤

### Issue标签

- `bug` - Bug报告
- `enhancement` - 功能请求
- `documentation` - 文档相关
- `good first issue` - 适合新手
- `help wanted` - 需要帮助
- `priority: high` - 高优先级
- `priority: low` - 低优先级

## Pull Request流程

1. **PR必须满足：**
   - 通过所有自动化测试
   - 代码符合规范
   - 包含必要的文档更新
   - 至少一位维护者审核通过

2. **审核标准：**
   - 代码质量
   - 测试覆盖
   - 性能影响
   - UI/UX体验
   - 平板适配情况

3. **合并要求：**
   - 所有检查通过
   - 解决所有审核意见
   - 没有合并冲突

## 版本发布

遵循[语义化版本](https://semver.org/)规范：

- **主版本号**：不兼容的API修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

## 资源

- [Android开发者文档](https://developer.android.com)
- [Kotlin官方文档](https://kotlinlang.org/docs/home.html)
- [Material Design指南](https://material.io/design)
- [项目README](README.md)

## 联系方式

- 创建Issue讨论
- 参与Pull Request评论
- 关注项目动态

---

再次感谢您的贡献！每一个贡献都让这个项目变得更好。🎉

