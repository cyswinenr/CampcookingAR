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
from datetime import datetime
from typing import Dict, List, Optional, Any
import logging

from models import StudentDataPackage, TeacherEvaluation, TeamInfo
from config import Config

logger = logging.getLogger(__name__)


class DataStorage:
    """数据存储管理器"""
    
    def __init__(self, data_dir: str, media_dir: str):
        self.data_dir = data_dir
        self.media_dir = media_dir
        self.evaluation_dir = Config.EVALUATION_DIR
        self.export_dir = Config.EXPORT_DIR
        
        # 确保目录存在
        os.makedirs(self.data_dir, exist_ok=True)
        os.makedirs(self.media_dir, exist_ok=True)
        os.makedirs(self.evaluation_dir, exist_ok=True)
        os.makedirs(self.export_dir, exist_ok=True)
    
    def save_student_data(self, data_package: StudentDataPackage) -> str:
        """保存学生数据"""
        try:
            # 生成学生ID
            student_id = data_package.teamInfo.get_student_id()
            
            # 创建学生数据目录
            student_dir = os.path.join(self.data_dir, student_id)
            os.makedirs(student_dir, exist_ok=True)
            
            # 保存JSON数据
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            json_file = os.path.join(student_dir, f'data_{timestamp}.json')
            
            with open(json_file, 'w', encoding='utf-8') as f:
                json.dump(data_package.to_dict(), f, ensure_ascii=False, indent=2)
            
            # 保存最新数据（覆盖）
            latest_file = os.path.join(student_dir, 'latest.json')
            shutil.copy(json_file, latest_file)
            
            logger.info(f"保存学生数据: {student_id} -> {json_file}")
            
            return student_id
            
        except Exception as e:
            logger.error(f"保存学生数据失败: {str(e)}", exc_info=True)
            raise
    
    def get_all_students(self) -> List[Dict[str, Any]]:
        """获取所有学生列表"""
        students = []
        
        try:
            if not os.path.exists(self.data_dir):
                return students
            
            for student_id in os.listdir(self.data_dir):
                student_dir = os.path.join(self.data_dir, student_id)
                if not os.path.isdir(student_dir):
                    continue
                
                # 读取最新数据
                latest_file = os.path.join(student_dir, 'latest.json')
                if not os.path.exists(latest_file):
                    continue
                
                try:
                    with open(latest_file, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                    
                    team_info = data.get('teamInfo', {})
                    process_record = data.get('processRecord')
                    summary_data = data.get('summaryData')
                    
                    # 计算完成阶段数
                    completed_stages = 0
                    total_stages = 0
                    if process_record:
                        stages = process_record.get('stages', {})
                        total_stages = len(stages)
                        completed_stages = sum(
                            1 for s in stages.values() 
                            if s.get('isCompleted', False)
                        )
                    
                    students.append({
                        'id': student_id,
                        'teamName': f"{team_info.get('school', '')} {team_info.get('grade', '')}年级 {team_info.get('className', '')} 炉号{team_info.get('stoveNumber', '')}",
                        'school': team_info.get('school', ''),
                        'grade': team_info.get('grade', ''),
                        'className': team_info.get('className', ''),
                        'stoveNumber': team_info.get('stoveNumber', ''),
                        'memberCount': team_info.get('memberCount', 0),
                        'memberNames': team_info.get('memberNames', ''),
                        'submitTime': os.path.getmtime(latest_file),
                        'hasProcessRecord': process_record is not None,
                        'hasSummary': summary_data is not None,
                        'completedStages': completed_stages,
                        'totalStages': total_stages
                    })
                    
                except Exception as e:
                    logger.error(f"读取学生数据失败 {student_id}: {str(e)}")
                    continue
            
            # 按提交时间排序
            students.sort(key=lambda x: x['submitTime'], reverse=True)
            
        except Exception as e:
            logger.error(f"获取学生列表失败: {str(e)}", exc_info=True)
        
        return students
    
    def get_student_data(self, student_id: str) -> Optional[Dict[str, Any]]:
        """获取指定学生的详细数据"""
        try:
            student_dir = os.path.join(self.data_dir, student_id)
            latest_file = os.path.join(student_dir, 'latest.json')
            
            if not os.path.exists(latest_file):
                return None
            
            with open(latest_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            # 获取评价数据
            evaluation = self.get_student_evaluation(student_id)
            if evaluation:
                data['teacherEvaluation'] = evaluation
            
            return data
            
        except Exception as e:
            logger.error(f"获取学生数据失败 {student_id}: {str(e)}", exc_info=True)
            return None
    
    def student_exists(self, student_id: str) -> bool:
        """检查学生是否存在"""
        student_dir = os.path.join(self.data_dir, student_id)
        latest_file = os.path.join(student_dir, 'latest.json')
        return os.path.exists(latest_file)
    
    def get_student_count(self) -> int:
        """获取学生数量"""
        try:
            if not os.path.exists(self.data_dir):
                return 0
            
            count = 0
            for item in os.listdir(self.data_dir):
                item_path = os.path.join(self.data_dir, item)
                if os.path.isdir(item_path):
                    latest_file = os.path.join(item_path, 'latest.json')
                    if os.path.exists(latest_file):
                        count += 1
            
            return count
            
        except Exception as e:
            logger.error(f"获取学生数量失败: {str(e)}")
            return 0
    
    def save_student_evaluation(self, student_id: str, evaluation: TeacherEvaluation):
        """保存教师评价"""
        try:
            # 确保学生存在
            if not self.student_exists(student_id):
                raise ValueError(f"学生 {student_id} 不存在")
            
            # 评价文件路径
            eval_file = os.path.join(self.evaluation_dir, f'{student_id}.json')
            
            # 读取现有评价（如果有）
            evaluations = {}
            if os.path.exists(eval_file):
                with open(eval_file, 'r', encoding='utf-8') as f:
                    evaluations = json.load(f)
            
            # 更新或添加评价
            evaluations[evaluation.stage] = evaluation.to_dict()
            
            # 保存
            with open(eval_file, 'w', encoding='utf-8') as f:
                json.dump(evaluations, f, ensure_ascii=False, indent=2)
            
            logger.info(f"保存评价: {student_id} - {evaluation.stage}")
            
        except Exception as e:
            logger.error(f"保存评价失败: {str(e)}", exc_info=True)
            raise
    
    def get_student_evaluation(self, student_id: str) -> Optional[Dict[str, Any]]:
        """获取学生评价"""
        try:
            eval_file = os.path.join(self.evaluation_dir, f'{student_id}.json')
            
            if not os.path.exists(eval_file):
                return None
            
            with open(eval_file, 'r', encoding='utf-8') as f:
                return json.load(f)
                
        except Exception as e:
            logger.error(f"获取评价失败: {str(e)}")
            return None
    
    def get_media_file_path(self, student_id: str, filename: str) -> Optional[str]:
        """获取媒体文件路径"""
        try:
            # 媒体文件可能存储在不同的位置
            # 这里简化处理，实际应该根据文件路径查找
            file_path = os.path.join(self.media_dir, student_id, filename)
            
            if os.path.exists(file_path):
                return file_path
            
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
        """获取统计数据"""
        try:
            students = self.get_all_students()
            
            total_students = len(students)
            students_with_process = sum(1 for s in students if s['hasProcessRecord'])
            students_with_summary = sum(1 for s in students if s['hasSummary'])
            
            # 计算平均完成度
            total_completed = sum(s['completedStages'] for s in students)
            total_stages = sum(s['totalStages'] for s in students)
            avg_completion = (total_completed / total_stages * 100) if total_stages > 0 else 0
            
            return {
                'totalStudents': total_students,
                'studentsWithProcess': students_with_process,
                'studentsWithSummary': students_with_summary,
                'averageCompletion': round(avg_completion, 2),
                'totalCompletedStages': total_completed,
                'totalStages': total_stages
            }
            
        except Exception as e:
            logger.error(f"获取统计失败: {str(e)}", exc_info=True)
            return {}

