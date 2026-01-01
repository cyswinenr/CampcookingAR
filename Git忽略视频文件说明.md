# 🚫 Git 忽略视频文件配置说明

## ✅ 已配置完成

视频文件已经添加到 `.gitignore`，**不会被同步到 GitHub**！

---

## 📋 忽略的文件类型

### 视频格式（全部忽略）
```
*.mp4      ← MP4 视频
*.avi      ← AVI 视频
*.mov      ← MOV 视频
*.wmv      ← WMV 视频
*.flv      ← FLV 视频
*.mkv      ← MKV 视频
*.webm     ← WebM 视频
*.m4v      ← M4V 视频
*.3gp      ← 3GP 视频
*.mpg      ← MPG 视频
*.mpeg     ← MPEG 视频
```

### 特定路径（确保忽略）
```
app/src/main/res/raw/*.mp4          ← raw 文件夹中的所有视频
app/src/main/res/raw/*.avi
app/src/main/res/raw/*.mov
... (所有视频格式)
```

---

## 🔍 如何验证视频没有被同步

### 方法 1：使用 Git Status 命令

在项目根目录打开终端（PowerShell），运行：

```powershell
git status
```

**预期结果：**
- ✅ 应该**看不到**任何 `.mp4` 文件
- ✅ 应该**看不到** `app/src/main/res/raw/` 下的视频文件

**如果看到视频文件：**
```
Changes not staged for commit:
  modified:   app/src/main/res/raw/video_fire_skills.mp4  ← 不应该出现
```
说明配置有问题，需要检查 `.gitignore`。

---

### 方法 2：使用 Git GUI

在 Android Studio 中：

1. 打开 **Git** 窗口（Alt + 9）
2. 查看 **Unversioned Files**（未版本控制的文件）
3. ✅ 视频文件应该**不在列表中**

---

### 方法 3：测试添加视频文件

1. **添加一个测试视频**
   ```
   复制任意 MP4 文件到：
   app/src/main/res/raw/test_video.mp4
   ```

2. **检查 Git 状态**
   ```powershell
   git status
   ```

3. **预期结果**
   - ✅ `test_video.mp4` **不会出现**在 Git 状态中
   - ✅ 说明 `.gitignore` 配置成功

4. **清理测试文件**
   ```
   删除 test_video.mp4
   ```

---

## 📝 .gitignore 配置内容

已在 `.gitignore` 中添加：

```gitignore
# =================================================================
# 视频文件（不同步到 GitHub，文件太大）
# =================================================================

# 所有视频文件格式
*.mp4
*.avi
*.mov
*.wmv
*.flv
*.mkv
*.webm
*.m4v
*.3gp
*.mpg
*.mpeg

# 特别指定 res/raw/ 中的视频文件（确保不会同步）
app/src/main/res/raw/*.mp4
app/src/main/res/raw/*.avi
app/src/main/res/raw/*.mov
app/src/main/res/raw/*.wmv
app/src/main/res/raw/*.flv
app/src/main/res/raw/*.mkv
app/src/main/res/raw/*.webm

# 外部存储位置的视频（如果以后使用）
Documents/CampcookingAR/Videos/*.mp4

# =================================================================
```

---

## ⚠️ 如果视频文件已经被提交到 Git

### 问题：之前已经提交了视频文件

如果视频文件之前已经被提交到 Git，即使现在添加到 `.gitignore`，也不会自动删除。

### 解决方法：从 Git 中移除（但保留本地文件）

```powershell
# 移除 raw 文件夹中的所有视频
git rm --cached app/src/main/res/raw/*.mp4
git rm --cached app/src/main/res/raw/*.avi
git rm --cached app/src/main/res/raw/*.mov

# 或者移除所有 .mp4 文件
git rm --cached -r *.mp4

# 提交更改
git commit -m "从版本控制中移除视频文件"

# 推送到远程仓库
git push
```

**说明：**
- `--cached` 参数：只从 Git 删除，本地文件保留
- 视频文件依然在你的电脑上
- 但不会再上传到 GitHub

---

## 🎯 同步流程（无视频文件）

### 正常的 Git 工作流程

```powershell
# 1. 查看状态
git status

# 2. 添加修改的代码文件（视频文件自动忽略）
git add .

# 3. 提交
git commit -m "更新功能"

# 4. 推送
git push
```

### 视频文件不会被包含

```
✅ 会同步：
- .kt 代码文件
- .xml 布局文件
- .png 图片文件
- .md 文档文件
- 其他项目文件

❌ 不会同步：
- *.mp4 视频文件
- *.avi 视频文件
- 其他视频格式
```

---

## 📊 文件大小对比

### 不忽略视频的情况

```
项目大小：
代码和资源: 5 MB
视频文件: 200 MB (假设 3 个视频，每个约 60-70 MB)
总计: 205 MB

同步时间：
- 首次 clone: 约 2-5 分钟（取决于网速）
- 每次 pull: 如果有视频更新，需要重新下载
```

### 忽略视频的情况（当前配置）✅

```
项目大小：
代码和资源: 5 MB
视频文件: 0 MB (不同步)
总计: 5 MB

同步时间：
- 首次 clone: 约 5-10 秒
- 每次 pull: 几秒钟
```

**节省：** 约 97.5% 的存储空间和同步时间！

---

## 💡 团队协作建议

### 如果有团队成员需要视频文件

#### 方案 1：单独分享视频（推荐）

```
1. 将视频文件打包（ZIP）
2. 通过网盘、U盘等分享
3. 团队成员解压到：
   app/src/main/res/raw/
```

#### 方案 2：使用 Git LFS（Git Large File Storage）

```powershell
# 安装 Git LFS
git lfs install

# 跟踪视频文件
git lfs track "*.mp4"

# 添加 .gitattributes
git add .gitattributes

# 正常提交
git add app/src/main/res/raw/*.mp4
git commit -m "使用 Git LFS 添加视频"
git push
```

**注意：** Git LFS 需要额外配置，且 GitHub 有存储限制。

#### 方案 3：使用云存储链接

在 `app/src/main/res/raw/README.md` 中添加下载链接：

```markdown
## 视频文件下载

由于文件较大，视频文件未包含在仓库中。

请从以下位置下载：
- 网盘链接：https://pan.baidu.com/...
- 提取码：xxxx

下载后放入：app/src/main/res/raw/
```

---

## 🔧 常见问题

### Q1：我添加了视频文件，但 Git 还是检测到了？

**可能原因：**
1. `.gitignore` 配置错误
2. 视频文件之前已经被提交

**解决方法：**
```powershell
# 检查 .gitignore 是否生效
git check-ignore -v app/src/main/res/raw/video_fire_skills.mp4

# 应该输出：
# .gitignore:89:*.mp4    app/src/main/res/raw/video_fire_skills.mp4

# 如果没有输出，说明没有被忽略，需要检查 .gitignore
```

### Q2：视频文件已经在远程仓库了，如何删除？

**步骤：**
```powershell
# 1. 从 Git 中移除（保留本地）
git rm --cached app/src/main/res/raw/*.mp4

# 2. 提交
git commit -m "移除视频文件"

# 3. 推送
git push

# 4. 清理历史记录（可选，需要 BFG Repo-Cleaner）
# 这会从整个历史中删除视频文件
```

### Q3：团队成员 clone 后没有视频文件怎么办？

**解决方法：**

**方式 1：** 提供视频文件包
```
1. 你打包视频文件
2. 分享给团队成员
3. 他们解压到 app/src/main/res/raw/
```

**方式 2：** 在 README 中说明
```markdown
## 首次设置

本项目的视频文件未包含在仓库中，请：

1. 从 [网盘链接] 下载视频文件
2. 解压到：app/src/main/res/raw/
3. 确保文件名正确：
   - video_fire_skills.mp4
   - video_cut_chicken.mp4
   - video_chop_ribs.mp4
```

### Q4：.gitignore 不生效？

**可能原因：** Git 缓存

**解决方法：**
```powershell
# 清除 Git 缓存
git rm -r --cached .

# 重新添加所有文件
git add .

# 提交
git commit -m "重新应用 .gitignore"
```

---

## ✅ 验证清单

完成以下检查，确保配置正确：

- [ ] `.gitignore` 中已添加视频文件规则
- [ ] 运行 `git status` 不显示视频文件
- [ ] 添加测试视频，`git status` 依然不显示
- [ ] 如果之前提交过视频，已使用 `git rm --cached` 移除
- [ ] 团队成员知道如何获取视频文件

---

## 📝 快速命令参考

```powershell
# 检查 Git 状态（不应显示视频）
git status

# 检查 .gitignore 是否生效
git check-ignore -v app/src/main/res/raw/*.mp4

# 移除已提交的视频（保留本地）
git rm --cached app/src/main/res/raw/*.mp4

# 清除 Git 缓存（如果 .gitignore 不生效）
git rm -r --cached .
git add .
git commit -m "重新应用 .gitignore"

# 查看哪些文件会被提交
git add . --dry-run
```

---

## 🎉 总结

✅ **视频文件已配置为不同步**

现在你可以：
- ✅ 随意添加视频到 `app/src/main/res/raw/`
- ✅ Git 不会跟踪这些视频文件
- ✅ 同步速度超快（只同步代码）
- ✅ 节省 GitHub 存储空间
- ✅ 本地依然有视频文件可以运行

---

**配置日期：** 2026-01-01  
**状态：** ✅ 已生效  
**验证：** 运行 `git status` 确认

视频文件不会再上传到 GitHub 了！🚀

