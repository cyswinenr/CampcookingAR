@echo off
chcp 65001 >nul
echo ============================================================
echo 野炊教学数据管理系统 - 教师端服务器
echo ============================================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Python，请先安装Python 3.7或更高版本
    pause
    exit /b 1
)

REM 检查依赖是否安装
echo 检查依赖包...
python -c "import flask" >nul 2>&1
if errorlevel 1 (
    echo 正在安装依赖包...
    pip install -r requirements.txt
    if errorlevel 1 (
        echo [错误] 依赖安装失败
        pause
        exit /b 1
    )
)

echo.
echo 启动服务器...
echo.
python app.py

pause

