#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库初始化脚本
根据数据库结构设计方案创建所有表和索引
"""

import sqlite3
import os
import logging
from typing import Optional

logger = logging.getLogger(__name__)


class DatabaseInitializer:
    """数据库初始化器"""
    
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.conn: Optional[sqlite3.Connection] = None
    
    def connect(self):
        """连接数据库"""
        self.conn = sqlite3.connect(self.db_path)
        self.conn.row_factory = sqlite3.Row  # 返回字典格式的行
        logger.info(f"连接到数据库: {self.db_path}")
    
    def close(self):
        """关闭数据库连接"""
        if self.conn:
            self.conn.close()
            logger.info("数据库连接已关闭")
    
    def execute_sql(self, sql: str):
        """执行SQL语句"""
        if not self.conn:
            raise Exception("数据库未连接")
        
        try:
            cursor = self.conn.cursor()
            cursor.execute(sql)
            self.conn.commit()
            logger.debug(f"执行SQL成功: {sql[:100]}...")
        except Exception as e:
            logger.error(f"执行SQL失败: {sql[:100]}... 错误: {str(e)}")
            raise
    
    def init_database(self):
        """初始化数据库，创建所有表和索引"""
        logger.info("开始初始化数据库...")
        
        # 1. 创建 teams 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS teams (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                team_id TEXT UNIQUE NOT NULL,
                school TEXT NOT NULL,
                grade TEXT NOT NULL,
                class_name TEXT NOT NULL,
                stove_number TEXT NOT NULL,
                member_count INTEGER NOT NULL DEFAULT 0,
                member_names TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                UNIQUE(school, grade, class_name, stove_number)
            )
        """)
        
        # 2. 创建 team_divisions 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS team_divisions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                team_id TEXT NOT NULL,
                group_leader TEXT,
                group_cooking TEXT,
                group_soup_rice TEXT,
                group_fire TEXT,
                group_health TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
                UNIQUE(team_id)
            )
        """)
        
        # 3. 创建 process_records 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS process_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                team_id TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                current_stage TEXT,
                overall_notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
                UNIQUE(team_id)
            )
        """)
        
        # 4. 创建 stage_records 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS stage_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                process_record_id INTEGER NOT NULL,
                stage_name TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                self_rating INTEGER DEFAULT 0,
                notes TEXT,
                problem_notes TEXT,
                is_completed INTEGER NOT NULL DEFAULT 0,
                selected_tags TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                FOREIGN KEY (process_record_id) REFERENCES process_records(id) ON DELETE CASCADE,
                UNIQUE(process_record_id, stage_name)
            )
        """)
        
        # 5. 创建 media_items 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS media_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stage_record_id INTEGER,
                summary_question INTEGER,
                file_path TEXT NOT NULL,
                file_type TEXT NOT NULL,
                file_size INTEGER,
                timestamp INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                FOREIGN KEY (stage_record_id) REFERENCES stage_records(id) ON DELETE CASCADE
            )
        """)
        
        # 6. 创建 summary_data 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS summary_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                team_id TEXT NOT NULL,
                answer1 TEXT,
                answer2 TEXT,
                answer3 TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
                UNIQUE(team_id)
            )
        """)
        
        # 7. 创建 teacher_evaluations 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS teacher_evaluations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                team_id TEXT NOT NULL,
                stage_name TEXT,
                rating INTEGER NOT NULL DEFAULT 0,
                comment TEXT,
                strengths TEXT,
                improvements TEXT,
                timestamp INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1,
                extra_data TEXT,
                FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
                UNIQUE(team_id)
            )
        """)
        
        # 8. 创建 data_versions 表
        self.execute_sql("""
            CREATE TABLE IF NOT EXISTS data_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                table_name TEXT NOT NULL,
                schema_version INTEGER NOT NULL,
                migration_script TEXT,
                applied_at INTEGER NOT NULL,
                description TEXT
            )
        """)
        
        # 创建索引
        logger.info("创建索引...")
        
        # teams 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_teams_team_id ON teams(team_id)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_teams_school ON teams(school)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_teams_stove_number ON teams(stove_number)")
        
        # team_divisions 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_team_divisions_team_id ON team_divisions(team_id)")
        
        # process_records 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_process_records_team_id ON process_records(team_id)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_process_records_start_time ON process_records(start_time)")
        
        # stage_records 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_stage_records_process_id ON stage_records(process_record_id)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_stage_records_stage_name ON stage_records(stage_name)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_stage_records_completed ON stage_records(is_completed)")
        
        # media_items 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_media_items_stage_id ON media_items(stage_record_id)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_media_items_file_path ON media_items(file_path)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_media_items_type ON media_items(file_type)")
        
        # summary_data 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_summary_data_team_id ON summary_data(team_id)")
        
        # teacher_evaluations 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_teacher_evaluations_team_id ON teacher_evaluations(team_id)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_teacher_evaluations_stage ON teacher_evaluations(stage_name)")
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_teacher_evaluations_timestamp ON teacher_evaluations(timestamp)")
        
        # data_versions 表索引
        self.execute_sql("CREATE INDEX IF NOT EXISTS idx_data_versions_table ON data_versions(table_name)")
        
        logger.info("数据库初始化完成！")
    
    def check_tables(self) -> bool:
        """检查表是否存在"""
        if not self.conn:
            return False
        
        cursor = self.conn.cursor()
        cursor.execute("""
            SELECT name FROM sqlite_master 
            WHERE type='table' AND name IN (
                'teams', 'team_divisions', 'process_records', 'stage_records',
                'media_items', 'summary_data', 'teacher_evaluations', 'data_versions'
            )
        """)
        tables = [row[0] for row in cursor.fetchall()]
        
        required_tables = [
            'teams', 'team_divisions', 'process_records', 'stage_records',
            'media_items', 'summary_data', 'teacher_evaluations', 'data_versions'
        ]
        
        missing_tables = set(required_tables) - set(tables)
        if missing_tables:
            logger.warning(f"缺少表: {missing_tables}")
            return False
        
        logger.info(f"所有表已存在: {tables}")
        return True


def init_database(db_path: str):
    """初始化数据库的便捷函数"""
    initializer = DatabaseInitializer(db_path)
    try:
        initializer.connect()
        initializer.init_database()
        return True
    except Exception as e:
        logger.error(f"数据库初始化失败: {str(e)}", exc_info=True)
        return False
    finally:
        initializer.close()


if __name__ == '__main__':
    import sys
    from config import Config
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 从配置文件获取数据库路径，或使用默认路径
    db_path = getattr(Config, 'DATABASE_PATH', 'data/campcooking.db')
    
    # 确保数据库目录存在
    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    
    print(f"正在初始化数据库: {db_path}")
    success = init_database(db_path)
    
    if success:
        print("数据库初始化成功！")
        sys.exit(0)
    else:
        print("数据库初始化失败！")
        sys.exit(1)

