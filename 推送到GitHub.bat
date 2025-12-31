@echo off
chcp 65001 >nul
echo ========================================
echo   南粤风炊火 - 推送到 GitHub
echo ========================================
echo.

REM 检查是否已初始化 Git
if not exist ".git" (
    echo ⚠️  Git 仓库未初始化！
    echo 正在运行初始化脚本...
    call "初始化Git仓库.bat"
    if errorlevel 1 exit /b 1
)

echo 请输入你的 GitHub 用户名：
set /p USERNAME=用户名: 

if "%USERNAME%"=="" (
    echo ❌ 用户名不能为空！
    pause
    exit /b 1
)

echo.
echo 正在配置远程仓库...
git remote remove origin 2>nul
git remote add origin https://github.com/%USERNAME%/CampcookingAR.git

echo.
echo 正在推送到 GitHub...
echo 注意：首次推送可能需要登录 GitHub 账号
echo.

git push -u origin main

if errorlevel 1 (
    echo.
    echo ❌ 推送失败！
    echo.
    echo 可能的原因：
    echo 1. GitHub 仓库不存在 - 请先在 GitHub 创建 'CampcookingAR' 仓库
    echo 2. 认证失败 - 请检查 GitHub 用户名和密码
    echo 3. 网络问题 - 请检查网络连接
    echo.
    echo 详细步骤请查看：如何发布到GitHub.md
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ 代码已成功推送到 GitHub！
echo.
echo 🌐 仓库地址：https://github.com/%USERNAME%/CampcookingAR
echo.
echo 后续更新代码请使用：
echo    git add .
echo    git commit -m "提交说明"
echo    git push
echo.
pause

