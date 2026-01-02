#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据模型定义
与Android端数据结构完全兼容
"""

from typing import Dict, List, Optional, Any
from datetime import datetime


class TeamInfo:
    """团队信息"""
    
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
    
    def get_team_name(self) -> str:
        return f"{self.school} {self.grade}年级 {self.className} 炉号{self.stoveNumber}"
    
    def get_student_id(self) -> str:
        """生成学生ID（用于唯一标识）"""
        return f"{self.school}_{self.grade}_{self.className}_{self.stoveNumber}"


class MediaItem:
    """媒体项（照片/视频）"""
    
    def __init__(self, data: Dict[str, Any]):
        self.path: str = data.get('path', '')
        self.type: str = data.get('type', 'PHOTO')  # PHOTO 或 VIDEO
        self.timestamp: int = data.get('timestamp', 0)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'path': self.path,
            'type': self.type,
            'timestamp': self.timestamp
        }


class StageRecord:
    """阶段记录"""
    
    def __init__(self, data: Dict[str, Any]):
        self.stage: str = data.get('stage', '')  # 阶段名称
        self.startTime: int = data.get('startTime', 0)
        self.endTime: Optional[int] = data.get('endTime')
        self.photos: List[str] = data.get('photos', [])
        self.mediaItems: List[MediaItem] = [
            MediaItem(item) for item in data.get('mediaItems', [])
        ]
        self.selfRating: int = data.get('selfRating', 0)
        self.selectedTags: List[str] = data.get('selectedTags', [])
        self.notes: str = data.get('notes', '')
        self.problemNotes: str = data.get('problemNotes', '')
        self.isCompleted: bool = data.get('isCompleted', False)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'stage': self.stage,
            'startTime': self.startTime,
            'endTime': self.endTime,
            'photos': self.photos,
            'mediaItems': [item.to_dict() for item in self.mediaItems],
            'selfRating': self.selfRating,
            'selectedTags': self.selectedTags,
            'notes': self.notes,
            'problemNotes': self.problemNotes,
            'isCompleted': self.isCompleted
        }


class ProcessRecord:
    """过程记录"""
    
    def __init__(self, data: Dict[str, Any]):
        self.teamInfo: TeamInfo = TeamInfo(data.get('teamInfo', {}))
        self.startTime: int = data.get('startTime', 0)
        self.endTime: Optional[int] = data.get('endTime')
        self.stages: Dict[str, StageRecord] = {}
        
        # 解析阶段数据
        stages_data = data.get('stages', {})
        if isinstance(stages_data, dict):
            for stage_name, stage_data in stages_data.items():
                self.stages[stage_name] = StageRecord(stage_data)
        
        self.currentStage: str = data.get('currentStage', 'PREPARATION')
        self.overallNotes: str = data.get('overallNotes', '')
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'teamInfo': self.teamInfo.to_dict(),
            'startTime': self.startTime,
            'endTime': self.endTime,
            'stages': {k: v.to_dict() for k, v in self.stages.items()},
            'currentStage': self.currentStage,
            'overallNotes': self.overallNotes
        }
    
    def get_completed_stages_count(self) -> int:
        """获取完成的阶段数"""
        return sum(1 for stage in self.stages.values() if stage.isCompleted)
    
    def get_total_stages_count(self) -> int:
        """获取总阶段数"""
        return len(self.stages)


class SummaryData:
    """课后总结数据"""
    
    def __init__(self, data: Dict[str, Any]):
        self.answer1: str = data.get('answer1', '')
        self.answer2: str = data.get('answer2', '')
        self.answer3: str = data.get('answer3', '')
        self.photos1: List[str] = data.get('photos1', [])
        self.photos2: List[str] = data.get('photos2', [])
        self.photos3: List[str] = data.get('photos3', [])
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'answer1': self.answer1,
            'answer2': self.answer2,
            'answer3': self.answer3,
            'photos1': self.photos1,
            'photos2': self.photos2,
            'photos3': self.photos3
        }


class StudentDataPackage:
    """学生数据包（完整数据）"""
    
    @staticmethod
    def from_dict(data: Dict[str, Any]) -> 'StudentDataPackage':
        """从字典创建数据包"""
        package = StudentDataPackage()
        package.teamInfo = TeamInfo(data.get('teamInfo', {}))
        package.processRecord = ProcessRecord(data.get('processRecord', {})) if data.get('processRecord') else None
        package.summaryData = SummaryData(data.get('summaryData', {})) if data.get('summaryData') else None
        package.exportTime = data.get('exportTime', 0)
        return package
    
    def __init__(self):
        self.teamInfo: Optional[TeamInfo] = None
        self.processRecord: Optional[ProcessRecord] = None
        self.summaryData: Optional[SummaryData] = None
        self.exportTime: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'teamInfo': self.teamInfo.to_dict() if self.teamInfo else {},
            'processRecord': self.processRecord.to_dict() if self.processRecord else None,
            'summaryData': self.summaryData.to_dict() if self.summaryData else None,
            'exportTime': self.exportTime
        }


class TeacherEvaluation:
    """教师评价"""
    
    @staticmethod
    def from_dict(data: Dict[str, Any]) -> 'TeacherEvaluation':
        """从字典创建评价"""
        eval_obj = TeacherEvaluation()
        eval_obj.stage = data.get('stage', '')
        eval_obj.rating = data.get('rating', 0)
        eval_obj.comment = data.get('comment', '')
        eval_obj.strengths = data.get('strengths', '')
        eval_obj.improvements = data.get('improvements', '')
        eval_obj.timestamp = data.get('timestamp', int(datetime.now().timestamp() * 1000))
        return eval_obj
    
    def __init__(self):
        self.stage: str = ''  # 评价的阶段
        self.rating: int = 0  # 评分 1-5
        self.comment: str = ''  # 评价内容
        self.strengths: str = ''  # 优点
        self.improvements: str = ''  # 需要改进的地方
        self.timestamp: int = int(datetime.now().timestamp() * 1000)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'stage': self.stage,
            'rating': self.rating,
            'comment': self.comment,
            'strengths': self.strengths,
            'improvements': self.improvements,
            'timestamp': self.timestamp
        }

