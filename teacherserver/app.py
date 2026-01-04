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

from models import StudentDataPackage, TeacherEvaluation, Menu
from storage import DataStorage
from config import Config
from db_init import init_database
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


@app.route('/api/submit_menu', methods=['POST'])
def submit_menu():
    """接收学生端提交的菜单数据"""
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
        
        if 'menuData' not in data:
            return jsonify({
                'status': 'error',
                'message': '缺少菜单数据'
            }), 400
        
        # 生成团队ID（使用与Team模型相同的格式）
        team_info = data.get('teamInfo', {})
        # 从teamInfo中提取字段（使用驼峰命名，但转换为下划线命名用于生成team_id）
        school = team_info.get('school', '')
        grade = team_info.get('grade', '')
        class_name = team_info.get('className', '')  # 从className读取，但变量名用class_name
        stove_number = team_info.get('stoveNumber', '')  # 从stoveNumber读取，但变量名用stove_number
        team_id = f"{school}_{grade}_{class_name}_{stove_number}"
        
        logger.info(f"收到菜单数据提交: {team_id}")
        logger.info(f"团队信息: school={school}, grade={grade}, className={class_name}, stoveNumber={stove_number}")
        
        # 解析菜单数据
        menu_data = data.get('menuData', {})
        menu = Menu({'menuData': menu_data})
        menu.team_id = team_id
        
        # 保存菜单到数据库（如果已存在则覆盖）
        from db_manager import DatabaseManager
        db_manager = DatabaseManager()
        db_manager.save_menu(menu)
        
        logger.info(f"✅ 菜单已保存: {team_id}, 汤: {menu.soup}, 菜数: {len(menu.dishes)}")
        
        return jsonify({
            'status': 'success',
            'teamId': team_id,
            'message': '菜单保存成功'
        }), 200
        
    except Exception as e:
        logger.error(f"处理菜单提交失败: {str(e)}", exc_info=True)
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
            
            # 获取菜单数据
            student_id = student['id']
            menu = None
            try:
                from db_manager import DatabaseManager
                db_manager = DatabaseManager()
                menu = db_manager.get_menu(student_id)
                if menu:
                    logger.debug(f"✅ 找到菜单: {student_id}, 汤: {menu.soup}, 菜数: {len(menu.dishes)}")
                else:
                    logger.debug(f"⚠️ 未找到菜单: {student_id}")
            except Exception as e:
                logger.warning(f"获取菜单失败 {student_id}: {str(e)}")
            
            # 构建菜单数据
            menu_data = None
            if menu:
                menu_data = {
                    'soup': menu.soup,
                    'dishes': menu.dishes
                }
                logger.debug(f"菜单数据已构建: {student_id}, soup={menu.soup}, dishes={menu.dishes}")
            
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
                'stageRatings': stage_ratings,  # 每个阶段的评分
                'menu': menu_data  # 菜单数据（汤和菜名列表）
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
    """获取指定学生的教师评价（所有阶段）"""
    try:
        evaluations = storage.get_all_student_evaluations(student_id)
        
        return jsonify({
            'status': 'success',
            'evaluations': evaluations
        }), 200
        
    except Exception as e:
        logger.error(f"获取评价失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/evaluation', methods=['POST'])
def save_student_evaluation(student_id: str):
    """保存教师评价（支持按阶段保存）"""
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
        
        # 确保 stage_name 存在
        if not evaluation.stage_name:
            # 尝试从 evaluation 数据中获取
            if 'stage' in data['evaluation']:
                evaluation.stage_name = data['evaluation']['stage']
            else:
                return jsonify({
                    'status': 'error',
                    'message': '缺少阶段名称（stage_name）'
                }), 400
        
        # 保存评价
        storage.save_student_evaluation(student_id, evaluation)
        
        logger.info(f"✅ 保存评价: {student_id} - {evaluation.stage_name}")
        
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


@app.route('/api/evaluation/teams', methods=['GET'])
def get_evaluation_teams():
    """获取可评价的团队列表（支持分页和炉号排序）"""
    try:
        # 获取分页参数
        page = request.args.get('page', 1, type=int)
        page_size = request.args.get('page_size', 5, type=int)

        # 限制每页数量范围（1-20）
        page_size = max(1, min(page_size, 20))

        result = storage.get_all_evaluation_teams(page=page, page_size=page_size)

        return jsonify({
            'status': 'success',
            **result  # 包含 teams 和 pagination
        }), 200
    except Exception as e:
        logger.error(f"获取评价团队列表失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/evaluation', methods=['POST'])
def save_evaluation():
    """保存完整的教师评价数据（新版本，高性能，单次操作）"""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({
                'status': 'error',
                'message': '未收到数据'
            }), 400
        
        # 验证必要字段
        if 'teamId' not in data:
            return jsonify({
                'status': 'error',
                'message': '缺少团队ID'
            }), 400
        
        team_id = data['teamId']
        team_name = data.get('teamName', team_id)
        evaluations = data.get('evaluations', {})
        
        logger.info(f"收到评价保存请求（V2）: team_id={team_id}, 评价环节数={len(evaluations)}")
        
        # 准备评价数据（JSON格式）
        evaluation_data = {
            'timestamp': data.get('timestamp', int(datetime.now().timestamp() * 1000)),
            'stages': {}
        }
        
        # 转换评价数据格式
        for stage, stage_eval in evaluations.items():
            if isinstance(stage_eval, dict):
                evaluation_data['stages'][stage] = {
                    'positiveTags': stage_eval.get('positiveTags', []),
                    'improvementTags': stage_eval.get('improvementTags', []),
                    'otherComment': stage_eval.get('otherComment', '')
                }
        
        # 保存评价（单次操作，高性能）
        success = storage.save_teacher_evaluation_v2(team_id, team_name, evaluation_data)
        
        if success:
            return jsonify({
                'status': 'success',
                'message': f'评价保存成功（已保存 {len(evaluation_data["stages"])} 个环节）'
            }), 200
        else:
            return jsonify({
                'status': 'error',
                'message': '保存评价失败'
            }), 500
        
    except Exception as e:
        logger.error(f"保存评价失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': f'服务器错误: {str(e)}'
        }), 500


@app.route('/api/evaluation/<team_id>', methods=['GET'])
def get_evaluation(team_id: str):
    """获取指定团队的完整评价数据（新版本，优先使用V2）"""
    try:
        # 优先从V2表获取
        evaluation_v2 = storage.get_teacher_evaluation_v2(team_id)
        if evaluation_v2:
            return jsonify({
                'status': 'success',
                'teamId': team_id,
                'evaluations': evaluation_v2.get('stages', {})
            }), 200
        
        # 如果V2没有，尝试从旧表获取（向后兼容）
        all_evaluations = storage.get_all_student_evaluations(team_id)
        if all_evaluations:
            # 转换为前端需要的格式
            evaluations = {}
            for stage, eval_data in all_evaluations.items():
                evaluations[stage] = {
                    'positiveTags': eval_data.get('strengths', '').split(', ') if eval_data.get('strengths') else [],
                    'improvementTags': eval_data.get('improvements', '').split(', ') if eval_data.get('improvements') else [],
                    'otherComment': eval_data.get('comment', '')
                }
            
            return jsonify({
                'status': 'success',
                'teamId': team_id,
                'evaluations': evaluations
            }), 200
        
        # 没有找到评价
        return jsonify({
            'status': 'success',
            'teamId': team_id,
            'evaluations': {}
        }), 200
        
    except Exception as e:
        logger.error(f"获取评价失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/evaluation/media/upload', methods=['POST'])
def upload_evaluation_media(student_id: str):
    """上传教师评价的媒体文件（照片/视频）"""
    try:
        if 'file' not in request.files:
            return jsonify({
                'status': 'error',
                'message': '没有文件'
            }), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({
                'status': 'error',
                'message': '文件名为空'
            }), 400
        
        # 获取文件信息
        file_type = request.form.get('type', 'PHOTO')  # PHOTO 或 VIDEO
        evaluation_stage = request.form.get('evaluation_stage', '')  # 评价阶段
        
        # 创建评价媒体目录
        evaluation_media_dir = os.path.join(Config.MEDIA_DIR, student_id, 'evaluations', evaluation_stage)
        os.makedirs(evaluation_media_dir, exist_ok=True)
        
        # 生成安全的文件名
        timestamp = int(datetime.now().timestamp() * 1000)
        file_ext = os.path.splitext(file.filename)[1] or ('.mp4' if file_type == 'VIDEO' else '.jpg')
        safe_filename = f"EVAL_{evaluation_stage}_{timestamp}{file_ext}"
        
        file_path = os.path.join(evaluation_media_dir, safe_filename)
        file.save(file_path)
        
        logger.info(f"✅ 上传评价媒体文件成功: {student_id}/evaluations/{evaluation_stage}/{safe_filename}")
        
        return jsonify({
            'status': 'success',
            'filename': f'evaluations/{evaluation_stage}/{safe_filename}',
            'file_type': file_type,
            'message': '文件上传成功'
        }), 200
        
    except Exception as e:
        logger.error(f"上传评价媒体文件失败: {str(e)}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500


@app.route('/api/student/<student_id>/evaluation/media/<path:filename>', methods=['GET'])
def get_evaluation_media(student_id: str, filename: str):
    """获取教师评价的媒体文件"""
    try:
        # 构建文件路径
        file_path = os.path.join(Config.MEDIA_DIR, student_id, filename)
        
        if not os.path.exists(file_path):
            return jsonify({
                'status': 'error',
                'message': '文件不存在'
            }), 404
        
        return send_file(file_path)
        
    except Exception as e:
        logger.error(f"获取评价媒体文件失败: {str(e)}", exc_info=True)
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
    """清空数据库数据（需要密码验证）"""
    try:
        # 获取请求数据
        data = request.get_json() or {}
        password = data.get('password', '')
        
        # 验证密码
        from config import Config
        if password != Config.CLEAR_DATABASE_PASSWORD:
            logger.warning(f"⚠️ 清空数据库请求被拒绝：密码错误（尝试的密码: {password[:3]}***）")
            return jsonify({
                'status': 'error',
                'message': '密码错误，无法清空数据库'
            }), 403
        
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
    os.makedirs(Config.EVALUATION_DIR, exist_ok=True)
    
    # 自动初始化数据库（确保所有表都存在）
    print("=" * 60)
    print("正在检查数据库...")
    print("=" * 60)
    try:
        db_path = Config.DATABASE_PATH
        # 确保数据库目录存在
        os.makedirs(os.path.dirname(db_path), exist_ok=True)
        
        # 初始化数据库（如果表不存在会自动创建）
        success = init_database(db_path)
        if success:
            print("✅ 数据库检查完成，所有表已就绪")
        else:
            print("⚠️  数据库初始化失败，但将继续启动服务器")
            print("   如果遇到表不存在错误，请手动运行: python db_init.py")
    except Exception as e:
        logger.error(f"数据库初始化出错: {str(e)}", exc_info=True)
        print(f"⚠️  数据库初始化出错: {str(e)}")
        print("   如果遇到表不存在错误，请手动运行: python db_init.py")
    print("=" * 60)
    
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

