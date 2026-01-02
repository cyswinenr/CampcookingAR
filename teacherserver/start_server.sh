#!/bin/bash
# 野炊教学数据管理系统 - 教师端服务器启动脚本（Linux/Mac）

echo "============================================================"
echo "野炊教学数据管理系统 - 教师端服务器"
echo "============================================================"
echo ""

# 检查Python是否安装
if ! command -v python3 &> /dev/null; then
    echo "[错误] 未检测到Python，请先安装Python 3.7或更高版本"
    exit 1
fi

# 检查依赖是否安装
echo "检查依赖包..."
if ! python3 -c "import flask" &> /dev/null; then
    echo "正在安装依赖包..."
    pip3 install -r requirements.txt
    if [ $? -ne 0 ]; then
        echo "[错误] 依赖安装失败"
        exit 1
    fi
fi

echo ""
echo "启动服务器..."
echo ""
python3 app.py

