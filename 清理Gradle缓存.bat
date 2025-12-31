@echo off
chcp 65001 >nul
echo ============================================
echo   清理 Gradle 缓存
echo ============================================
echo.

echo [1/3] 删除项目 .gradle 文件夹...
if exist ".gradle" (
    rmdir /s /q ".gradle"
    echo ✓ 已删除项目 .gradle 文件夹
) else (
    echo - 项目 .gradle 文件夹不存在
)
echo.

echo [2/3] 删除 app/build 文件夹...
if exist "app\build" (
    rmdir /s /q "app\build"
    echo ✓ 已删除 app/build 文件夹
) else (
    echo - app/build 文件夹不存在
)
echo.

echo [3/3] 删除 .idea 缓存文件夹...
if exist ".idea" (
    rmdir /s /q ".idea"
    echo ✓ 已删除 .idea 文件夹
) else (
    echo - .idea 文件夹不存在
)
echo.

echo ============================================
echo   清理完成！
echo ============================================
echo.
echo 下一步操作：
echo 1. 重新打开 Android Studio
echo 2. 打开此项目
echo 3. 等待 Gradle 同步
echo.
echo 按任意键退出...
pause >nul

