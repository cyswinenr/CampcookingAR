@echo off
chcp 65001 >nul
echo ============================================================
echo 教师端服务器 - 打包成 EXE 文件
echo ============================================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Python，请先安装Python 3.7或更高版本
    pause
    exit /b 1
)

echo 步骤 1: 安装依赖包...
echo.
pip install -r requirements.txt
if errorlevel 1 (
    echo [错误] 依赖安装失败
    pause
    exit /b 1
)

echo.
echo 步骤 2: 安装 PyInstaller...
echo.
pip install pyinstaller
if errorlevel 1 (
    echo [错误] PyInstaller 安装失败
    pause
    exit /b 1
)

echo.
echo 步骤 3: 清理之前的打包文件...
echo.
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
if exist "教师端服务器.spec" del /q "教师端服务器.spec"

echo.
echo 步骤 4: 开始打包...
echo.
echo 当前目录: %CD%
echo 确保在 teacherserver 目录下运行此脚本
echo.
REM 确保在正确的目录下运行 PyInstaller
cd /d "%~dp0"
pyinstaller build_exe.spec

if errorlevel 1 (
    echo.
    echo [错误] 打包失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo 步骤 5: 复制模板文件...
echo.
if exist "dist\templates" rmdir /s /q "dist\templates"
xcopy /E /I /Y "templates" "dist\templates"

if not exist "dist\templates\index.html" (
    echo [警告] 模板文件复制失败，请手动复制 templates 文件夹到 dist 目录
)

echo.
echo ============================================================
echo ✅ 打包完成！
echo ============================================================
echo.
echo 打包文件位置: dist\教师端服务器.exe
echo 模板文件夹: dist\templates\
echo.
echo 使用说明:
echo 1. 将 dist\教师端服务器.exe 和 dist\templates\ 文件夹复制到目标电脑
echo 2. 确保 templates 文件夹与 exe 文件在同一目录
echo 3. 首次运行会自动创建 data 目录和数据库
echo 4. 双击运行即可启动服务器
echo.
echo 注意: 首次运行时，请确保:
echo - 防火墙允许程序访问网络
echo - 端口 5000 未被占用
echo.
pause

