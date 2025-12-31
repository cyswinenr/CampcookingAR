# 📦 GitHub功能完整说明

本项目已配置完整的GitHub集成功能，包括自动化工作流、Issue管理、安全策略等。

## 📋 目录

- [项目结构](#项目结构)
- [自动化工作流](#自动化工作流)
- [Issue管理](#issue管理)
- [安全功能](#安全功能)
- [发布管理](#发布管理)
- [使用指南](#使用指南)

---

## 📁 项目结构

### GitHub配置文件清单

```
.github/
├── workflows/                      # GitHub Actions工作流
│   ├── android-build.yml          # ✅ Android CI自动构建
│   ├── release.yml                # ✅ 发布版本自动化
│   ├── labeler.yml                # ✅ PR自动标签
│   └── greetings.yml              # ✅ 新贡献者欢迎消息
├── ISSUE_TEMPLATE/                 # Issue模板
│   ├── bug_report.md              # ✅ Bug报告模板
│   └── feature_request.md         # ✅ 功能请求模板
├── pull_request_template.md        # ✅ PR模板
├── CODE_OWNERS                     # ✅ 代码所有者
├── SECURITY.md                     # ✅ 安全政策
├── FUNDING.yml                     # ✅ 赞助配置
├── dependabot.yml                  # ✅ 依赖自动更新
├── stale.yml                       # ✅ 闲置Issue处理
├── labeler.yml                     # ✅ 自动标签规则
└── release-drafter.yml             # ✅ Release草稿

根目录/
├── LICENSE                         # ✅ MIT许可证
├── CODE_OF_CONDUCT.md             # ✅ 行为准则
├── CONTRIBUTING.md                 # ✅ 贡献指南
├── CHANGELOG.md                    # ✅ 更新日志
├── .gitignore                      # ✅ Git忽略配置
├── README.md                       # ✅ 项目说明（含徽章）
├── 如何发布到GitHub.md             # ✅ 发布教程
├── GitHub功能说明.md               # ✅ 本文件
├── 初始化Git仓库.bat               # ✅ 快速初始化脚本
└── 推送到GitHub.bat                # ✅ 快速推送脚本
```

---

## 🤖 自动化工作流

### 1. Android CI构建 (`android-build.yml`)

**触发条件：**
- 推送到 `main` 或 `develop` 分支
- 创建Pull Request到这些分支

**功能：**
- ✅ 自动编译项目
- ✅ 运行单元测试
- ✅ 构建Debug APK
- ✅ 运行Lint代码检查
- ✅ 上传构建产物（保留30天）

**查看方式：**
```
仓库 → Actions → Android CI
```

**徽章显示：**
```markdown
[![Android CI](https://github.com/YOUR_USERNAME/CampcookingAR/workflows/Android%20CI/badge.svg)](https://github.com/YOUR_USERNAME/CampcookingAR/actions)
```

### 2. Release构建 (`release.yml`)

**触发条件：**
- 推送带 `v*` 格式的标签（如 `v1.0.0`）

**功能：**
- ✅ 构建Release APK
- ✅ 自动创建GitHub Release
- ✅ 上传APK到Release
- ✅ 生成Release说明

**使用方法：**
```bash
# 创建标签
git tag -a v1.0.0 -m "发布版本 1.0.0"

# 推送标签
git push origin v1.0.0

# 自动触发Release构建
```

### 3. PR自动标签 (`labeler.yml`)

**功能：**
根据修改的文件自动为PR添加标签：

| 文件类型 | 标签 |
|---------|------|
| `*.kt`, `*.java` | `android` |
| `res/layout/**` | `ui` |
| `res/**` | `resources` |
| `*.gradle` | `config` |
| `*.md` | `documentation` |
| `.github/**` | `ci` |

**效果：**
创建PR后自动添加相应标签，方便分类管理。

### 4. 欢迎新贡献者 (`greetings.yml`)

**功能：**
- 首次创建Issue → 自动发送欢迎消息
- 首次提交PR → 自动发送贡献感谢

**消息示例：**
> 👋 感谢您创建第一个Issue！
> 我们会尽快审查您的问题...

---

## 📝 Issue管理

### Issue模板

#### 1. Bug报告模板

**包含字段：**
- Bug描述
- 复现步骤
- 期望行为
- 实际行为
- 截图
- 设备信息（型号、Android版本、屏幕尺寸）
- 日志信息

**创建方式：**
```
Issues → New issue → 选择 "Bug报告"
```

#### 2. 功能请求模板

**包含字段：**
- 功能描述
- 问题背景
- 建议的解决方案
- 替代方案
- 使用场景
- 优先级

**创建方式：**
```
Issues → New issue → 选择 "功能请求"
```

### 自动化处理

#### 闲置Issue管理 (`stale.yml`)

**规则：**
- 60天无活动 → 标记为 `stale`
- 再过7天无响应 → 自动关闭
- 豁免标签：`pinned`, `security`, `priority: high`

**效果：**
自动清理长期无响应的Issue，保持Issue列表整洁。

---

## 🔒 安全功能

### 1. 安全政策 (`SECURITY.md`)

**内容：**
- 支持的版本列表
- 漏洞报告流程
- 响应时间承诺
- 负责任的披露指南
- 安全最佳实践

**查看方式：**
```
仓库 → Security → Security policy
```

### 2. Dependabot自动更新 (`dependabot.yml`)

**功能：**
- 每周自动检查Gradle依赖更新
- 每周检查GitHub Actions更新
- 自动创建PR更新依赖
- 限制最多10个开放的PR

**标签：**
- `dependencies` - 依赖更新
- `gradle` - Gradle依赖
- `github-actions` - Actions更新

**效果：**
保持依赖最新，减少安全风险。

### 3. 代码所有者 (`CODE_OWNERS`)

**功能：**
- 指定文件/目录的审查负责人
- PR需要对应负责人批准
- 自动请求相关人员审查

**配置示例：**
```
*.gradle @build-team
*.md @documentation-team
```

---

## 📦 发布管理

### 1. Release草稿 (`release-drafter.yml`)

**功能：**
根据PR标签自动生成Release说明：

**分类：**
- 🚀 新功能 (`enhancement`, `feature`)
- 🐛 Bug修复 (`bug`, `fix`)
- 📚 文档 (`documentation`)
- 🔧 维护 (`chore`, `dependencies`)
- ⚡ 性能优化 (`performance`)
- ♻️ 代码重构 (`refactor`)

**自动版本号：**
- `major` 标签 → 主版本号+1
- `minor` 标签 → 次版本号+1
- `patch` 标签 → 修订号+1
- 默认 → 修订号+1

### 2. 更新日志 (`CHANGELOG.md`)

**格式：**
遵循 [Keep a Changelog](https://keepachangelog.com/) 规范

**版本号：**
遵循 [语义化版本](https://semver.org/) 规范

**内容：**
- [Unreleased] - 开发中的功能
- [1.0.0] - 已发布版本
- 每个版本包含：新增、变更、修复、安全等

---

## 👥 社区管理

### 1. 行为准则 (`CODE_OF_CONDUCT.md`)

**基于：** Contributor Covenant v1.4

**内容：**
- 我们的承诺
- 行为标准
- 责任和执行
- 适用范围

### 2. 贡献指南 (`CONTRIBUTING.md`)

**包含：**
- 行为准则
- 如何贡献（Bug报告、功能建议、代码提交）
- 开发流程
- 代码规范（Kotlin、XML）
- 提交规范（约定式提交）
- 问题反馈
- PR流程

### 3. PR模板 (`pull_request_template.md`)

**包含字段：**
- 变更类型（Bug修复、新功能、重构等）
- 变更描述
- 相关Issue
- 测试情况
- 截图/视频
- 检查清单

---

## 💰 赞助支持

### 配置文件 (`FUNDING.yml`)

**支持平台：**
- GitHub Sponsors
- Patreon
- Open Collective
- Ko-fi
- 自定义链接

**配置示例：**
```yaml
github: [your-github-username]
custom: ["https://donate.example.com"]
```

---

## 📊 项目徽章

README中已包含的徽章：

```markdown
# CI状态
[![Android CI](https://github.com/YOUR_USERNAME/CampcookingAR/workflows/Android%20CI/badge.svg)](...)

# 许可证
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](...)

# API级别
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](...)

# 平台
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](...)

# Kotlin版本
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-blue.svg)](...)
```

**显示效果：**
- 构建状态（通过/失败）
- 许可证类型
- 支持的Android版本
- 开发语言和版本

---

## 🚀 使用指南

### 快速开始

#### Windows用户：

**初始化Git仓库：**
```cmd
双击运行：初始化Git仓库.bat
```

**推送到GitHub：**
```cmd
双击运行：推送到GitHub.bat
```

#### 命令行用户：

**初始化：**
```bash
git init
git add .
git commit -m "feat: 初始提交"
git branch -M main
```

**推送：**
```bash
git remote add origin https://github.com/YOUR_USERNAME/CampcookingAR.git
git push -u origin main
```

### 日常工作流程

#### 1. 创建功能分支
```bash
git checkout -b feature/new-feature
```

#### 2. 开发和提交
```bash
git add .
git commit -m "feat: 添加新功能"
git push origin feature/new-feature
```

#### 3. 创建Pull Request
- 访问GitHub仓库
- 点击 "Pull requests" → "New pull request"
- 选择分支
- 填写PR模板
- 提交PR

#### 4. 代码审查
- 自动运行CI测试
- 等待审查批准
- 解决反馈意见

#### 5. 合并代码
- 审查通过后合并
- 自动关闭相关Issue

### 发布新版本

#### 1. 更新版本号
```kotlin
// app/build.gradle
defaultConfig {
    versionCode 2
    versionName "1.1.0"
}
```

#### 2. 更新CHANGELOG
```markdown
## [1.1.0] - 2025-01-15
### 新增
- 新功能描述
```

#### 3. 创建标签
```bash
git tag -a v1.1.0 -m "发布版本 1.1.0"
git push origin v1.1.0
```

#### 4. 自动发布
- GitHub Actions自动构建
- 自动创建Release
- 自动上传APK

---

## 📈 监控和统计

### GitHub Insights

**查看方式：**
```
仓库 → Insights
```

**可查看：**
- 贡献者统计
- 提交历史
- 代码频率
- 流量统计
- 依赖关系
- 社区标准

### Actions统计

**查看方式：**
```
仓库 → Actions
```

**可查看：**
- 工作流运行历史
- 构建成功率
- 运行时长
- 下载产物

---

## ✅ 检查清单

### 发布到GitHub前

- [ ] 已在GitHub创建仓库
- [ ] 已配置Git用户信息
- [ ] 已更新README中的用户名
- [ ] 已检查.gitignore配置
- [ ] 已测试本地构建成功
- [ ] 已阅读所有文档

### 首次推送后

- [ ] 检查CI构建状态
- [ ] 配置仓库Settings
- [ ] 添加Topics标签
- [ ] 设置分支保护
- [ ] 邀请协作者
- [ ] 配置Projects（可选）

### 日常维护

- [ ] 定期查看Issue
- [ ] 审查Pull Request
- [ ] 更新依赖
- [ ] 发布新版本
- [ ] 更新文档
- [ ] 响应安全警告

---

## 📚 相关文档

- [如何发布到GitHub.md](如何发布到GitHub.md) - 详细发布教程
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) - 行为准则
- [SECURITY.md](.github/SECURITY.md) - 安全政策
- [CHANGELOG.md](CHANGELOG.md) - 更新日志

---

## 🎯 最佳实践

### 提交规范
```bash
# 约定式提交
feat: 新功能
fix: Bug修复
docs: 文档更新
style: 代码格式
refactor: 代码重构
perf: 性能优化
test: 测试相关
chore: 构建/工具

# 示例
git commit -m "feat(splash): 添加封面动画效果"
git commit -m "fix(ui): 修复横屏显示问题"
```

### 分支策略
- `main` - 生产分支，始终可发布
- `develop` - 开发分支
- `feature/*` - 功能分支
- `bugfix/*` - 修复分支
- `release/*` - 发布分支
- `hotfix/*` - 紧急修复

### Issue管理
- 使用标签分类
- 关联相关Issue
- 及时更新状态
- 关闭时说明原因

### PR流程
- 小而专注的PR
- 完整的测试
- 清晰的描述
- 及时响应反馈

---

## 🔗 有用的链接

- [GitHub文档](https://docs.github.com)
- [GitHub Actions](https://docs.github.com/actions)
- [约定式提交](https://www.conventionalcommits.org/zh-hans/)
- [语义化版本](https://semver.org/lang/zh-CN/)
- [Keep a Changelog](https://keepachangelog.com/zh-CN/)

---

## ❓ 常见问题

### Q: 如何查看CI构建日志？
A: 仓库 → Actions → 选择工作流运行 → 查看详细日志

### Q: 如何自动发布Release？
A: 创建并推送版本标签，如 `git tag v1.0.0 && git push origin v1.0.0`

### Q: Dependabot PR如何处理？
A: 审查依赖更新，测试无问题后合并PR

### Q: 如何修改工作流配置？
A: 编辑 `.github/workflows/*.yml` 文件，推送后自动生效

### Q: Issue太多如何管理？
A: 使用标签分类，设置Milestones，启用Project看板

---

**🎉 恭喜！您已经掌握了项目的所有GitHub功能！**

如有疑问，请查看相关文档或创建Issue讨论。

