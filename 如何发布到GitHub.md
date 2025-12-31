# 📦 如何将项目发布到GitHub

本指南将帮助您将"南粤风炊火"项目发布到GitHub。

## 📋 准备工作

### 1. 创建GitHub账号
如果还没有GitHub账号，请访问 https://github.com 注册。

### 2. 安装Git
- Windows: 下载并安装 https://git-scm.com/download/win
- Mac: 使用 `brew install git` 或从官网下载
- Linux: `sudo apt-get install git` (Ubuntu/Debian)

### 3. 配置Git
```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"
```

## 🚀 发布步骤

### 方法一：使用GitHub Desktop（推荐新手）

1. **下载GitHub Desktop**
   - 访问 https://desktop.github.com/ 下载

2. **登录GitHub账号**
   - 打开GitHub Desktop
   - File → Options → Sign in

3. **发布仓库**
   - File → Add Local Repository
   - 选择项目文件夹 `CampcookingAR`
   - 点击 "Publish repository"
   - 填写仓库信息
   - 点击 "Publish"

### 方法二：使用命令行

#### 步骤1: 在GitHub上创建仓库

1. 登录GitHub
2. 点击右上角 "+" → "New repository"
3. 填写信息：
   - Repository name: `CampcookingAR`
   - Description: `野炊教学AR应用 - 专为10-11寸Android平板设计`
   - 选择 Public 或 Private
   - **不要**勾选 "Initialize this repository with a README"
4. 点击 "Create repository"

#### 步骤2: 初始化本地仓库

打开命令行，进入项目目录：

```bash
cd O:\PadSoftware\CampcookingAR
```

初始化Git仓库：

```bash
# 初始化Git仓库
git init

# 添加所有文件
git add .

# 提交初始版本
git commit -m "feat: 初始提交 - 完成封面页开发"

# 设置主分支名称为main
git branch -M main
```

#### 步骤3: 关联远程仓库

```bash
# 替换YOUR_USERNAME为你的GitHub用户名
git remote add origin https://github.com/YOUR_USERNAME/CampcookingAR.git

# 推送到GitHub
git push -u origin main
```

### 方法三：使用Android Studio内置Git

1. **启用版本控制**
   - VCS → Enable Version Control Integration
   - 选择 Git → OK

2. **提交文件**
   - VCS → Commit
   - 选择要提交的文件
   - 填写提交信息
   - 点击 "Commit"

3. **分享到GitHub**
   - VCS → Import into Version Control → Share Project on GitHub
   - 登录GitHub账号
   - 填写仓库信息
   - 点击 "Share"

## 📝 发布后的配置

### 1. 更新README文件

将README.md中的占位符替换为实际信息：

```markdown
# 替换以下内容：
YOUR_USERNAME → 你的GitHub用户名
your.email@example.com → 你的邮箱
```

### 2. 配置GitHub Pages（可选）

如果要展示项目文档：

1. 进入仓库 Settings
2. 找到 "Pages" 部分
3. Source 选择 "main" 分支
4. 点击 "Save"

### 3. 添加Topics

为仓库添加标签以便他人发现：

1. 进入仓库主页
2. 点击 "Add topics"
3. 添加：`android`, `kotlin`, `ar`, `cooking`, `education`, `tablet`, `landscape`

### 4. 保护主分支

1. Settings → Branches
2. 添加分支保护规则
3. 勾选：
   - Require pull request reviews before merging
   - Require status checks to pass before merging

## 🔄 日常更新流程

### 提交更改

```bash
# 查看更改
git status

# 添加更改的文件
git add .

# 或添加特定文件
git add app/src/main/java/com/campcooking/ar/MainActivity.kt

# 提交更改
git commit -m "feat: 添加主页导航功能"

# 推送到GitHub
git push
```

### 提交信息规范

使用约定式提交格式：

```bash
git commit -m "类型: 简短描述"

# 示例：
git commit -m "feat: 添加AR相机功能"
git commit -m "fix: 修复横屏显示问题"
git commit -m "docs: 更新README文档"
git commit -m "style: 优化代码格式"
git commit -m "refactor: 重构封面页逻辑"
git commit -m "perf: 优化启动速度"
git commit -m "test: 添加单元测试"
```

## 🌿 分支管理

### 创建功能分支

```bash
# 创建并切换到新分支
git checkout -b feature/ar-camera

# 在新分支上开发
# ... 进行代码修改 ...

# 提交更改
git add .
git commit -m "feat: 实现AR相机基础功能"

# 推送到GitHub
git push -u origin feature/ar-camera
```

### 创建Pull Request

1. 访问GitHub仓库页面
2. 点击 "Pull requests" → "New pull request"
3. 选择要合并的分支
4. 填写PR描述
5. 点击 "Create pull request"
6. 等待审核和合并

## 📦 创建Release

### 通过GitHub界面

1. 点击 "Releases" → "Create a new release"
2. 点击 "Choose a tag" → 输入版本号（如 `v1.0.0`）
3. 填写Release信息：
   ```markdown
   ## 南粤风炊火 v1.0.0
   
   ### 新功能
   - ✨ 精美封面启动页
   - 📱 横屏平板优化
   - 🎬 流畅动画效果
   
   ### 设备要求
   - Android 7.0+
   - 10-11寸平板
   - 横屏使用
   ```
4. 上传APK文件（从 `app/build/outputs/apk/release/`）
5. 点击 "Publish release"

### 通过命令行

```bash
# 创建标签
git tag -a v1.0.0 -m "发布版本 1.0.0"

# 推送标签到GitHub
git push origin v1.0.0

# 然后在GitHub上创建Release
```

## 🔧 GitHub Actions配置

项目已包含CI/CD配置，自动化构建会在以下情况触发：

- 推送到 `main` 或 `develop` 分支
- 创建Pull Request
- 创建新标签（触发Release构建）

查看构建状态：
- 进入仓库 → Actions 标签页

## 📊 项目统计

### 添加徽章

在README.md中添加状态徽章（已包含）：

```markdown
[![Android CI](https://github.com/YOUR_USERNAME/CampcookingAR/workflows/Android%20CI/badge.svg)](https://github.com/YOUR_USERNAME/CampcookingAR/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
```

### 启用Insights

GitHub自动提供项目统计：
- 进入仓库 → Insights
- 查看贡献者、提交历史、流量等

## 🎯 最佳实践

### 1. 定期提交
```bash
# 每完成一个小功能就提交
git add .
git commit -m "feat: 完成XX功能"
git push
```

### 2. 写好提交信息
- 使用约定式提交格式
- 简短但描述清楚
- 必要时添加详细说明

### 3. 使用分支
- `main` - 稳定版本
- `develop` - 开发版本
- `feature/*` - 新功能
- `bugfix/*` - Bug修复

### 4. 代码审查
- 重要功能通过PR合并
- 至少一人审核
- 通过CI检查后合并

### 5. 版本管理
- 遵循语义化版本
- 每个版本创建Release
- 附上详细的更新说明

## ❓ 常见问题

### Q1: 推送时要求输入用户名密码？

使用Personal Access Token：

1. GitHub → Settings → Developer settings → Personal access tokens
2. Generate new token → 勾选 `repo` 权限
3. 复制生成的token
4. 推送时使用token作为密码

### Q2: 文件太大无法推送？

检查是否包含了不应该提交的文件：

```bash
# 查看大文件
git rev-list --objects --all | git cat-file --batch-check='%(objectsize) %(rest)' | sort -rn | head

# 删除大文件并重新提交
git rm --cached 大文件路径
echo "大文件路径" >> .gitignore
git commit -m "chore: 删除大文件"
```

### Q3: 如何撤销提交？

```bash
# 撤销最后一次提交，保留更改
git reset --soft HEAD~1

# 撤销最后一次提交，丢弃更改
git reset --hard HEAD~1

# 如果已经推送，需要强制推送（慎用）
git push -f origin main
```

## 📚 更多资源

- [Git官方文档](https://git-scm.com/doc)
- [GitHub文档](https://docs.github.com)
- [Git速查表](https://training.github.com/downloads/zh_CN/github-git-cheat-sheet/)
- [约定式提交](https://www.conventionalcommits.org/zh-hans/)

## ✅ 检查清单

发布前确认：

- [ ] 所有代码已提交
- [ ] .gitignore配置正确
- [ ] README.md信息完整
- [ ] LICENSE文件存在
- [ ] 构建成功无错误
- [ ] 更新了版本号
- [ ] 添加了必要的文档

---

**祝您发布顺利！** 🎉

如有问题，请参考 [GitHub Docs](https://docs.github.com) 或在Issues中提问。

