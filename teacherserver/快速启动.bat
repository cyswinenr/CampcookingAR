@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo 正在启动服务器...
echo.

REM 检查Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未安装Python，请先安装Python 3.7+
    pause
    exit /b 1
)

REM 检查并安装依赖
python -c "import flask" >nul 2>&1
if errorlevel 1 (
    echo 正在安装依赖包...
    pip install -r requirements.txt
)

REM 启动服务器
python app.py

pause

