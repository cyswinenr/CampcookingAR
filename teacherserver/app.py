#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
教师端数据接收服务器
运行在笔记本上，接收学生端提交的数据，并提供教师端API接口
"""

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import json
import os
import socket
from datetime import datetime
from typing import Dict, List, Optional
import logging

from models import StudentDataPackage, TeacherEvaluation
from storage import DataStorage
from config import Config

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建Flask应用
app = Flask(__name__)
CORS(app)  # 允许跨域请求

# 初始化数据存储
storage = DataStorage(Config.DATA_DIR, Config.MEDIA_DIR)


@app.route('/api/status', methods=['GET'])
def get_status():
    """获取服务器状态"""
    try:
        student_count = storage.get_student_count()
        return jsonify({
            'status': 'running',
            'students': student_count,
            'timestamp': datetime.now().isoformat(),
            'server_ip': get_local_ip(),
            'port': Config.PORT
        }), 200
    except Exception as e:
        logger.error(f"获取状态失败: {str(e)}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/api/submit', methods=['POST'])
def submit_student_data():
    """接收学生端提交的数据"""
    try:
        # 获取JSON数据
        data = request.get_json()
        
        if not data:
            return jsonify({
                'status': 'error',
                'message': '未收到数据'
            }), 400
        
        # 验证数据格式
        if 'teamInfo' not in data:
            return jsonify({
                'status': 'error',
                'message': '缺少团队信息'
            }), 400
        
        # 解析数据包
        try:
            data_package = StudentDataPackage.from_dict(data)
        except Exception as e:
            logger.error(f"数据解析失败: {str(e)}")
            return jsonify({
                'status': 'error',
                'message': f'数据格式错误: {str(e)}'
            }), 400
        
        # 保存学生数据
        student_id = storage.save_student_data(data_package)
        
        logger.info(f"✅ 收到学生数据: {student_id}")
        
        return jsonify({
            'status': 'success',
            'studentId': student_id,
            'message': '数据接收成功'
        }), 200
        
    except Exception as e:
        logger.error(f"处理提交失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': f'服务器错误: {str(e)}'
        }), 500


@app.route('/api/students', methods=['GET'])
def get_students():
    """获取所有学生列表"""
    try:
        students = storage.get_all_students()
        
        # 转换为API格式
        result = []
        for student in students:
            result.append({
                'id': student['id'],
                'teamName': student['teamName'],
                'school': student['school'],
                'grade': student['grade'],
                'className': student['className'],
                'stoveNumber': student['stoveNumber'],
                'memberCount': student['memberCount'],
                'memberNames': student['memberNames'],
                'submitTime': student['submitTime'],
                'hasProcessRecord': student['hasProcessRecord'],
                'hasSummary': student['hasSummary'],
                'completedStages': student['completedStages'],
                'totalStages': student['totalStages']
            })
        
        return jsonify({
            'status': 'success',
            'students': result,
            'count': len(result)
        }), 200
        
    except Exception as e:
        logger.error(f"获取学生列表失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>', methods=['GET'])
def get_student_data(student_id: str):
    """获取指定学生的详细数据"""
    try:
        student_data = storage.get_student_data(student_id)
        
        if not student_data:
            return jsonify({
                'status': 'error',
                'message': '学生数据不存在'
            }), 404
        
        return jsonify({
            'status': 'success',
            'data': student_data
        }), 200
        
    except Exception as e:
        logger.error(f"获取学生数据失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/evaluation', methods=['GET'])
def get_student_evaluation(student_id: str):
    """获取指定学生的教师评价"""
    try:
        evaluation = storage.get_student_evaluation(student_id)
        
        return jsonify({
            'status': 'success',
            'evaluation': evaluation
        }), 200
        
    except Exception as e:
        logger.error(f"获取评价失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/evaluation', methods=['POST'])
def save_student_evaluation(student_id: str):
    """保存教师评价"""
    try:
        data = request.get_json()
        
        if not data or 'evaluation' not in data:
            return jsonify({
                'status': 'error',
                'message': '缺少评价数据'
            }), 400
        
        # 验证学生是否存在
        if not storage.student_exists(student_id):
            return jsonify({
                'status': 'error',
                'message': '学生数据不存在'
            }), 404
        
        # 解析评价数据
        evaluation = TeacherEvaluation.from_dict(data['evaluation'])
        
        # 保存评价
        storage.save_student_evaluation(student_id, evaluation)
        
        logger.info(f"✅ 保存评价: {student_id}")
        
        return jsonify({
            'status': 'success',
            'message': '评价保存成功'
        }), 200
        
    except Exception as e:
        logger.error(f"保存评价失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/media/<path:filename>', methods=['GET'])
def get_media_file(student_id: str, filename: str):
    """获取媒体文件（照片/视频）"""
    try:
        file_path = storage.get_media_file_path(student_id, filename)
        
        if not file_path or not os.path.exists(file_path):
            return jsonify({
                'status': 'error',
                'message': '文件不存在'
            }), 404
        
        return send_file(file_path)
        
    except Exception as e:
        logger.error(f"获取媒体文件失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/export', methods=['GET'])
def export_all_data():
    """导出所有数据为ZIP文件"""
    try:
        zip_path = storage.export_all_data()
        
        if not zip_path or not os.path.exists(zip_path):
            return jsonify({
                'status': 'error',
                'message': '导出失败'
            }), 500
        
        return send_file(zip_path, as_attachment=True, download_name=f'学生数据导出_{datetime.now().strftime("%Y%m%d_%H%M%S")}.zip')
        
    except Exception as e:
        logger.error(f"导出数据失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/statistics', methods=['GET'])
def get_statistics():
    """获取统计数据"""
    try:
        stats = storage.get_statistics()
        return jsonify({
            'status': 'success',
            'statistics': stats
        }), 200
        
    except Exception as e:
        logger.error(f"获取统计失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/', methods=['GET'])
def index():
    """Web管理界面（简单版本）"""
    try:
        student_count = storage.get_student_count()
        server_ip = get_local_ip()
        
        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>教师端数据管理</title>
            <style>
                body {{
                    font-family: Arial, sans-serif;
                    max-width: 1200px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f5f5f5;
                }}
                .header {{
                    background-color: #4CAF50;
                    color: white;
                    padding: 20px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                }}
                .info {{
                    background-color: white;
                    padding: 15px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }}
                .students-list {{
                    background-color: white;
                    padding: 15px;
                    border-radius: 5px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }}
                .student-item {{
                    padding: 10px;
                    border-bottom: 1px solid #eee;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }}
                .student-item:last-child {{
                    border-bottom: none;
                }}
                button {{
                    background-color: #4CAF50;
                    color: white;
                    border: none;
                    padding: 8px 16px;
                    border-radius: 4px;
                    cursor: pointer;
                }}
                button:hover {{
                    background-color: #45a049;
                }}
                .status {{
                    display: inline-block;
                    padding: 4px 8px;
                    border-radius: 4px;
                    font-size: 12px;
                }}
                .status.running {{
                    background-color: #4CAF50;
                    color: white;
                }}
            </style>
        </head>
        <body>
            <div class="header">
                <h1>🏕️ 野炊教学数据管理系统</h1>
            </div>
            
            <div class="info">
                <h2>服务器信息</h2>
                <p><strong>状态:</strong> <span class="status running">运行中</span></p>
                <p><strong>服务器地址:</strong> http://{server_ip}:{Config.PORT}</p>
                <p><strong>已接收学生数据:</strong> {student_count} 组</p>
                <p><strong>当前时间:</strong> {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
            </div>
            
            <div class="students-list">
                <h2>学生列表</h2>
                <p>使用教师端APP查看详细数据和进行评价</p>
                <p>API接口文档请查看 README.md</p>
            </div>
        </body>
        </html>
        """
        return html, 200
        
    except Exception as e:
        logger.error(f"生成首页失败: {str(e)}")
        return f"<h1>服务器错误</h1><p>{str(e)}</p>", 500


def get_local_ip():
    """获取本机IP地址"""
    try:
        # 连接到一个远程地址来获取本机IP
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        try:
            hostname = socket.gethostname()
            ip = socket.gethostbyname(hostname)
            return ip
        except Exception:
            return "127.0.0.1"


def main():
    """启动服务器"""
    # 确保数据目录存在
    os.makedirs(Config.DATA_DIR, exist_ok=True)
    os.makedirs(Config.MEDIA_DIR, exist_ok=True)
    
    # 获取本机IP
    server_ip = get_local_ip()
    
    print("=" * 60)
    print("🏕️  野炊教学数据管理系统 - 教师端服务器")
    print("=" * 60)
    print(f"服务器地址: http://{server_ip}:{Config.PORT}")
    print(f"Web管理界面: http://{server_ip}:{Config.PORT}/")
    print(f"API状态查询: http://{server_ip}:{Config.PORT}/api/status")
    print("=" * 60)
    print("等待学生端提交数据...")
    print("按 Ctrl+C 停止服务器")
    print("=" * 60)
    
    # 启动Flask服务器
    app.run(
        host='0.0.0.0',  # 允许局域网访问
        port=Config.PORT,
        debug=Config.DEBUG,
        threaded=True
    )


if __name__ == '__main__':
    main()

