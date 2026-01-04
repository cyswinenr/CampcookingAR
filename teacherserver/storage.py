#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据存储管理模块
负责学生数据的保存、读取和管理
"""

import os
import json
import shutil
import zipfile
import re
from datetime import datetime
from typing import Dict, List, Optional, Any
import logging

from models import StudentDataPackage, TeacherEvaluation, TeacherEvaluationV2, TeacherEvaluationTeam, TeamInfo, Team, TeamDivision, ProcessRecord, StageRecord, SummaryData
from config import Config
from db_manager import DatabaseManager

logger = logging.getLogger(__name__)


class DataStorage:
    """数据存储管理器"""
    
    def __init__(self, data_dir: str, media_dir: str):
        self.data_dir = data_dir
        self.media_dir = media_dir
        self.evaluation_dir = Config.EVALUATION_DIR
        self.export_dir = Config.EXPORT_DIR
        self.db_manager = DatabaseManager()
        
        # 确保目录存在
        os.makedirs(self.data_dir, exist_ok=True)
        os.makedirs(self.media_dir, exist_ok=True)
        os.makedirs(self.evaluation_dir, exist_ok=True)
        os.makedirs(self.export_dir, exist_ok=True)
    
    def save_student_data(self, data_package: StudentDataPackage) -> str:
        """保存学生数据到数据库"""
        try:
            # 生成学生ID（team_id）
            student_id = data_package.teamInfo.get_student_id()
            
            # ⭐ 关键调试：检查原始数据
            logger.info(f"🔍 开始保存学生数据到数据库: {student_id}")
            if hasattr(data_package, '_raw_data') and data_package._raw_data:
                process_record_raw = data_package._raw_data.get('processRecord')
                if process_record_raw:
                    has_stages = 'stages' in process_record_raw
                    logger.info(f"   原始数据检查:")
                    logger.info(f"     processRecord 存在: {process_record_raw is not None}")
                    logger.info(f"     包含 stages 字段: {has_stages}")
                    if has_stages:
                        stages_dict = process_record_raw.get('stages', {})
                        stages_count = len(stages_dict)
                        logger.info(f"     stages 数量: {stages_count}")
                        # 统计媒体文件
                        total_media = 0
                        for stage_name, stage_data in stages_dict.items():
                            media_items = stage_data.get('mediaItems', [])
                            if not media_items:
                                media_items = stage_data.get('media_items', [])
                            media_count = len(media_items) if media_items else 0
                            total_media += media_count
                        logger.info(f"     总计媒体文件: {total_media}")
                    else:
                        logger.warning(f"     ⚠️ processRecord 中没有 stages 字段！")
                        logger.warning(f"     processRecord 的键: {list(process_record_raw.keys())}")
                else:
                    logger.warning(f"   ⚠️ _raw_data 中没有 processRecord")
            else:
                logger.warning(f"⚠️ data_package 没有 _raw_data 或为空")
            
            # 1. 保存团队信息
            team = Team({'teamInfo': data_package.teamInfo.to_dict()})
            self.db_manager.save_team(team)
            
            # 2. 保存团队分工（如果有）
            if data_package.teamDivision and not data_package.teamDivision.is_empty():
                # 确保team_id已设置
                data_package.teamDivision.team_id = student_id
                self.db_manager.save_team_division(student_id, data_package.teamDivision)
            
            # 3. 保存过程记录和阶段记录（如果有）
            if data_package.processRecord:
                # 提取阶段记录和媒体文件
                stages = []
                stages_media = {}  # 存储每个阶段的媒体文件
                # 从原始数据中提取stages（如果存在）
                if hasattr(data_package, '_raw_data') and data_package._raw_data:
                    process_data = data_package._raw_data.get('processRecord')
                    logger.info(f"处理过程记录数据: process_data存在={process_data is not None}")
                    
                    if process_data:
                        if 'stages' in process_data:
                            stages_dict = process_data.get('stages', {})
                            logger.info(f"找到stages数据: {len(stages_dict)} 个阶段")
                            
                            for stage_name, stage_data in stages_dict.items():
                                try:
                                    stage = StageRecord(stage_data)
                                    stages.append(stage)
                                    
                                    # 提取媒体文件
                                    media_items = []
                                    if 'mediaItems' in stage_data:
                                        media_items = stage_data['mediaItems']
                                    elif 'media_items' in stage_data:
                                        media_items = stage_data['media_items']
                                    
                                    if media_items:
                                        stages_media[stage_name] = media_items
                                        logger.info(f"  阶段 {stage_name}: {len(media_items)} 个媒体文件")
                                        # 记录每个媒体文件的详细信息
                                        for idx, media_item in enumerate(media_items):
                                            logger.info(f"    媒体文件 {idx+1}: path={media_item.get('path', 'N/A')}, type={media_item.get('type', 'N/A')}")
                                    else:
                                        logger.info(f"  阶段 {stage_name}: 没有媒体文件")
                                        
                                except Exception as e:
                                    logger.error(f"处理阶段 {stage_name} 失败: {str(e)}", exc_info=True)
                        else:
                            logger.warning(f"processRecord 中没有 'stages' 字段")
                            logger.debug(f"processRecord 的键: {list(process_data.keys()) if process_data else []}")
                    else:
                        logger.warning(f"process_data 为 None")
                else:
                    logger.warning(f"data_package 没有 _raw_data 属性或 _raw_data 为空")
                
                # 保存过程记录和阶段记录（包括媒体文件）
                logger.info(f"准备保存: {len(stages)} 个阶段记录, {len(stages_media)} 个阶段有媒体文件")
                self.db_manager.save_process_record(student_id, data_package.processRecord, stages, stages_media)
            
            # 4. 保存课后总结（如果有）
            if data_package.summaryData:
                self.db_manager.save_summary_data(student_id, data_package.summaryData)
            
            logger.info(f"保存学生数据到数据库: {student_id}")
            
            return student_id
            
        except Exception as e:
            logger.error(f"保存学生数据失败: {str(e)}", exc_info=True)
            raise
    
    def get_all_students(self) -> List[Dict[str, Any]]:
        """获取所有学生列表（从数据库读取）"""
        students = []
        
        try:
            # 从数据库获取所有团队
            teams = self.db_manager.get_all_teams()
            
            for team in teams:
                student_id = team.team_id
                
                try:
                    # 获取团队分工信息
                    team_division = self.db_manager.get_team_division(student_id)
                    group_leader = ''
                    if team_division:
                        group_leader = team_division.group_leader
                    
                    # 获取过程记录和阶段记录
                    process_result = self.db_manager.get_process_record(student_id)
                    completed_stages = 0
                    total_stages = 0
                    has_process_record = False
                    stage_ratings = {}  # 存储每个阶段的评分
                    if process_result:
                        process_record, stages = process_result
                        has_process_record = True
                        total_stages = len(stages)
                        completed_stages = sum(1 for s in stages if s.is_completed)
                        logger.info(f"🔍 学生 {student_id}: 找到 {len(stages)} 个阶段记录")
                        # 提取每个阶段的评分
                        for stage in stages:
                            # 确保正确读取评分值（处理 None、0 等情况）
                            self_rating = stage.self_rating
                            if self_rating is None:
                                self_rating = 0
                            else:
                                # 确保是整数类型
                                try:
                                    self_rating = int(self_rating)
                                except (ValueError, TypeError):
                                    self_rating = 0
                            
                            stage_ratings[stage.stage_name] = {
                                'selfRating': self_rating,
                                'isCompleted': stage.is_completed
                            }
                            # 使用 INFO 级别，确保能看到日志
                            logger.info(f"✅ 阶段 {stage.stage_name} 评分: {self_rating} (原始值: {stage.self_rating}, 类型: {type(stage.self_rating)})")
                    
                    # 检查是否有课后总结
                    summary_data = self.db_manager.get_summary_data(student_id)
                    has_summary = summary_data is not None
                    
                    # 提取炉号数字用于排序
                    stove_number_str = team.stove_number
                    stove_number_int = 0
                    try:
                        # 尝试从字符串中提取数字，例如 "1号炉" -> 1
                        match = re.search(r'(\d+)', stove_number_str)
                        if match:
                            stove_number_int = int(match.group(1))
                    except:
                        pass
                    
                    students.append({
                        'id': student_id,
                        'teamName': team.get_display_name(),
                        'school': team.school,
                        'grade': team.grade,
                        'className': team.class_name,
                        'stoveNumber': team.stove_number,
                        'stoveNumberInt': stove_number_int,  # 用于排序
                        'memberCount': team.member_count,
                        'memberNames': team.member_names,
                        'groupLeader': group_leader,  # 项目组长
                        'submitTime': team.updated_at / 1000.0,  # 转换为秒（兼容旧格式）
                        'hasProcessRecord': has_process_record,
                        'hasSummary': has_summary,
                        'completedStages': completed_stages,
                        'totalStages': total_stages,
                        'stageRatings': stage_ratings  # 每个阶段的评分
                    })
                    
                    # 记录评分数据摘要
                    if stage_ratings:
                        logger.info(f"📊 学生 {student_id} 的评分摘要: {len(stage_ratings)} 个阶段有数据")
                        for stage_name, rating_data in stage_ratings.items():
                            logger.info(f"   {stage_name}: {rating_data['selfRating']} 星")
                    
                except Exception as e:
                    logger.error(f"读取学生数据失败 {student_id}: {str(e)}")
                    continue
            
            # 按照炉号数字排序（1-20），从小到大
            # 如果炉号相同，则按提交时间排序（后提交的排在后面）
            students.sort(key=lambda x: (x['stoveNumberInt'], x.get('submitTime', 0)))
            
        except Exception as e:
            logger.error(f"获取学生列表失败: {str(e)}", exc_info=True)
        
        return students
    
    def get_student_data(self, student_id: str) -> Optional[Dict[str, Any]]:
        """获取指定学生的详细数据（从数据库读取）"""
        try:
            # 获取团队信息
            team = self.db_manager.get_team(student_id)
            if not team:
                return None
            
            # 组装数据
            data = {
                'teamInfo': team.to_android_dict()
            }
            
            # 获取团队分工
            team_division = self.db_manager.get_team_division(student_id)
            if team_division and not team_division.is_empty():
                data['teamDivision'] = team_division.to_android_dict()
            else:
                data['teamDivision'] = None
            
            # 获取过程记录和阶段记录
            try:
                process_result = self.db_manager.get_process_record(student_id)
                if process_result:
                    process_record, stages = process_result
                    # 组装过程记录（Android格式）
                    try:
                        process_dict = process_record.to_android_dict()
                        # 添加阶段记录
                        stages_dict = {}
                        for stage in stages:
                            try:
                                stages_dict[stage.stage_name] = stage.to_android_dict()
                            except Exception as e:
                                logger.error(f"转换阶段记录失败 {stage.stage_name}: {str(e)}", exc_info=True)
                                # 使用默认值
                                stages_dict[stage.stage_name] = {
                                    'stage': stage.stage_name if hasattr(stage, 'stage_name') else '',
                                    'startTime': getattr(stage, 'start_time', 0),
                                    'endTime': getattr(stage, 'end_time', None),
                                    'selfRating': getattr(stage, 'self_rating', 0),
                                    'notes': getattr(stage, 'notes', ''),
                                    'problemNotes': getattr(stage, 'problem_notes', ''),
                                    'isCompleted': getattr(stage, 'is_completed', False),
                                    'selectedTags': getattr(stage, 'selected_tags', [])
                                }
                        process_dict['stages'] = stages_dict
                        data['processRecord'] = process_dict
                    except Exception as e:
                        logger.error(f"转换过程记录失败: {str(e)}", exc_info=True)
                        # 使用默认值
                        data['processRecord'] = {
                            'startTime': getattr(process_record, 'start_time', 0),
                            'endTime': getattr(process_record, 'end_time', None),
                            'currentStage': getattr(process_record, 'current_stage', 'PREPARATION'),
                            'overallNotes': getattr(process_record, 'overall_notes', ''),
                            'stages': {}
                        }
                else:
                    data['processRecord'] = None
            except Exception as e:
                logger.error(f"获取过程记录失败: {str(e)}", exc_info=True)
                data['processRecord'] = None
            
            # 获取课后总结
            summary_data = self.db_manager.get_summary_data(student_id)
            if summary_data:
                data['summaryData'] = summary_data.to_android_dict()
            else:
                data['summaryData'] = None
            
            # 获取评价数据
            evaluation = self.get_student_evaluation(student_id)
            if evaluation:
                data['teacherEvaluation'] = evaluation.to_android_dict()
            
            # 添加exportTime（使用updated_at）
            data['exportTime'] = team.updated_at
            
            return data
            
        except Exception as e:
            logger.error(f"获取学生数据失败 {student_id}: {str(e)}", exc_info=True)
            return None
    
    def student_exists(self, student_id: str) -> bool:
        """检查学生是否存在（从数据库检查）"""
        team = self.db_manager.get_team(student_id)
        return team is not None
    
    def get_student_count(self) -> int:
        """获取学生数量（从数据库）"""
        try:
            teams = self.db_manager.get_all_teams()
            return len(teams)
        except Exception as e:
            logger.error(f"获取学生数量失败: {str(e)}")
            return 0
    
    def save_student_evaluation(self, student_id: str, evaluation: TeacherEvaluation):
        """保存教师评价（到数据库）"""
        try:
            # 确保学生存在
            if not self.student_exists(student_id):
                raise ValueError(f"学生 {student_id} 不存在")
            
            # 保存到数据库
            self.db_manager.save_teacher_evaluation(student_id, evaluation)
            
            logger.info(f"保存评价到数据库: {student_id} - {evaluation.stage_name}")
            
        except Exception as e:
            logger.error(f"保存评价失败: {str(e)}", exc_info=True)
            raise
    
    def get_student_evaluation(self, student_id: str, stage_name: Optional[str] = None) -> Optional[Dict[str, Any]]:
        """获取学生评价（从数据库），如果指定stage_name则获取特定阶段的评价"""
        try:
            evaluation = self.db_manager.get_teacher_evaluation(student_id, stage_name)
            if evaluation:
                return evaluation.to_android_dict()
            return None
                
        except Exception as e:
            logger.error(f"获取评价失败: {str(e)}")
            return None
    
    def get_all_student_evaluations(self, student_id: str) -> Dict[str, Dict[str, Any]]:
        """获取学生所有阶段的评价"""
        try:
            evaluations = self.db_manager.get_all_teacher_evaluations(student_id)
            result = {}
            for stage_name, evaluation in evaluations.items():
                result[stage_name] = evaluation.to_android_dict()
            return result
        except Exception as e:
            logger.error(f"获取所有评价失败: {str(e)}", exc_info=True)
            return {}
            logger.error(f"获取评价失败: {str(e)}")
            return None
    
    def get_media_file_path(self, student_id: str, filename: str) -> Optional[str]:
        """获取媒体文件路径"""
        try:
            logger.debug(f"查找媒体文件: student_id={student_id}, filename={filename}")
            
            # 处理不同的路径格式
            # 1. 如果filename是完整路径（Android路径），提取文件名
            original_filename = filename
            if os.path.sep in filename or '/' in filename:
                # 提取文件名（处理Android路径格式）
                filename = os.path.basename(filename)
                logger.debug(f"从完整路径提取文件名: {original_filename} -> {filename}")
            
            # 2. 尝试在媒体目录中查找（优先级最高）
            file_path = os.path.join(self.media_dir, student_id, filename)
            if os.path.exists(file_path):
                logger.info(f"✅ 找到媒体文件（媒体目录）: {file_path}")
                return file_path
            
            # 3. 尝试在媒体目录根目录查找
            file_path = os.path.join(self.media_dir, filename)
            if os.path.exists(file_path):
                logger.info(f"✅ 找到媒体文件（媒体根目录）: {file_path}")
                return file_path
            
            # 4. 尝试在学生数据目录中查找
            student_dir = os.path.join(self.data_dir, student_id)
            file_path = os.path.join(student_dir, filename)
            if os.path.exists(file_path):
                logger.info(f"✅ 找到媒体文件（数据目录）: {file_path}")
                return file_path
            
            # 5. 如果filename是完整路径且文件存在（本地测试用）
            if os.path.exists(original_filename):
                logger.info(f"✅ 找到媒体文件（完整路径）: {original_filename}")
                return original_filename
            
            # 6. 尝试从数据库查找对应的文件路径
            try:
                from db_manager import DatabaseManager
                db_manager = DatabaseManager()
                # 查询包含该文件名的记录
                rows = db_manager._fetch_all(
                    "SELECT file_path FROM media_items WHERE file_path LIKE ? OR file_path LIKE ? LIMIT 5",
                    (f'%{filename}', f'%{os.path.basename(filename)}')
                )
                
                if rows:
                    logger.info(f"在数据库中找到 {len(rows)} 条相关记录")
                    for row in rows:
                        db_path = row['file_path']
                        # 提取文件名
                        db_filename = os.path.basename(db_path)
                        # 再次尝试查找
                        test_path = os.path.join(self.media_dir, student_id, db_filename)
                        if os.path.exists(test_path):
                            logger.info(f"✅ 通过数据库路径找到文件: {test_path}")
                            return test_path
            except Exception as e:
                logger.debug(f"从数据库查找失败: {str(e)}")
            
            logger.warning(f"❌ 媒体文件未找到: student_id={student_id}, filename={filename}")
            logger.warning(f"   尝试过的路径:")
            logger.warning(f"     1. {os.path.join(self.media_dir, student_id, filename)}")
            logger.warning(f"     2. {os.path.join(self.media_dir, filename)}")
            logger.warning(f"     3. {os.path.join(self.data_dir, student_id, filename)}")
            return None
            
        except Exception as e:
            logger.error(f"获取媒体文件路径失败: {str(e)}", exc_info=True)
            return None
    
    def export_all_data(self) -> Optional[str]:
        """导出所有数据为ZIP文件（包含数据库、媒体文件、学生数据、评价数据等）"""
        try:
            from config import Config
            
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            zip_filename = f'campcooking_export_{timestamp}.zip'
            zip_path = os.path.join(self.export_dir, zip_filename)
            
            logger.info(f"开始导出所有数据到: {zip_path}")
            
            with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
                # 1. 添加数据库文件
                db_path = Config.DATABASE_PATH
                if os.path.exists(db_path):
                    zipf.write(db_path, 'campcooking.db')
                    logger.info(f"✅ 已添加数据库文件: {db_path}")
                else:
                    logger.warning(f"⚠️ 数据库文件不存在: {db_path}")
                
                # 2. 添加学生数据目录
                if os.path.exists(self.data_dir):
                    student_count = 0
                    for root, dirs, files in os.walk(self.data_dir):
                        for file in files:
                            file_path = os.path.join(root, file)
                            arcname = os.path.join('students', os.path.relpath(file_path, self.data_dir))
                            zipf.write(file_path, arcname)
                            student_count += 1
                    logger.info(f"✅ 已添加 {student_count} 个学生数据文件")
                
                # 3. 添加评价数据
                if os.path.exists(self.evaluation_dir):
                    eval_count = 0
                    for file in os.listdir(self.evaluation_dir):
                        file_path = os.path.join(self.evaluation_dir, file)
                        if os.path.isfile(file_path):
                            arcname = os.path.join('evaluations', file)
                            zipf.write(file_path, arcname)
                            eval_count += 1
                    logger.info(f"✅ 已添加 {eval_count} 个评价文件")
                
                # 4. 添加媒体文件（照片和视频）
                if os.path.exists(self.media_dir):
                    media_count = 0
                    total_size = 0
                    for root, dirs, files in os.walk(self.media_dir):
                        for file in files:
                            file_path = os.path.join(root, file)
                            arcname = os.path.join('media', os.path.relpath(file_path, self.media_dir))
                            zipf.write(file_path, arcname)
                            media_count += 1
                            total_size += os.path.getsize(file_path)
                    logger.info(f"✅ 已添加 {media_count} 个媒体文件 (总大小: {total_size / 1024 / 1024:.2f} MB)")
                
                # 5. 添加元数据文件（导出信息）
                metadata = {
                    'export_time': datetime.now().isoformat(),
                    'export_version': '1.0',
                    'database_path': 'campcooking.db',
                    'students_dir': 'students',
                    'evaluations_dir': 'evaluations',
                    'media_dir': 'media',
                    'description': '野炊教学数据管理系统 - 完整数据导出'
                }
                metadata_json = json.dumps(metadata, ensure_ascii=False, indent=2)
                zipf.writestr('metadata.json', metadata_json.encode('utf-8'))
                logger.info("✅ 已添加元数据文件")
            
            file_size = os.path.getsize(zip_path)
            logger.info(f"✅ 导出完成: {zip_path} (大小: {file_size / 1024 / 1024:.2f} MB)")
            return zip_path
            
        except Exception as e:
            logger.error(f"导出数据失败: {str(e)}", exc_info=True)
            return None
    
    def import_all_data(self, zip_path: str, merge_mode: bool = False) -> Dict[str, Any]:
        """
        从ZIP文件导入所有数据
        
        Args:
            zip_path: ZIP文件路径
            merge_mode: 是否合并模式（True=合并数据，False=覆盖数据）
        
        Returns:
            导入结果字典，包含成功/失败信息
        """
        try:
            from config import Config
            
            result = {
                'success': False,
                'message': '',
                'imported_items': {
                    'database': False,
                    'students': 0,
                    'evaluations': 0,
                    'media': 0
                },
                'errors': []
            }
            
            if not os.path.exists(zip_path):
                result['message'] = f'ZIP文件不存在: {zip_path}'
                return result
            
            logger.info(f"开始导入数据从: {zip_path}")
            
            with zipfile.ZipFile(zip_path, 'r') as zipf:
                # 读取元数据
                metadata = None
                if 'metadata.json' in zipf.namelist():
                    try:
                        metadata_json = zipf.read('metadata.json').decode('utf-8')
                        metadata = json.loads(metadata_json)
                        logger.info(f"读取元数据: {metadata.get('export_time', '未知时间')}")
                    except Exception as e:
                        logger.warning(f"读取元数据失败: {str(e)}")
                
                # 1. 导入数据库
                if 'campcooking.db' in zipf.namelist():
                    try:
                        if not merge_mode:
                            # 覆盖模式：备份现有数据库
                            if os.path.exists(Config.DATABASE_PATH):
                                backup_path = Config.DATABASE_PATH + f'.backup_{datetime.now().strftime("%Y%m%d_%H%M%S")}'
                                shutil.copy2(Config.DATABASE_PATH, backup_path)
                                logger.info(f"已备份现有数据库到: {backup_path}")
                            
                            # 提取数据库文件
                            os.makedirs(os.path.dirname(Config.DATABASE_PATH), exist_ok=True)
                            with zipf.open('campcooking.db') as db_file:
                                with open(Config.DATABASE_PATH, 'wb') as out_file:
                                    out_file.write(db_file.read())
                            result['imported_items']['database'] = True
                            logger.info("✅ 数据库导入成功")
                        else:
                            # 合并模式：需要更复杂的处理，暂时跳过
                            logger.warning("⚠️ 合并模式暂不支持数据库导入，跳过")
                            result['errors'].append("合并模式暂不支持数据库导入")
                    except Exception as e:
                        error_msg = f"数据库导入失败: {str(e)}"
                        logger.error(error_msg, exc_info=True)
                        result['errors'].append(error_msg)
                
                # 2. 导入学生数据
                student_files = [f for f in zipf.namelist() if f.startswith('students/') and not f.endswith('/')]
                if student_files:
                    try:
                        for file_info in student_files:
                            # 提取相对路径
                            rel_path = file_info[len('students/'):]
                            target_path = os.path.join(self.data_dir, rel_path)
                            
                            # 创建目录
                            os.makedirs(os.path.dirname(target_path), exist_ok=True)
                            
                            # 提取文件
                            with zipf.open(file_info) as src_file:
                                with open(target_path, 'wb') as dst_file:
                                    dst_file.write(src_file.read())
                            
                            result['imported_items']['students'] += 1
                        
                        logger.info(f"✅ 已导入 {result['imported_items']['students']} 个学生数据文件")
                    except Exception as e:
                        error_msg = f"学生数据导入失败: {str(e)}"
                        logger.error(error_msg, exc_info=True)
                        result['errors'].append(error_msg)
                
                # 3. 导入评价数据
                eval_files = [f for f in zipf.namelist() if f.startswith('evaluations/') and not f.endswith('/')]
                if eval_files:
                    try:
                        for file_info in eval_files:
                            filename = os.path.basename(file_info)
                            target_path = os.path.join(self.evaluation_dir, filename)
                            
                            # 合并模式：如果文件已存在，跳过
                            if merge_mode and os.path.exists(target_path):
                                continue
                            
                            with zipf.open(file_info) as src_file:
                                with open(target_path, 'wb') as dst_file:
                                    dst_file.write(src_file.read())
                            
                            result['imported_items']['evaluations'] += 1
                        
                        logger.info(f"✅ 已导入 {result['imported_items']['evaluations']} 个评价文件")
                    except Exception as e:
                        error_msg = f"评价数据导入失败: {str(e)}"
                        logger.error(error_msg, exc_info=True)
                        result['errors'].append(error_msg)
                
                # 4. 导入媒体文件
                media_files = [f for f in zipf.namelist() if f.startswith('media/') and not f.endswith('/')]
                if media_files:
                    try:
                        for file_info in media_files:
                            # 提取相对路径
                            rel_path = file_info[len('media/'):]
                            target_path = os.path.join(self.media_dir, rel_path)
                            
                            # 合并模式：如果文件已存在，跳过
                            if merge_mode and os.path.exists(target_path):
                                continue
                            
                            # 创建目录
                            os.makedirs(os.path.dirname(target_path), exist_ok=True)
                            
                            # 提取文件
                            with zipf.open(file_info) as src_file:
                                with open(target_path, 'wb') as dst_file:
                                    dst_file.write(src_file.read())
                            
                            result['imported_items']['media'] += 1
                        
                        logger.info(f"✅ 已导入 {result['imported_items']['media']} 个媒体文件")
                    except Exception as e:
                        error_msg = f"媒体文件导入失败: {str(e)}"
                        logger.error(error_msg, exc_info=True)
                        result['errors'].append(error_msg)
            
            result['success'] = len(result['errors']) == 0
            result['message'] = f"导入完成: 数据库={result['imported_items']['database']}, 学生数据={result['imported_items']['students']}, 评价={result['imported_items']['evaluations']}, 媒体={result['imported_items']['media']}"
            
            logger.info(f"✅ 导入完成: {result['message']}")
            if result['errors']:
                logger.warning(f"⚠️ 导入过程中有 {len(result['errors'])} 个错误")
            
            return result
            
        except Exception as e:
            error_msg = f"导入数据失败: {str(e)}"
            logger.error(error_msg, exc_info=True)
            return {
                'success': False,
                'message': error_msg,
                'imported_items': {},
                'errors': [error_msg]
            }
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取统计数据（从数据库）"""
        try:
            return self.db_manager.get_statistics()
        except Exception as e:
            logger.error(f"获取统计失败: {str(e)}", exc_info=True)
            return {
                'totalStudents': 0,
                'studentsWithProcess': 0,
                'studentsWithSummary': 0,
                'averageCompletion': 0,
                'totalCompletedStages': 0,
                'totalStages': 0
            }
    
    # ==================== Teacher Evaluation V2 操作 ====================
    
    def save_teacher_evaluation_v2(self, team_id: str, team_name: str, evaluation_data: Dict[str, Any]) -> bool:
        """保存教师评价V2（高性能版本，单次数据库操作 + JSON文件）"""
        try:
            # 确保评价目录存在
            os.makedirs(Config.EVALUATION_DIR, exist_ok=True)
            
            # 准备JSON数据
            json_data = {
                'teamId': team_id,
                'teamName': team_name,
                'timestamp': evaluation_data.get('timestamp', int(datetime.now().timestamp() * 1000)),
                'stages': evaluation_data.get('stages', {})
            }
            
            # 保存JSON文件
            timestamp = int(datetime.now().timestamp() * 1000)
            safe_team_id = team_id.replace('/', '_').replace('\\', '_')
            json_filename = f"evaluation_{safe_team_id}_{timestamp}.json"
            json_file_path = os.path.join(Config.EVALUATION_DIR, json_filename)
            
            # 保存带时间戳的文件
            with open(json_file_path, 'w', encoding='utf-8') as f:
                json.dump(json_data, f, ensure_ascii=False, indent=2)
            
            # 保存最新版本（覆盖）
            latest_filename = f"evaluation_{safe_team_id}_latest.json"
            latest_file_path = os.path.join(Config.EVALUATION_DIR, latest_filename)
            with open(latest_file_path, 'w', encoding='utf-8') as f:
                json.dump(json_data, f, ensure_ascii=False, indent=2)
            
            # 保存到数据库
            self.db_manager.save_teacher_evaluation_v2(
                team_id=team_id,
                evaluation_data=json_data,
                json_file_path=json_file_path
            )
            
            # 确保团队在teacher_evaluation_teams表中
            self.db_manager.save_teacher_evaluation_team(team_id, team_name)
            
            logger.info(f"✅ 保存教师评价V2成功: {team_id}, JSON文件: {json_file_path}")
            return True
            
        except Exception as e:
            logger.error(f"保存教师评价V2失败: {str(e)}", exc_info=True)
            return False
    
    def get_teacher_evaluation_v2(self, team_id: str) -> Optional[Dict[str, Any]]:
        """获取教师评价V2"""
        try:
            evaluation = self.db_manager.get_teacher_evaluation_v2(team_id)
            if evaluation:
                return evaluation.to_json_dict()
            return None
        except Exception as e:
            logger.error(f"获取教师评价V2失败: {str(e)}", exc_info=True)
            return None
    
    def get_all_evaluation_teams(self, page: int = 1, page_size: int = 5) -> Dict[str, Any]:
        """
        获取所有可评价的团队列表（从teams表读取所有已提交数据的团队）

        Args:
            page: 页码（从1开始）
            page_size: 每页数量（默认5个）

        Returns:
            包含团队列表和分页信息的字典
        """
        try:
            import re

            # 从 teams 表读取所有团队
            teams = self.db_manager.get_all_teams()

            # 提取炉号数字并排序
            def extract_stove_number(team):
                """从炉号中提取数字，如 '1号炉' -> 1"""
                match = re.search(r'(\d+)', team.stove_number)
                return int(match.group(1)) if match else 999

            # 按炉号数字排序
            teams.sort(key=extract_stove_number)

            # 构建完整团队信息
            all_teams = []
            for team in teams:
                # 获取团队分工信息
                division = self.db_manager.get_team_division(team.team_id)
                group_leader = division.group_leader if division else ""

                # 构建显示名称（学校 + 年级 + 班级 + 炉号）
                display_name = f"{team.school} {team.grade}{team.class_name} {team.stove_number}"

                all_teams.append({
                    'id': team.team_id,
                    'teamId': team.team_id,
                    'teamName': display_name,
                    'school': team.school,
                    'grade': team.grade,
                    'className': team.class_name,
                    'stoveNumber': team.stove_number,
                    'memberCount': team.member_count,
                    'memberNames': team.member_names,
                    'groupLeader': group_leader,
                    # 团队分工
                    'division': {
                        'groupLeader': division.group_leader if division else "",
                        'groupCooking': division.group_cooking if division else "",
                        'groupSoupRice': division.group_soup_rice if division else "",
                        'groupFire': division.group_fire if division else "",
                        'groupHealth': division.group_health if division else ""
                    } if division else None
                })

            # 分页处理
            total_count = len(all_teams)
            total_pages = (total_count + page_size - 1) // page_size  # 向上取整
            page = max(1, min(page, total_pages)) if total_pages > 0 else 1  # 确保页码有效

            start_idx = (page - 1) * page_size
            end_idx = min(start_idx + page_size, total_count)
            page_teams = all_teams[start_idx:end_idx]

            return {
                'teams': page_teams,
                'pagination': {
                    'currentPage': page,
                    'pageSize': page_size,
                    'totalPages': total_pages,
                    'totalCount': total_count,
                    'hasNext': page < total_pages,
                    'hasPrev': page > 1
                }
            }
        except Exception as e:
            logger.error(f"获取评价团队列表失败: {str(e)}", exc_info=True)
            return {
                'teams': [],
                'pagination': {
                    'currentPage': 1,
                    'pageSize': page_size,
                    'totalPages': 0,
                    'totalCount': 0,
                    'hasNext': False,
                    'hasPrev': False
                }
            }

