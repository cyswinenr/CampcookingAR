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

from models import StudentDataPackage, TeacherEvaluation, TeamInfo, Team, TeamDivision, ProcessRecord, StageRecord, SummaryData
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
                # 提取阶段记录
                stages = []
                # 从原始数据中提取stages（如果存在）
                if hasattr(data_package, '_raw_data') and data_package._raw_data:
                    process_data = data_package._raw_data.get('processRecord')
                    if process_data and 'stages' in process_data:
                        stages_dict = process_data.get('stages', {})
                        for stage_name, stage_data in stages_dict.items():
                            stage = StageRecord(stage_data)
                            stages.append(stage)
                
                # 保存过程记录和阶段记录
                self.db_manager.save_process_record(student_id, data_package.processRecord, stages)
            
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
                    if process_result:
                        process_record, stages = process_result
                        has_process_record = True
                        total_stages = len(stages)
                        completed_stages = sum(1 for s in stages if s.is_completed)
                    
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
                        'totalStages': total_stages
                    })
                    
                except Exception as e:
                    logger.error(f"读取学生数据失败 {student_id}: {str(e)}")
                    continue
            
            # 按照炉号数字排序（1-20）
            students.sort(key=lambda x: x['stoveNumberInt'])
            
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
    
    def get_student_evaluation(self, student_id: str) -> Optional[Dict[str, Any]]:
        """获取学生评价（从数据库）"""
        try:
            evaluation = self.db_manager.get_teacher_evaluation(student_id)
            if evaluation:
                return evaluation.to_android_dict()
            return None
                
        except Exception as e:
            logger.error(f"获取评价失败: {str(e)}")
            return None
    
    def get_media_file_path(self, student_id: str, filename: str) -> Optional[str]:
        """获取媒体文件路径"""
        try:
            # 处理不同的路径格式
            # 1. 如果filename是完整路径，提取文件名
            if os.path.sep in filename:
                filename = os.path.basename(filename)
            
            # 2. 尝试在媒体目录中查找
            file_path = os.path.join(self.media_dir, student_id, filename)
            if os.path.exists(file_path):
                return file_path
            
            # 3. 尝试在媒体目录根目录查找
            file_path = os.path.join(self.media_dir, filename)
            if os.path.exists(file_path):
                return file_path
            
            # 4. 尝试在学生数据目录中查找
            student_dir = os.path.join(self.data_dir, student_id)
            file_path = os.path.join(student_dir, filename)
            if os.path.exists(file_path):
                return file_path
            
            # 5. 如果filename是完整路径且文件存在
            if os.path.exists(filename):
                return filename
            
            logger.warning(f"媒体文件未找到: {student_id}/{filename}")
            return None
            
        except Exception as e:
            logger.error(f"获取媒体文件路径失败: {str(e)}")
            return None
    
    def export_all_data(self) -> Optional[str]:
        """导出所有数据为ZIP文件"""
        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            zip_filename = f'export_{timestamp}.zip'
            zip_path = os.path.join(self.export_dir, zip_filename)
            
            with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
                # 添加学生数据
                if os.path.exists(self.data_dir):
                    for root, dirs, files in os.walk(self.data_dir):
                        for file in files:
                            file_path = os.path.join(root, file)
                            arcname = os.path.relpath(file_path, self.data_dir)
                            zipf.write(file_path, arcname)
                
                # 添加评价数据
                if os.path.exists(self.evaluation_dir):
                    for file in os.listdir(self.evaluation_dir):
                        file_path = os.path.join(self.evaluation_dir, file)
                        if os.path.isfile(file_path):
                            arcname = os.path.join('evaluations', file)
                            zipf.write(file_path, arcname)
            
            logger.info(f"导出数据: {zip_path}")
            return zip_path
            
        except Exception as e:
            logger.error(f"导出数据失败: {str(e)}", exc_info=True)
            return None
    
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

