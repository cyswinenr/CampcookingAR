# -*- mode: python ; coding: utf-8 -*-
"""
PyInstaller 打包配置文件
用于将教师端服务器打包成 exe 文件
"""

import os
import sys
from PyInstaller.utils.hooks import collect_data_files, collect_submodules

# 获取当前目录（spec 文件所在目录）
# spec 文件在 teacherserver 目录下
try:
    # PyInstaller 会自动设置 SPECPATH 变量，指向 spec 文件的完整路径
    spec_file_path = os.path.abspath(SPECPATH)
    # spec 文件路径应该是: .../teacherserver/build_exe.spec
    # 所以 spec_dir 应该是 teacherserver 目录
    spec_dir = os.path.dirname(spec_file_path)
except NameError:
    # 如果 SPECPATH 不存在，使用当前工作目录
    spec_dir = os.getcwd()

# 验证并修正路径
# 如果 spec_dir 是项目根目录（没有 app.py），需要进入 teacherserver 子目录
app_py_path = os.path.join(spec_dir, 'app.py')
if not os.path.exists(app_py_path):
    # 尝试在 teacherserver 子目录查找
    teacherserver_dir = os.path.join(spec_dir, 'teacherserver')
    teacherserver_app_py = os.path.join(teacherserver_dir, 'app.py')
    if os.path.exists(teacherserver_app_py):
        spec_dir = teacherserver_dir
        app_py_path = teacherserver_app_py
    else:
        # 如果都不存在，尝试使用当前工作目录（假设在 teacherserver 目录下运行）
        cwd = os.getcwd()
        cwd_app_py = os.path.join(cwd, 'app.py')
        if os.path.exists(cwd_app_py):
            spec_dir = cwd
            app_py_path = cwd_app_py
        else:
            raise FileNotFoundError(
                f'找不到 app.py 文件。\n'
                f'已尝试路径:\n'
                f'  1. {os.path.join(spec_dir, "app.py")}\n'
                f'  2. {teacherserver_app_py}\n'
                f'  3. {cwd_app_py}\n'
                f'请确保在 teacherserver 目录下运行打包脚本。'
            )

# 收集所有需要的数据文件
# 注意：只打包模板文件，不打包数据目录（数据目录会在运行时创建）
templates_path = os.path.join(spec_dir, 'templates')
if os.path.exists(templates_path):
    datas = [
        (templates_path, 'templates'),  # HTML 模板文件
    ]
    print(f"[DEBUG] 找到模板目录: {templates_path}")
else:
    datas = []
    print(f"[DEBUG] 警告: 未找到模板目录: {templates_path}")

# 收集 Flask 相关的隐藏导入
hiddenimports = [
    'flask',
    'flask_cors',
    'werkzeug',
    'jinja2',
    'sqlite3',
    'json',
    'logging',
    'datetime',
    'threading',
    'random',
    'time',
]

# 添加所有 Python 模块
hiddenimports += collect_submodules('flask')
hiddenimports += collect_submodules('werkzeug')
hiddenimports += collect_submodules('jinja2')

# 定义 block_cipher（用于加密，可选，设为 None 表示不加密）
block_cipher = None

# app_py_path 已经在上面确定，这里验证一下
if not os.path.exists(app_py_path):
    raise FileNotFoundError(f'找不到 app.py 文件，当前查找路径: {app_py_path}')

a = Analysis(
    [app_py_path],
    pathex=[spec_dir],
    binaries=[],
    datas=datas,
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='教师端服务器',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,  # 显示控制台窗口
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,  # 可以添加图标文件路径
)

