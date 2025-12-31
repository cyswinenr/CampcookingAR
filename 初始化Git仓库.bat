@echo off
chcp 65001 >nul
echo ========================================
echo   南粤风炊火 - Git 仓库初始化脚本
echo ========================================
echo.

echo [1/5] 初始化 Git 仓库...
git init
if errorlevel 1 (
    echo ❌ Git 初始化失败！请确保已安装 Git。
    pause
    exit /b 1
)
echo ✓ Git 仓库初始化成功

echo.
echo [2/5] 添加所有文件到暂存区...
git add .
if errorlevel 1 (
    echo ❌ 添加文件失败！
    pause
    exit /b 1
)
echo ✓ 文件添加成功

echo.
echo [3/5] 创建初始提交...
git commit -m "feat: 初始提交 - 完成封面页开发

- ✨ 实现精美的封面启动页
- 📱 优化横屏平板显示
- 🎬 添加流畅动画效果
- 📚 完善项目文档
- 🔧 配置 GitHub 工作流"
if errorlevel 1 (
    echo ❌ 提交失败！
    pause
    exit /b 1
)
echo ✓ 初始提交创建成功

echo.
echo [4/5] 设置主分支名称为 main...
git branch -M main
echo ✓ 分支设置成功

echo.
echo [5/5] 下一步操作...
echo.
echo 请按照以下步骤将代码推送到 GitHub：
echo.
echo 1. 在 GitHub 上创建新仓库 'CampcookingAR'
echo 2. 运行以下命令（替换 YOUR_USERNAME 为你的 GitHub 用户名）：
echo.
echo    git remote add origin https://github.com/YOUR_USERNAME/CampcookingAR.git
echo    git push -u origin main
echo.
echo 3. 或者直接运行 '推送到GitHub.bat' 脚本
echo.
echo ✅ Git 仓库初始化完成！
echo.
echo 详细说明请查看：如何发布到GitHub.md
echo.
pause

