#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
教师端数据接收服务器
运行在笔记本上，接收学生端提交的数据，并提供教师端API接口
"""

from flask import Flask, request, jsonify, send_file, render_template_string
from flask_cors import CORS
import json
import os
import socket
import shutil
from datetime import datetime
from typing import Dict, List, Optional
import logging

from models import StudentDataPackage, TeacherEvaluation
from storage import DataStorage
from config import Config
import sqlite3

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
        
        # ⭐ 关键修复：立即检查并保存原始 JSON 数据到文件
        try:
            # 生成学生ID
            team_info = data.get('teamInfo', {})
            student_id = f"{team_info.get('school', '')}_{team_info.get('grade', '')}_{team_info.get('className', '')}_{team_info.get('stoveNumber', '')}"
            
            # 立即检查接收到的数据
            logger.info("=" * 60)
            logger.info("收到学生数据提交")
            logger.info(f"学生ID: {student_id}")
            logger.info(f"数据键: {list(data.keys())}")
            
            process_record = data.get('processRecord')
            if process_record:
                logger.info(f"✅ processRecord 存在")
                logger.info(f"   processRecord 的键: {list(process_record.keys())}")
                has_stages = 'stages' in process_record
                logger.info(f"   包含 stages 字段: {has_stages}")
                if has_stages:
                    stages = process_record.get('stages', {})
                    logger.info(f"   stages 数量: {len(stages)}")
                    total_media = 0
                    for stage_name, stage_data in stages.items():
                        media_items = stage_data.get('mediaItems', [])
                        if not media_items:
                            media_items = stage_data.get('media_items', [])
                        media_count = len(media_items) if media_items else 0
                        total_media += media_count
                        if media_count > 0:
                            logger.info(f"      阶段 {stage_name}: {media_count} 个媒体文件")
                    logger.info(f"   总计: {total_media} 个媒体文件")
                else:
                    logger.warning(f"   ⚠️ processRecord 中没有 stages 字段！")
            else:
                logger.warning("⚠️ processRecord 不存在")
            logger.info("=" * 60)
            
            # 创建学生数据目录
            student_dir = os.path.join(Config.DATA_DIR, student_id)
            os.makedirs(student_dir, exist_ok=True)
            
            # 保存原始 JSON 数据（带时间戳）
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            json_filename = f'data_{timestamp}.json'
            json_path = os.path.join(student_dir, json_filename)
            
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            # 同时保存为 latest.json（覆盖）
            latest_json_path = os.path.join(student_dir, 'latest.json')
            with open(latest_json_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            logger.info(f"✅ 已保存原始 JSON 数据: {json_path}")
            logger.info(f"   包含 stages: {'stages' in data.get('processRecord', {})}")
            
        except Exception as e:
            logger.error(f"保存 JSON 文件失败: {str(e)}", exc_info=True)
            # 继续处理，不中断流程
        
        # 解析数据包
        try:
            data_package = StudentDataPackage.from_dict(data)
        except Exception as e:
            logger.error(f"数据解析失败: {str(e)}")
            return jsonify({
                'status': 'error',
                'message': f'数据格式错误: {str(e)}'
            }), 400
        
        # 保存学生数据到数据库
        student_id = storage.save_student_data(data_package)
        
        # 记录分工信息（如果有）
        if data_package.teamDivision:
            logger.info(f"✅ 收到学生数据: {student_id}, 包含分工信息")
            try:
                # 使用 to_android_dict() 避免 team_id 问题
                division_info = data_package.teamDivision.to_android_dict()
                logger.info(f"   分工详情: {division_info}")
            except Exception as e:
                logger.warning(f"   记录分工信息失败: {str(e)}")
        else:
            logger.info(f"✅ 收到学生数据: {student_id}, 无分工信息")
        
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
        
        # 转换为API格式（已经按炉号排序）
        result = []
        for student in students:
            stage_ratings = student.get('stageRatings', {})
            # 调试：记录评分数据
            if stage_ratings:
                logger.debug(f"学生 {student['id']} 的评分数据: {stage_ratings}")
            
            result.append({
                'id': student['id'],
                'teamName': student['teamName'],
                'school': student['school'],
                'grade': student['grade'],
                'className': student['className'],
                'stoveNumber': student['stoveNumber'],
                'memberCount': student['memberCount'],
                'memberNames': student['memberNames'],
                'groupLeader': student.get('groupLeader', ''),  # 项目组长
                'submitTime': student['submitTime'],
                'hasProcessRecord': student['hasProcessRecord'],
                'hasSummary': student['hasSummary'],
                'completedStages': student['completedStages'],
                'totalStages': student['totalStages'],
                'stageRatings': stage_ratings  # 每个阶段的评分
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


@app.route('/api/student/<student_id>/media/upload', methods=['POST'])
def upload_media_file(student_id: str):
    """上传媒体文件（照片/视频）"""
    try:
        # 记录所有上传请求（包括失败的）
        logger.info("=" * 60)
        logger.info(f"收到文件上传请求: student_id={student_id}")
        logger.info(f"请求方法: {request.method}")
        logger.info(f"Content-Type: {request.content_type}")
        logger.info(f"请求文件: {list(request.files.keys())}")
        logger.info(f"请求表单: {list(request.form.keys())}")
        
        if 'file' not in request.files:
            logger.warning("❌ 上传请求中没有 'file' 字段")
            logger.warning(f"   可用的文件字段: {list(request.files.keys())}")
            return jsonify({
                'status': 'error',
                'message': '没有文件'
            }), 400
        
        file = request.files['file']
        if file.filename == '':
            logger.warning("❌ 上传的文件名为空")
            return jsonify({
                'status': 'error',
                'message': '文件名为空'
            }), 400
        
        # 获取文件信息
        original_path = request.form.get('original_path', '')  # Android端的原始路径
        file_type = request.form.get('type', 'PHOTO')  # PHOTO 或 VIDEO
        timestamp = request.form.get('timestamp', '0')
        
        logger.info(f"文件信息:")
        logger.info(f"   文件名: {file.filename}")
        logger.info(f"   原始路径: {original_path}")
        logger.info(f"   文件类型: {file_type}")
        logger.info(f"   时间戳: {timestamp}")
        
        # 创建学生媒体目录
        student_media_dir = os.path.join(Config.MEDIA_DIR, student_id)
        os.makedirs(student_media_dir, exist_ok=True)
        logger.info(f"媒体目录: {student_media_dir}")
        
        # 生成安全的文件名（使用原始文件名或时间戳）
        if original_path:
            # 从原始路径提取文件名
            safe_filename = os.path.basename(original_path)
        else:
            # 使用上传的文件名
            safe_filename = file.filename
        
        # 确保文件名安全
        safe_filename = safe_filename.replace('..', '').replace('/', '').replace('\\', '')
        
        file_path = os.path.join(student_media_dir, safe_filename)
        logger.info(f"保存路径: {file_path}")
        
        # 保存文件
        file.save(file_path)
        logger.info(f"文件已保存，大小: {os.path.getsize(file_path)} 字节")
        
        # 更新数据库中的文件路径（如果存在）
        try:
            from db_manager import DatabaseManager
            db_manager = DatabaseManager()
            # 查找使用原始路径的记录并更新为服务器端路径
            db_manager._execute(
                "UPDATE media_items SET file_path = ? WHERE file_path = ?",
                (safe_filename, original_path)
            )
            logger.info(f"   已更新数据库路径: {original_path} -> {safe_filename}")
        except Exception as e:
            logger.warning(f"   更新数据库路径失败: {str(e)}")
        
        logger.info(f"✅ 上传媒体文件成功: {student_id}/{safe_filename}")
        logger.info(f"   原始路径: {original_path}")
        logger.info(f"   文件类型: {file_type}")
        logger.info(f"   文件大小: {os.path.getsize(file_path)} 字节")
        
        return jsonify({
            'status': 'success',
            'filename': safe_filename,
            'message': '文件上传成功'
        }), 200
        
    except Exception as e:
        logger.error(f"上传媒体文件失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/media/<path:filename>', methods=['GET'])
def get_media_file(student_id: str, filename: str):
    """获取媒体文件（照片/视频）"""
    try:
        # 增强日志
        logger.info(f"请求媒体文件: student_id={student_id}, filename={filename[:100]}...")
        
        # 首先尝试从存储管理器查找
        file_path = storage.get_media_file_path(student_id, filename)
        
        if not file_path:
            logger.warning(f"媒体文件路径未找到: {student_id}/{filename[:50]}...")
            
            # 尝试从数据库查找完整路径
            try:
                from db_manager import DatabaseManager
                db_manager = DatabaseManager()
                
                # 如果filename是完整Android路径，提取文件名
                search_filename = os.path.basename(filename) if '/' in filename or '\\' in filename else filename
                
                # 查询数据库中的文件路径（匹配文件名）
                rows = db_manager._fetch_all(
                    "SELECT file_path FROM media_items WHERE file_path LIKE ? OR file_path LIKE ? LIMIT 5",
                    (f'%{search_filename}', f'%{os.path.basename(search_filename)}')
                )
                
                if rows:
                    logger.info(f"在数据库中找到 {len(rows)} 条相关记录")
                    for row in rows:
                        db_path = row['file_path']
                        logger.info(f"数据库路径: {db_path[:100]}...")
                        
                        # 提取文件名
                        db_filename = os.path.basename(db_path)
                        logger.info(f"提取的文件名: {db_filename}")
                        
                        # 尝试使用提取的文件名查找
                        file_path = storage.get_media_file_path(student_id, db_filename)
                        if file_path and os.path.exists(file_path):
                            logger.info(f"✅ 通过数据库路径找到文件: {file_path}")
                            break
                        
                        # 如果数据库路径是Android路径，说明文件未上传
                        if db_path.startswith('/storage/') or db_path.startswith('storage/'):
                            logger.warning(f"⚠️ 数据库中的路径是Android路径，文件可能未上传: {db_path[:100]}...")
                            logger.warning(f"   文件名应该是: {db_filename}")
                            logger.warning(f"   请检查Android端是否已重新编译并上传文件")
            except Exception as e:
                logger.error(f"从数据库查找路径失败: {str(e)}", exc_info=True)
        
        if not file_path or not os.path.exists(file_path):
            logger.error(f"❌ 媒体文件不存在: {file_path}")
            
            # 提供更详细的错误信息
            error_info = {
                'student_id': student_id,
                'filename': filename[:100] + ('...' if len(filename) > 100 else ''),
                'searched_path': str(file_path) if file_path else None,
                'hint': '文件可能未上传到服务器，请检查Android端是否已重新编译并上传文件'
            }
            
            return jsonify({
                'status': 'error',
                'message': '文件不存在',
                'debug': error_info
            }), 404
        
        logger.info(f"✅ 找到媒体文件: {file_path}")
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


@app.route('/api/database/clear', methods=['POST'])
def clear_database():
    """清空数据库数据"""
    try:
        # 使用db_manager清空数据库
        from db_manager import DatabaseManager
        db_manager = DatabaseManager()
        
        try:
            counts = db_manager.clear_all_data()
            cleared_items = [f"数据库表 {table}: {count} 条记录" for table, count in counts.items()]
            
            # 验证清空结果
            verification = {}
            for table in counts.keys():
                # 重新查询确认
                cursor = db_manager._execute(f"SELECT COUNT(*) FROM {table}")
                count = cursor.fetchone()[0]
                verification[f"db_{table}"] = count
            
            db_manager.close()
            
            logger.warning("⚠️ 所有数据库数据已被清空！")
            logger.info(f"清空验证结果: {verification}")
            
            return jsonify({
                'status': 'success',
                'message': '数据库已清空',
                'cleared_items': cleared_items,
                'verification': verification
            }), 200
            
        except Exception as e:
            db_manager.close()
            logger.error(f"清空数据库失败: {str(e)}", exc_info=True)
            return jsonify({
                'status': 'error',
                'message': f'清空数据库失败: {str(e)}'
            }), 500
        
    except Exception as e:
        logger.error(f"清空数据失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': f'清空失败: {str(e)}'
        }), 500


@app.route('/', methods=['GET'])
def index():
    """Web管理界面"""
    try:
        # 读取HTML模板文件
        template_path = os.path.join(os.path.dirname(__file__), 'templates', 'index.html')
        if os.path.exists(template_path):
            with open(template_path, 'r', encoding='utf-8') as f:
                html = f.read()
            return html, 200
        else:
            # 如果模板文件不存在，返回简单版本
            student_count = storage.get_student_count()
            server_ip = get_local_ip()
            return f"""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>教师端数据管理</title>
            </head>
            <body>
                <h1>🏕️ 野炊教学数据管理系统</h1>
                <p>服务器地址: http://{server_ip}:{Config.PORT}</p>
                <p>已接收学生数据: {student_count} 组</p>
                <p>请访问 /api/students 查看学生列表</p>
            </body>
            </html>
            """, 200
        
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

