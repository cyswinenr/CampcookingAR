#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
配置文件
"""

import os
import sys

def get_base_dir():
    """获取应用基础目录（支持打包后的环境）"""
    if getattr(sys, 'frozen', False):
        # 打包后的环境（PyInstaller）
        # exe 文件所在目录作为基础目录
        base_dir = os.path.dirname(sys.executable)
    else:
        # 开发环境
        base_dir = os.path.dirname(os.path.abspath(__file__))
    return base_dir

class Config:
    """服务器配置"""
    
    # 服务器配置
    PORT = 5000  # 服务器端口
    DEBUG = False  # 调试模式（生产环境设为False）
    HOST = '0.0.0.0'  # 监听所有网络接口
    
    # 数据存储配置
    BASE_DIR = get_base_dir()
    DATA_DIR = os.path.join(BASE_DIR, 'data', 'students')  # 学生数据目录
    MEDIA_DIR = os.path.join(BASE_DIR, 'data', 'media')  # 媒体文件目录
    EVALUATION_DIR = os.path.join(BASE_DIR, 'data', 'evaluations')  # 评价数据目录
    EXPORT_DIR = os.path.join(BASE_DIR, 'data', 'exports')  # 导出文件目录
    DATABASE_PATH = os.path.join(BASE_DIR, 'data', 'campcooking.db')  # SQLite数据库路径
    
    # 允许的文件类型
    ALLOWED_IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif', '.bmp'}
    ALLOWED_VIDEO_EXTENSIONS = {'.mp4', '.avi', '.mov', '.mkv'}
    
    # 最大文件大小（字节）
    MAX_FILE_SIZE = 100 * 1024 * 1024  # 100MB
    
    # API配置
    CORS_ORIGINS = ['*']  # 允许的跨域来源（生产环境应限制具体域名）
    
    # 清空数据库密码配置
    CLEAR_DATABASE_PASSWORD = '81438316'  # 清空数据库所需的密码

