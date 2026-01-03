#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库操作管理模块
封装所有数据库CRUD操作
"""

import sqlite3
import json
import logging
import time
import random
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime

from models import (
    Team, TeamDivision, ProcessRecord, StageRecord,
    SummaryData, TeacherEvaluation, MediaItem, STAGE_ORDER
)
from config import Config

logger = logging.getLogger(__name__)

# 数据库重试配置
MAX_RETRIES = 5  # 最大重试次数
RETRY_DELAY_BASE = 0.1  # 基础延迟（秒）
RETRY_DELAY_MAX = 1.0  # 最大延迟（秒）


class DatabaseManager:
    """数据库管理器（线程安全）"""
    
    def __init__(self, db_path: Optional[str] = None):
        self.db_path = db_path or Config.DATABASE_PATH
    
    def _get_connection(self) -> sqlite3.Connection:
        """获取数据库连接（每次请求创建新连接，线程安全）"""
        import threading
        # 为每个线程创建独立的连接
        thread_id = threading.current_thread().ident
        if not hasattr(self, '_thread_connections'):
            self._thread_connections = {}
        
        if thread_id not in self._thread_connections:
            conn = sqlite3.connect(self.db_path, check_same_thread=False, timeout=30.0)
            conn.row_factory = sqlite3.Row  # 返回字典格式的行
            # 启用外键约束
            conn.execute("PRAGMA foreign_keys = ON")
            # 启用 WAL 模式（Write-Ahead Logging）以提高并发性能
            try:
                conn.execute("PRAGMA journal_mode=WAL")
                logger.debug("已启用 WAL 模式")
            except Exception as e:
                logger.warning(f"启用 WAL 模式失败（可能已启用）: {str(e)}")
            # 设置 busy_timeout（毫秒），自动重试锁定的数据库
            conn.execute("PRAGMA busy_timeout=5000")  # 5秒超时
            self._thread_connections[thread_id] = conn
        
        return self._thread_connections[thread_id]
    
    def close(self):
        """关闭数据库连接"""
        if hasattr(self, '_thread_connections'):
            import threading
            thread_id = threading.current_thread().ident
            if thread_id in self._thread_connections:
                self._thread_connections[thread_id].close()
                del self._thread_connections[thread_id]
    
    def _execute(self, sql: str, params: tuple = ()) -> sqlite3.Cursor:
        """执行SQL语句（带重试机制）"""
        last_exception = None
        
        for attempt in range(MAX_RETRIES):
            try:
                conn = self._get_connection()
                cursor = conn.cursor()
                cursor.execute(sql, params)
                conn.commit()
                return cursor
            except sqlite3.OperationalError as e:
                error_msg = str(e).lower()
                # 检查是否是数据库锁定错误
                if 'locked' in error_msg or 'database is locked' in error_msg:
                    last_exception = e
                    if attempt < MAX_RETRIES - 1:
                        # 指数退避 + 随机抖动，避免同时重试
                        delay = min(
                            RETRY_DELAY_BASE * (2 ** attempt) + random.uniform(0, 0.1),
                            RETRY_DELAY_MAX
                        )
                        logger.warning(
                            f"数据库被锁定，{delay:.2f}秒后重试 "
                            f"({attempt + 1}/{MAX_RETRIES}): {str(e)}"
                        )
                        time.sleep(delay)
                        continue
                    else:
                        logger.error(f"数据库锁定，已达到最大重试次数: {str(e)}")
                        raise
                else:
                    # 其他类型的错误，直接抛出
                    logger.error(f"数据库操作失败: {str(e)}")
                    raise
            except Exception as e:
                # 非数据库锁定错误，直接抛出
                logger.error(f"数据库操作失败: {str(e)}")
                raise
        
        # 如果所有重试都失败
        if last_exception:
            raise last_exception
        raise Exception("数据库操作失败：未知错误")
    
    def _fetch_one(self, sql: str, params: tuple = ()) -> Optional[Dict[str, Any]]:
        """执行查询并返回单条记录"""
        cursor = self._execute(sql, params)
        row = cursor.fetchone()
        if row:
            return dict(row)
        return None
    
    def _fetch_all(self, sql: str, params: tuple = ()) -> List[Dict[str, Any]]:
        """执行查询并返回所有记录"""
        cursor = self._execute(sql, params)
        rows = cursor.fetchall()
        return [dict(row) for row in rows]
    
    # ==================== Teams 操作 ====================
    
    def save_team(self, team: Team) -> int:
        """保存或更新团队信息"""
        try:
            # 检查是否已存在
            existing = self._fetch_one(
                "SELECT id FROM teams WHERE team_id = ?",
                (team.team_id,)
            )
            
            if existing:
                # 更新
                team.update_timestamp()
                self._execute("""
                    UPDATE teams SET
                        school = ?, grade = ?, class_name = ?, stove_number = ?,
                        member_count = ?, member_names = ?,
                        updated_at = ?, schema_version = ?, extra_data = ?
                    WHERE team_id = ?
                """, (
                    team.school, team.grade, team.class_name, team.stove_number,
                    team.member_count, team.member_names,
                    team.updated_at, team.schema_version, team.extra_data,
                    team.team_id
                ))
                team.id = existing['id']
                logger.info(f"更新团队: {team.team_id}")
            else:
                # 插入
                cursor = self._execute("""
                    INSERT INTO teams (
                        team_id, school, grade, class_name, stove_number,
                        member_count, member_names,
                        created_at, updated_at, schema_version, extra_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    team.team_id, team.school, team.grade, team.class_name, team.stove_number,
                    team.member_count, team.member_names,
                    team.created_at, team.updated_at, team.schema_version, team.extra_data
                ))
                team.id = cursor.lastrowid
                logger.info(f"插入团队: {team.team_id}")
            
            return team.id
        except Exception as e:
            logger.error(f"保存团队失败: {str(e)}", exc_info=True)
            raise
    
    def get_team(self, team_id: str) -> Optional[Team]:
        """获取团队信息"""
        try:
            row = self._fetch_one("SELECT * FROM teams WHERE team_id = ?", (team_id,))
            if row:
                return Team(row)
            return None
        except Exception as e:
            logger.error(f"获取团队失败: {str(e)}", exc_info=True)
            return None
    
    def get_all_teams(self) -> List[Team]:
        """获取所有团队"""
        try:
            rows = self._fetch_all("SELECT * FROM teams ORDER BY school, grade, class_name, stove_number")
            return [Team(row) for row in rows]
        except Exception as e:
            logger.error(f"获取所有团队失败: {str(e)}", exc_info=True)
            return []
    
    # ==================== Team Divisions 操作 ====================
    
    def save_team_division(self, team_id: str, division: TeamDivision) -> int:
        """保存或更新团队分工（一对一关系）"""
        try:
            division.team_id = team_id
            
            # 检查是否已存在
            existing = self._fetch_one(
                "SELECT id FROM team_divisions WHERE team_id = ?",
                (team_id,)
            )
            
            if existing:
                # 更新
                division.update_timestamp()
                self._execute("""
                    UPDATE team_divisions SET
                        group_leader = ?, group_cooking = ?, group_soup_rice = ?,
                        group_fire = ?, group_health = ?,
                        updated_at = ?, schema_version = ?, extra_data = ?
                    WHERE team_id = ?
                """, (
                    division.group_leader, division.group_cooking, division.group_soup_rice,
                    division.group_fire, division.group_health,
                    division.updated_at, division.schema_version, division.extra_data,
                    team_id
                ))
                division.id = existing['id']
                logger.info(f"更新团队分工: {team_id}")
            else:
                # 插入
                cursor = self._execute("""
                    INSERT INTO team_divisions (
                        team_id, group_leader, group_cooking, group_soup_rice,
                        group_fire, group_health,
                        created_at, updated_at, schema_version, extra_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    team_id, division.group_leader, division.group_cooking, division.group_soup_rice,
                    division.group_fire, division.group_health,
                    division.created_at, division.updated_at, division.schema_version, division.extra_data
                ))
                division.id = cursor.lastrowid
                logger.info(f"插入团队分工: {team_id}")
            
            return division.id
        except Exception as e:
            logger.error(f"保存团队分工失败: {str(e)}", exc_info=True)
            raise
    
    def get_team_division(self, team_id: str) -> Optional[TeamDivision]:
        """获取团队分工"""
        try:
            row = self._fetch_one("SELECT * FROM team_divisions WHERE team_id = ?", (team_id,))
            if row:
                return TeamDivision(row)
            return None
        except Exception as e:
            logger.error(f"获取团队分工失败: {str(e)}", exc_info=True)
            return None
    
    # ==================== Process Records 操作 ====================
    
    def save_process_record(self, team_id: str, process_record: ProcessRecord, stages: List[StageRecord], stages_media: Optional[Dict[str, List[Dict[str, Any]]]] = None) -> int:
        """保存或更新过程记录和阶段记录（使用事务）"""
        conn = self._get_connection()
        try:
            conn.execute("BEGIN TRANSACTION")
            
            process_record.team_id = team_id
            
            # 检查是否已存在过程记录
            existing = self._fetch_one(
                "SELECT id FROM process_records WHERE team_id = ?",
                (team_id,)
            )
            
            if existing:
                # 更新过程记录
                process_record.update_timestamp()
                self._execute("""
                    UPDATE process_records SET
                        start_time = ?, end_time = ?, current_stage = ?, overall_notes = ?,
                        updated_at = ?, schema_version = ?, extra_data = ?
                    WHERE team_id = ?
                """, (
                    process_record.start_time, process_record.end_time,
                    process_record.current_stage, process_record.overall_notes,
                    process_record.updated_at, process_record.schema_version, process_record.extra_data,
                    team_id
                ))
                process_record.id = existing['id']
                # 删除旧的阶段记录
                self._execute("DELETE FROM stage_records WHERE process_record_id = ?", (process_record.id,))
                logger.info(f"更新过程记录: {team_id}")
            else:
                # 插入过程记录
                cursor = self._execute("""
                    INSERT INTO process_records (
                        team_id, start_time, end_time, current_stage, overall_notes,
                        created_at, updated_at, schema_version, extra_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    team_id, process_record.start_time, process_record.end_time,
                    process_record.current_stage, process_record.overall_notes,
                    process_record.created_at, process_record.updated_at,
                    process_record.schema_version, process_record.extra_data
                ))
                process_record.id = cursor.lastrowid
                logger.info(f"插入过程记录: {team_id}")
            
            # 插入所有阶段记录
            for stage in stages:
                stage.process_record_id = process_record.id
                stage.update_timestamp()
                cursor = self._execute("""
                    INSERT INTO stage_records (
                        process_record_id, stage_name, start_time, end_time,
                        self_rating, notes, problem_notes, is_completed, selected_tags,
                        created_at, updated_at, schema_version, extra_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    stage.process_record_id, stage.stage_name, stage.start_time, stage.end_time,
                    stage.self_rating, stage.notes, stage.problem_notes,
                    1 if stage.is_completed else 0,
                    json.dumps(stage.selected_tags, ensure_ascii=False) if stage.selected_tags else '[]',
                    stage.created_at, stage.updated_at, stage.schema_version, stage.extra_data
                ))
                stage.id = cursor.lastrowid
                
                # 删除该阶段旧的媒体文件（如果存在）
                self._execute("DELETE FROM media_items WHERE stage_record_id = ?", (stage.id,))
                
                # 保存该阶段的媒体文件
                logger.info(f"处理阶段 {stage.stage_name} (ID: {stage.id}) 的媒体文件")
                logger.info(f"stages_media 的键: {list(stages_media.keys()) if stages_media else []}")
                
                if stages_media and stage.stage_name in stages_media:
                    media_items = stages_media[stage.stage_name]
                    logger.info(f"找到 {len(media_items)} 个媒体文件需要保存")
                    
                    for idx, media_data in enumerate(media_items):
                        try:
                            logger.debug(f"处理媒体文件 {idx+1}: {media_data}")
                            media_item = MediaItem(media_data)
                            media_item.stage_record_id = stage.id
                            media_item.update_timestamp()
                            
                            # 确保 timestamp 有值
                            if not media_item.timestamp:
                                media_item.timestamp = media_item.created_at
                            
                            logger.info(f"保存媒体文件: path={media_item.file_path}, type={media_item.file_type}, timestamp={media_item.timestamp}")
                            
                            self._execute("""
                                INSERT INTO media_items (
                                    stage_record_id, summary_question, file_path, file_type,
                                    file_size, timestamp, created_at, schema_version, extra_data
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, (
                                media_item.stage_record_id, media_item.summary_question,
                                media_item.file_path, media_item.file_type,
                                media_item.file_size, media_item.timestamp,
                                media_item.created_at, media_item.schema_version, media_item.extra_data
                            ))
                            logger.info(f"✅ 成功保存媒体文件: {media_item.file_path}")
                        except Exception as e:
                            logger.error(f"保存媒体文件失败: {str(e)}, 数据: {media_data}", exc_info=True)
                            # 继续处理其他媒体文件
                else:
                    logger.info(f"阶段 {stage.stage_name} 没有媒体文件需要保存")
            
            conn.commit()
            media_count = sum(len(media_list) for media_list in (stages_media or {}).values())
            logger.info(f"保存过程记录和{len(stages)}个阶段记录，{media_count}个媒体文件: {team_id}")
            return process_record.id
            
        except Exception as e:
            conn.rollback()
            logger.error(f"保存过程记录失败: {str(e)}", exc_info=True)
            raise
    
    def get_process_record(self, team_id: str) -> Optional[Tuple[ProcessRecord, List[StageRecord]]]:
        """获取过程记录及所有阶段记录（按STAGE_ORDER排序）"""
        try:
            # 获取过程记录
            process_row = self._fetch_one(
                "SELECT * FROM process_records WHERE team_id = ?",
                (team_id,)
            )
            
            if not process_row:
                return None
            
            # 确保数据是字典格式（sqlite3.Row转换为dict）
            if not isinstance(process_row, dict):
                process_row = dict(process_row)
            
            try:
                process_record = ProcessRecord(process_row)
            except Exception as e:
                logger.error(f"创建ProcessRecord对象失败: {str(e)}, 数据: {process_row}", exc_info=True)
                raise
            
            # 获取阶段记录（按固定顺序排序）
            stage_rows = self._fetch_all("""
                SELECT * FROM stage_records
                WHERE process_record_id = ?
                ORDER BY CASE stage_name
                    WHEN 'PREPARATION' THEN 1
                    WHEN 'FIRE_MAKING' THEN 2
                    WHEN 'COOKING_RICE' THEN 3
                    WHEN 'COOKING_DISHES' THEN 4
                    WHEN 'SHOWCASE' THEN 5
                    WHEN 'CLEANING' THEN 6
                    WHEN 'COMPLETED' THEN 7
                    ELSE 999
                END
            """, (process_record.id,))
            
            # 确保每个stage_row都是字典格式
            stages = []
            for row in stage_rows:
                if not isinstance(row, dict):
                    row = dict(row)
                try:
                    stage = StageRecord(row)
                    # 调试：记录阶段评分
                    logger.debug(f"从数据库读取阶段 {stage.stage_name}: self_rating={row.get('self_rating')}, 类型={type(row.get('self_rating'))}")
                    # 获取该阶段的媒体文件
                    media_rows = self._fetch_all(
                        "SELECT * FROM media_items WHERE stage_record_id = ? ORDER BY timestamp",
                        (stage.id,)
                    )
                    # 转换为MediaItem对象并添加到stage
                    media_items = []
                    for media_row in media_rows:
                        if not isinstance(media_row, dict):
                            media_row = dict(media_row)
                        media_item = MediaItem(media_row)
                        media_items.append(media_item.to_android_dict())
                    # 将媒体文件添加到stage对象（用于前端显示）
                    stage.media_items = media_items
                    stages.append(stage)
                except Exception as e:
                    logger.error(f"创建StageRecord对象失败: {str(e)}, 数据: {row}", exc_info=True)
                    raise
            
            return (process_record, stages)
        except Exception as e:
            logger.error(f"获取过程记录失败: {str(e)}", exc_info=True)
            return None
    
    # ==================== Summary Data 操作 ====================
    
    def save_summary_data(self, team_id: str, summary: SummaryData) -> int:
        """保存或更新课后总结（一对一关系）"""
        try:
            summary.team_id = team_id
            
            # 检查是否已存在
            existing = self._fetch_one(
                "SELECT id FROM summary_data WHERE team_id = ?",
                (team_id,)
            )
            
            if existing:
                # 更新
                summary.update_timestamp()
                self._execute("""
                    UPDATE summary_data SET
                        answer1 = ?, answer2 = ?, answer3 = ?,
                        updated_at = ?, schema_version = ?, extra_data = ?
                    WHERE team_id = ?
                """, (
                    summary.answer1, summary.answer2, summary.answer3,
                    summary.updated_at, summary.schema_version, summary.extra_data,
                    team_id
                ))
                summary.id = existing['id']
                logger.info(f"更新课后总结: {team_id}")
            else:
                # 插入
                cursor = self._execute("""
                    INSERT INTO summary_data (
                        team_id, answer1, answer2, answer3,
                        created_at, updated_at, schema_version, extra_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    team_id, summary.answer1, summary.answer2, summary.answer3,
                    summary.created_at, summary.updated_at, summary.schema_version, summary.extra_data
                ))
                summary.id = cursor.lastrowid
                logger.info(f"插入课后总结: {team_id}")
            
            return summary.id
        except Exception as e:
            logger.error(f"保存课后总结失败: {str(e)}", exc_info=True)
            raise
    
    def get_summary_data(self, team_id: str) -> Optional[SummaryData]:
        """获取课后总结"""
        try:
            row = self._fetch_one("SELECT * FROM summary_data WHERE team_id = ?", (team_id,))
            if row:
                return SummaryData(row)
            return None
        except Exception as e:
            logger.error(f"获取课后总结失败: {str(e)}", exc_info=True)
            return None
    
    # ==================== Teacher Evaluations 操作 ====================
    
    def save_teacher_evaluation(self, team_id: str, evaluation: TeacherEvaluation) -> int:
        """保存或更新教师评价（支持每个团队多个阶段的评价）"""
        try:
            evaluation.team_id = team_id
            
            # 确保 stage_name 不为空
            if not evaluation.stage_name:
                raise ValueError("stage_name 不能为空")
            
            # 检查是否已存在（按 team_id 和 stage_name）
            existing = self._fetch_one(
                "SELECT id FROM teacher_evaluations WHERE team_id = ? AND stage_name = ?",
                (team_id, evaluation.stage_name)
            )
            
            if existing:
                # 更新
                evaluation.update_timestamp()
                self._execute("""
                    UPDATE teacher_evaluations SET
                        rating = ?, comment = ?,
                        strengths = ?, improvements = ?, timestamp = ?,
                        updated_at = ?, schema_version = ?, extra_data = ?
                    WHERE team_id = ? AND stage_name = ?
                """, (
                    evaluation.rating, evaluation.comment,
                    evaluation.strengths, evaluation.improvements, evaluation.timestamp,
                    evaluation.updated_at, evaluation.schema_version, evaluation.extra_data,
                    team_id, evaluation.stage_name
                ))
                evaluation.id = existing['id']
                logger.info(f"更新教师评价: {team_id} - {evaluation.stage_name}")
            else:
                # 插入
                cursor = self._execute("""
                    INSERT INTO teacher_evaluations (
                        team_id, stage_name, rating, comment,
                        strengths, improvements, timestamp,
                        created_at, updated_at, schema_version, extra_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    team_id, evaluation.stage_name, evaluation.rating, evaluation.comment,
                    evaluation.strengths, evaluation.improvements, evaluation.timestamp,
                    evaluation.created_at, evaluation.updated_at, evaluation.schema_version, evaluation.extra_data
                ))
                evaluation.id = cursor.lastrowid
                logger.info(f"插入教师评价: {team_id} - {evaluation.stage_name}")
            
            return evaluation.id
        except Exception as e:
            logger.error(f"保存教师评价失败: {str(e)}", exc_info=True)
            raise
    
    def get_teacher_evaluation(self, team_id: str, stage_name: Optional[str] = None) -> Optional[TeacherEvaluation]:
        """获取教师评价（如果指定stage_name，则获取特定阶段的评价）"""
        try:
            if stage_name:
                row = self._fetch_one(
                    "SELECT * FROM teacher_evaluations WHERE team_id = ? AND stage_name = ?",
                    (team_id, stage_name)
                )
            else:
                # 兼容旧代码：如果没有指定stage_name，返回第一个找到的评价
                row = self._fetch_one(
                    "SELECT * FROM teacher_evaluations WHERE team_id = ? LIMIT 1",
                    (team_id,)
                )
            if row:
                return TeacherEvaluation(row)
            return None
        except Exception as e:
            logger.error(f"获取教师评价失败: {str(e)}", exc_info=True)
            return None
    
    def get_all_teacher_evaluations(self, team_id: str) -> Dict[str, TeacherEvaluation]:
        """获取团队所有阶段的教师评价"""
        try:
            rows = self._fetch_all(
                "SELECT * FROM teacher_evaluations WHERE team_id = ? ORDER BY CASE stage_name WHEN 'PREPARATION' THEN 1 WHEN 'FIRE_MAKING' THEN 2 WHEN 'COOKING_RICE' THEN 3 WHEN 'COOKING_DISHES' THEN 4 WHEN 'SHOWCASE' THEN 5 WHEN 'CLEANING' THEN 6 WHEN 'COMPLETED' THEN 7 ELSE 999 END",
                (team_id,)
            )
            evaluations = {}
            for row in rows:
                if not isinstance(row, dict):
                    row = dict(row)
                evaluation = TeacherEvaluation(row)
                evaluations[evaluation.stage_name] = evaluation
            return evaluations
        except Exception as e:
            logger.error(f"获取所有教师评价失败: {str(e)}", exc_info=True)
            return {}
    
    # ==================== 统计操作 ====================
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取统计数据"""
        try:
            # 总团队数
            total_teams = self._fetch_one("SELECT COUNT(*) as count FROM teams")['count']
            
            # 有过程记录的团队数
            teams_with_process = self._fetch_one(
                "SELECT COUNT(DISTINCT team_id) as count FROM process_records"
            )['count']
            
            # 有课后总结的团队数
            teams_with_summary = self._fetch_one(
                "SELECT COUNT(*) as count FROM summary_data"
            )['count']
            
            # 阶段完成统计
            stage_stats = self._fetch_one("""
                SELECT 
                    COUNT(*) as total_stages,
                    SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) as completed_stages
                FROM stage_records
            """)
            
            total_stages = stage_stats['total_stages'] or 0
            completed_stages = stage_stats['completed_stages'] or 0
            avg_completion = (completed_stages / total_stages * 100) if total_stages > 0 else 0
            
            return {
                'totalStudents': total_teams,
                'studentsWithProcess': teams_with_process,
                'studentsWithSummary': teams_with_summary,
                'averageCompletion': round(avg_completion, 2),
                'totalCompletedStages': completed_stages,
                'totalStages': total_stages
            }
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
    
    # ==================== 清空数据 ====================
    
    def clear_all_data(self) -> Dict[str, int]:
        """清空所有数据，返回删除的记录数"""
        conn = self._get_connection()
        try:
            conn.execute("BEGIN TRANSACTION")
            
            counts = {}
            
            # 按顺序删除（考虑外键约束）
            tables = [
                'stage_records',
                'process_records',
                'media_items',
                'summary_data',
                'teacher_evaluations',
                'team_divisions',
                'teams'
            ]
            
            for table in tables:
                cursor = self._execute(f"DELETE FROM {table}")
                counts[table] = cursor.rowcount
            
            conn.commit()
            logger.info(f"清空所有数据: {counts}")
            return counts
            
        except Exception as e:
            conn.rollback()
            logger.error(f"清空数据失败: {str(e)}", exc_info=True)
            raise

