#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试阶段评分数据读取
"""

import sqlite3
import json
from config import Config
from db_manager import DatabaseManager
from storage import DataStorage

def test_database_ratings():
    """直接查询数据库中的评分数据"""
    print("=" * 80)
    print("1. 直接查询数据库")
    print("=" * 80)
    
    db_path = Config.DATABASE_PATH
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT 
            t.team_id,
            t.school,
            t.grade,
            t.class_name,
            t.stove_number,
            sr.stage_name,
            sr.self_rating,
            sr.is_completed
        FROM teams t
        LEFT JOIN process_records pr ON t.team_id = pr.team_id
        LEFT JOIN stage_records sr ON pr.id = sr.process_record_id
        WHERE sr.stage_name IS NOT NULL
        ORDER BY t.stove_number, sr.stage_name
    """)
    
    rows = cursor.fetchall()
    if rows:
        current_team = None
        for row in rows:
            team_id = row['team_id']
            if team_id != current_team:
                print(f"\n团队: {team_id} ({row['school']} {row['grade']}{row['class_name']} {row['stove_number']})")
                current_team = team_id
            print(f"  阶段: {row['stage_name']:20s} | 评分: {row['self_rating']:2d} | 完成: {row['is_completed']}")
    else:
        print("数据库中没有阶段记录数据")
    
    conn.close()
    print()

def test_storage_ratings():
    """测试 storage.get_all_students() 返回的数据"""
    print("=" * 80)
    print("2. 测试 storage.get_all_students()")
    print("=" * 80)
    
    storage = DataStorage(Config.DATA_DIR, Config.MEDIA_DIR)
    students = storage.get_all_students()
    
    for student in students[:3]:  # 只显示前3个
        print(f"\n学生ID: {student['id']}")
        print(f"  有过程记录: {student['hasProcessRecord']}")
        stage_ratings = student.get('stageRatings', {})
        print(f"  阶段评分数量: {len(stage_ratings)}")
        if stage_ratings:
            for stage_name, rating_data in stage_ratings.items():
                print(f"    {stage_name}: {rating_data}")
        else:
            print("    无评分数据")
    print()

def test_api_response():
    """测试 API 返回的数据格式"""
    print("=" * 80)
    print("3. 测试 API 响应格式")
    print("=" * 80)
    
    storage = DataStorage(Config.DATA_DIR, Config.MEDIA_DIR)
    students = storage.get_all_students()
    
    # 模拟 API 响应
    result = []
    for student in students[:2]:  # 只显示前2个
        result.append({
            'id': student['id'],
            'stageRatings': student.get('stageRatings', {})
        })
    
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print()

if __name__ == '__main__':
    test_database_ratings()
    test_storage_ratings()
    test_api_response()

