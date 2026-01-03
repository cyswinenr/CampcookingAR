#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据模型定义
根据数据库结构设计方案生成
支持与Android端数据结构兼容
"""

import json
from typing import Dict, List, Optional, Any
from datetime import datetime


# ==================== 阶段名称常量 ====================
STAGE_ORDER = {
    'PREPARATION': 1,
    'FIRE_MAKING': 2,
    'COOKING_RICE': 3,
    'COOKING_DISHES': 4,
    'SHOWCASE': 5,
    'CLEANING': 6,
    'COMPLETED': 7
}

STAGE_DISPLAY_NAMES = {
    'PREPARATION': '准备阶段',
    'FIRE_MAKING': '生火',
    'COOKING_RICE': '煮饭',
    'COOKING_DISHES': '炒菜',
    'SHOWCASE': '成果展示',
    'CLEANING': '卫生清洁',
    'COMPLETED': '整体表现'
}


# ==================== 数据库模型基类 ====================
class BaseModel:
    """数据库模型基类"""
    
    def __init__(self):
        self.id: Optional[int] = None
        self.created_at: int = int(datetime.now().timestamp() * 1000)
        self.updated_at: int = int(datetime.now().timestamp() * 1000)
        self.schema_version: int = 1
        self.extra_data: Optional[str] = None
    
    def get_extra_data(self) -> Dict[str, Any]:
        """解析extra_data JSON"""
        if self.extra_data:
            try:
                return json.loads(self.extra_data)
            except:
                return {}
        return {}
    
    def set_extra_data(self, data: Dict[str, Any]):
        """设置extra_data JSON"""
        self.extra_data = json.dumps(data, ensure_ascii=False) if data else None
    
    def update_timestamp(self):
        """更新updated_at时间戳"""
        self.updated_at = int(datetime.now().timestamp() * 1000)


# ==================== 1. teams - 团队信息表 ====================
class Team(BaseModel):
    """团队信息表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.team_id = ''
        self.school = ''
        self.grade = ''
        self.class_name = ''
        self.stove_number = ''
        self.member_count = 0
        self.member_names = ''
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 确保team_id属性已初始化（防御性检查）
        if not hasattr(self, 'team_id'):
            self.team_id = ''
        
        # 兼容Android端的TeamInfo格式
        if 'teamInfo' in data:
            team_info = data['teamInfo']
            self.school = team_info.get('school', '')
            self.grade = team_info.get('grade', '')
            self.class_name = team_info.get('className', '')
            self.stove_number = team_info.get('stoveNumber', '')
            self.member_count = team_info.get('memberCount', 0)
            self.member_names = team_info.get('memberNames', '')
        else:
            # 数据库格式
            self.team_id = data.get('team_id', '')
            self.school = data.get('school', '')
            self.grade = data.get('grade', '')
            self.class_name = data.get('class_name', '')
            self.stove_number = data.get('stove_number', '')
            self.member_count = data.get('member_count', 0)
            self.member_names = data.get('member_names', '')
        
        # 生成team_id
        if not self.team_id:
            self.team_id = f"{self.school}_{self.grade}_{self.class_name}_{self.stove_number}"
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'updated_at' in data:
            self.updated_at = data['updated_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        return {
            'id': self.id,
            'team_id': self.team_id,
            'school': self.school,
            'grade': self.grade,
            'class_name': self.class_name,
            'stove_number': self.stove_number,
            'member_count': self.member_count,
            'member_names': self.member_names,
            'created_at': self.created_at,
            'updated_at': self.updated_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式"""
        return {
            'school': self.school,
            'grade': self.grade,
            'className': self.class_name,
            'stoveNumber': self.stove_number,
            'memberCount': self.member_count,
            'memberNames': self.member_names
        }
    
    def get_display_name(self) -> str:
        """获取显示名称"""
        return f"{self.school} {self.grade}年级 {self.class_name} 炉号{self.stove_number}"


# ==================== 2. team_divisions - 团队分工表 ====================
class TeamDivision(BaseModel):
    """团队分工表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.team_id = ''
        self.group_leader = ''
        self.group_cooking = ''
        self.group_soup_rice = ''
        self.group_fire = ''
        self.group_health = ''
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 初始化 team_id（如果不存在）
        if not hasattr(self, 'team_id'):
            self.team_id = ''
        
        # 兼容Android端的TeamDivision格式
        if 'teamDivision' in data:
            division_data = data['teamDivision']
            self.group_leader = division_data.get('groupLeader', '')
            self.group_cooking = division_data.get('groupCooking', '')
            self.group_soup_rice = division_data.get('groupSoupRice', '')
            self.group_fire = division_data.get('groupFire', '')
            self.group_health = division_data.get('groupHealth', '')
        else:
            # 数据库格式
            self.team_id = data.get('team_id', '')
            self.group_leader = data.get('group_leader', '')
            self.group_cooking = data.get('group_cooking', '')
            self.group_soup_rice = data.get('group_soup_rice', '')
            self.group_fire = data.get('group_fire', '')
            self.group_health = data.get('group_health', '')
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'updated_at' in data:
            self.updated_at = data['updated_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        # 确保 team_id 存在
        if not hasattr(self, 'team_id'):
            self.team_id = ''
        
        return {
            'id': self.id,
            'team_id': self.team_id,
            'group_leader': self.group_leader,
            'group_cooking': self.group_cooking,
            'group_soup_rice': self.group_soup_rice,
            'group_fire': self.group_fire,
            'group_health': self.group_health,
            'created_at': self.created_at,
            'updated_at': self.updated_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式"""
        return {
            'groupLeader': self.group_leader,
            'groupCooking': self.group_cooking,
            'groupSoupRice': self.group_soup_rice,
            'groupFire': self.group_fire,
            'groupHealth': self.group_health
        }
    
    def is_empty(self) -> bool:
        """检查是否为空"""
        return not any([
            self.group_leader,
            self.group_cooking,
            self.group_soup_rice,
            self.group_fire,
            self.group_health
        ])


# ==================== 3. process_records - 过程记录表 ====================
class ProcessRecord(BaseModel):
    """过程记录表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.team_id = ''
        self.start_time = 0
        self.end_time = None
        self.current_stage = 'PREPARATION'
        self.overall_notes = ''
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 兼容Android端的ProcessRecord格式
        if 'processRecord' in data:
            process_data = data['processRecord']
            self.start_time = process_data.get('startTime', 0)
            self.end_time = process_data.get('endTime')
            self.current_stage = process_data.get('currentStage', 'PREPARATION')
            self.overall_notes = process_data.get('overallNotes', '')
        else:
            # 数据库格式
            self.team_id = data.get('team_id', '')
            self.start_time = data.get('start_time', 0)
            self.end_time = data.get('end_time')
            self.current_stage = data.get('current_stage', 'PREPARATION')
            self.overall_notes = data.get('overall_notes', '')
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'updated_at' in data:
            self.updated_at = data['updated_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        return {
            'id': self.id,
            'team_id': self.team_id,
            'start_time': self.start_time,
            'end_time': self.end_time,
            'current_stage': self.current_stage,
            'overall_notes': self.overall_notes,
            'created_at': self.created_at,
            'updated_at': self.updated_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式（需要配合StageRecord）"""
        # 防御性检查，确保属性已初始化
        if not hasattr(self, 'start_time'):
            self.start_time = 0
        if not hasattr(self, 'end_time'):
            self.end_time = None
        if not hasattr(self, 'current_stage'):
            self.current_stage = 'PREPARATION'
        if not hasattr(self, 'overall_notes'):
            self.overall_notes = ''
        
        return {
            'startTime': self.start_time,
            'endTime': self.end_time,
            'currentStage': self.current_stage,
            'overallNotes': self.overall_notes
        }


# ==================== 4. stage_records - 阶段记录表 ====================
class StageRecord(BaseModel):
    """阶段记录表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.process_record_id = None
        self.stage_name = ''
        self.start_time = 0
        self.end_time = None
        self.self_rating = 0
        self.notes = ''
        self.problem_notes = ''
        self.is_completed = False
        self.selected_tags = []
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 兼容Android端的StageRecord格式
        if 'stage' in data:
            self.stage_name = data.get('stage', '')
            self.start_time = data.get('startTime', 0)
            self.end_time = data.get('endTime')
            self.self_rating = data.get('selfRating', 0)
            self.notes = data.get('notes', '')
            self.problem_notes = data.get('problemNotes', '')
            self.is_completed = data.get('isCompleted', False)
            self.selected_tags = data.get('selectedTags', [])
        else:
            # 数据库格式
            self.process_record_id = data.get('process_record_id')
            self.stage_name = data.get('stage_name', '')
            self.start_time = data.get('start_time', 0)
            self.end_time = data.get('end_time')
            self.self_rating = data.get('self_rating', 0)
            self.notes = data.get('notes', '')
            self.problem_notes = data.get('problem_notes', '')
            self.is_completed = bool(data.get('is_completed', 0))
            # selected_tags在数据库中存储为JSON字符串
            tags_str = data.get('selected_tags', '[]')
            if isinstance(tags_str, str):
                try:
                    self.selected_tags = json.loads(tags_str)
                except:
                    self.selected_tags = []
            else:
                self.selected_tags = tags_str or []
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'updated_at' in data:
            self.updated_at = data['updated_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        return {
            'id': self.id,
            'process_record_id': self.process_record_id,
            'stage_name': self.stage_name,
            'start_time': self.start_time,
            'end_time': self.end_time,
            'self_rating': self.self_rating,
            'notes': self.notes,
            'problem_notes': self.problem_notes,
            'is_completed': 1 if self.is_completed else 0,
            'selected_tags': json.dumps(self.selected_tags, ensure_ascii=False),
            'created_at': self.created_at,
            'updated_at': self.updated_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式"""
        # 防御性检查，确保属性已初始化
        if not hasattr(self, 'stage_name'):
            self.stage_name = ''
        if not hasattr(self, 'start_time'):
            self.start_time = 0
        if not hasattr(self, 'end_time'):
            self.end_time = None
        if not hasattr(self, 'self_rating'):
            self.self_rating = 0
        if not hasattr(self, 'notes'):
            self.notes = ''
        if not hasattr(self, 'problem_notes'):
            self.problem_notes = ''
        if not hasattr(self, 'is_completed'):
            self.is_completed = False
        if not hasattr(self, 'selected_tags'):
            self.selected_tags = []
        if not hasattr(self, 'media_items'):
            self.media_items = []
        
        result = {
            'stage': self.stage_name,
            'startTime': self.start_time,
            'endTime': self.end_time,
            'selfRating': self.self_rating,
            'notes': self.notes,
            'problemNotes': self.problem_notes,
            'isCompleted': self.is_completed,
            'selectedTags': self.selected_tags
        }
        
        # 添加媒体文件（如果存在）
        if hasattr(self, 'media_items') and self.media_items:
            result['mediaItems'] = self.media_items
        
        return result
    
    def get_duration_minutes(self) -> int:
        """获取用时（分钟）"""
        if self.start_time == 0 or self.end_time is None:
            return 0
        return int((self.end_time - self.start_time) / 1000 / 60)
    
    def get_stage_order(self) -> int:
        """获取阶段顺序"""
        return STAGE_ORDER.get(self.stage_name, 999)


# ==================== 5. media_items - 媒体文件表 ====================
class MediaItem(BaseModel):
    """媒体文件表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.stage_record_id = None
        self.summary_question = None
        self.file_path = ''
        self.file_type = 'PHOTO'  # PHOTO 或 VIDEO
        self.file_size = None
        self.timestamp = 0
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 兼容Android端的MediaItem格式
        if 'path' in data:
            self.file_path = data.get('path', '')
            # 处理 type 字段：可能是字符串 "PHOTO" 或 "VIDEO"
            type_value = data.get('type', 'PHOTO')
            if isinstance(type_value, str):
                self.file_type = type_value.upper()  # 确保大写
            else:
                self.file_type = 'PHOTO'
            # timestamp 可能为 0，使用当前时间作为默认值
            timestamp_value = data.get('timestamp', 0)
            self.timestamp = timestamp_value if timestamp_value > 0 else int(datetime.now().timestamp() * 1000)
        else:
            # 数据库格式
            self.stage_record_id = data.get('stage_record_id')
            self.summary_question = data.get('summary_question')
            self.file_path = data.get('file_path', '')
            self.file_type = data.get('file_type', 'PHOTO')
            self.file_size = data.get('file_size')
            timestamp_value = data.get('timestamp', 0)
            self.timestamp = timestamp_value if timestamp_value > 0 else int(datetime.now().timestamp() * 1000)
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        return {
            'id': self.id,
            'stage_record_id': self.stage_record_id,
            'summary_question': self.summary_question,
            'file_path': self.file_path,
            'file_type': self.file_type,
            'file_size': self.file_size,
            'timestamp': self.timestamp,
            'created_at': self.created_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式"""
        return {
            'path': self.file_path,
            'type': self.file_type,
            'timestamp': self.timestamp
        }


# ==================== 6. summary_data - 课后总结表 ====================
class SummaryData(BaseModel):
    """课后总结表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.team_id = ''
        self.answer1 = ''
        self.answer2 = ''
        self.answer3 = ''
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 兼容Android端的SummaryData格式
        if 'summaryData' in data:
            summary_data = data['summaryData']
            self.answer1 = summary_data.get('answer1', '')
            self.answer2 = summary_data.get('answer2', '')
            self.answer3 = summary_data.get('answer3', '')
        else:
            # 数据库格式
            self.team_id = data.get('team_id', '')
            self.answer1 = data.get('answer1', '')
            self.answer2 = data.get('answer2', '')
            self.answer3 = data.get('answer3', '')
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'updated_at' in data:
            self.updated_at = data['updated_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        return {
            'id': self.id,
            'team_id': self.team_id,
            'answer1': self.answer1,
            'answer2': self.answer2,
            'answer3': self.answer3,
            'created_at': self.created_at,
            'updated_at': self.updated_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式（照片通过media_items表关联）"""
        return {
            'answer1': self.answer1,
            'answer2': self.answer2,
            'answer3': self.answer3
        }


# ==================== 7. teacher_evaluations - 教师评价表 ====================
class TeacherEvaluation(BaseModel):
    """教师评价表"""
    
    def __init__(self, data: Optional[Dict[str, Any]] = None):
        super().__init__()
        # 先初始化所有属性
        self.team_id = ''
        self.stage_name = None
        self.rating = 0
        self.comment = ''
        self.strengths = ''
        self.improvements = ''
        self.timestamp = int(datetime.now().timestamp() * 1000)
        
        if data:
            self.from_dict(data)
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典创建（兼容Android端格式）"""
        # 兼容Android端的TeacherEvaluation格式
        if 'stage' in data:
            self.stage_name = data.get('stage') or None
            self.rating = data.get('rating', 0)
            self.comment = data.get('comment', '')
            self.strengths = data.get('strengths', '')
            self.improvements = data.get('improvements', '')
            self.timestamp = data.get('timestamp', int(datetime.now().timestamp() * 1000))
        else:
            # 数据库格式
            self.team_id = data.get('team_id', '')
            self.stage_name = data.get('stage_name')
            self.rating = data.get('rating', 0)
            self.comment = data.get('comment', '')
            self.strengths = data.get('strengths', '')
            self.improvements = data.get('improvements', '')
            self.timestamp = data.get('timestamp', int(datetime.now().timestamp() * 1000))
        
        # 数据库字段
        if 'id' in data:
            self.id = data['id']
        if 'created_at' in data:
            self.created_at = data['created_at']
        if 'updated_at' in data:
            self.updated_at = data['updated_at']
        if 'schema_version' in data:
            self.schema_version = data['schema_version']
        if 'extra_data' in data:
            self.extra_data = data['extra_data']
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（数据库格式）"""
        return {
            'id': self.id,
            'team_id': self.team_id,
            'stage_name': self.stage_name,
            'rating': self.rating,
            'comment': self.comment,
            'strengths': self.strengths,
            'improvements': self.improvements,
            'timestamp': self.timestamp,
            'created_at': self.created_at,
            'updated_at': self.updated_at,
            'schema_version': self.schema_version,
            'extra_data': self.extra_data
        }
    
    def to_android_dict(self) -> Dict[str, Any]:
        """转换为Android端格式"""
        return {
            'stage': self.stage_name or '',
            'rating': self.rating,
            'comment': self.comment,
            'strengths': self.strengths,
            'improvements': self.improvements,
            'timestamp': self.timestamp
        }


# ==================== 兼容性类（保持向后兼容） ====================
class TeamInfo:
    """团队信息（兼容旧代码）"""
    
    def __init__(self, data: Dict[str, Any]):
        self.school: str = data.get('school', '')
        self.grade: str = data.get('grade', '')
        self.className: str = data.get('className', '')
        self.stoveNumber: str = data.get('stoveNumber', '')
        self.memberCount: int = data.get('memberCount', 0)
        self.memberNames: str = data.get('memberNames', '')
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'school': self.school,
            'grade': self.grade,
            'className': self.className,
            'stoveNumber': self.stoveNumber,
            'memberCount': self.memberCount,
            'memberNames': self.memberNames
        }
    
    def get_student_id(self) -> str:
        """生成学生ID（用于唯一标识）"""
        return f"{self.school}_{self.grade}_{self.className}_{self.stoveNumber}"


class StudentDataPackage:
    """学生数据包（兼容旧代码）"""
    
    @staticmethod
    def from_dict(data: Dict[str, Any]) -> 'StudentDataPackage':
        """从字典创建数据包"""
        package = StudentDataPackage()
        package.teamInfo = TeamInfo(data.get('teamInfo', {}))
        
        # 处理团队分工
        team_division_data = data.get('teamDivision')
        if team_division_data:
            package.teamDivision = TeamDivision({'teamDivision': team_division_data})
        else:
            package.teamDivision = None
        
        package.processRecord = ProcessRecord({'processRecord': data.get('processRecord', {})}) if data.get('processRecord') else None
        package.summaryData = SummaryData({'summaryData': data.get('summaryData', {})}) if data.get('summaryData') else None
        package.exportTime = data.get('exportTime', 0)
        
        # 保存原始数据，以便访问stages等信息
        package._raw_data = data
        
        return package
    
    def __init__(self):
        self.teamInfo: Optional[TeamInfo] = None
        self.teamDivision: Optional[TeamDivision] = None
        self.processRecord: Optional[ProcessRecord] = None
        self.summaryData: Optional[SummaryData] = None
        self.exportTime: int = 0
        self._raw_data: Optional[Dict[str, Any]] = None  # 保存原始数据
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'teamInfo': self.teamInfo.to_dict() if self.teamInfo else {},
            'teamDivision': self.teamDivision.to_android_dict() if self.teamDivision and not self.teamDivision.is_empty() else None,
            'processRecord': self.processRecord.to_android_dict() if self.processRecord else None,
            'summaryData': self.summaryData.to_android_dict() if self.summaryData else None,
            'exportTime': self.exportTime
        }
