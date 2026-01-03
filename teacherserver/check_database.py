#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查数据库中的数据
"""

import sqlite3
import os
from config import Config

def check_database():
    """检查数据库中的数据"""
    db_path = Config.DATABASE_PATH
    print(f"数据库文件路径: {os.path.abspath(db_path)}")
    print(f"文件是否存在: {os.path.exists(db_path)}")
    
    if not os.path.exists(db_path):
        print("[错误] 数据库文件不存在！")
        return
    
    if os.path.getsize(db_path) == 0:
        print("[错误] 数据库文件为空！")
        return
    
    print(f"文件大小: {os.path.getsize(db_path)} 字节")
    print("\n" + "="*60)
    print("数据库内容检查")
    print("="*60)
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 获取所有表
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
        tables = cursor.fetchall()
        print(f"\n数据库中的表 ({len(tables)} 个):")
        for table in tables:
            table_name = table[0]
            cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
            count = cursor.fetchone()[0]
            print(f"  - {table_name}: {count} 条记录")
        
        # 检查每个表的数据
        print("\n" + "="*60)
        print("详细数据检查")
        print("="*60)
        
        # 1. teams 表
        cursor.execute("SELECT COUNT(*) FROM teams")
        teams_count = cursor.fetchone()[0]
        print(f"\n1. teams 表: {teams_count} 条记录")
        if teams_count > 0:
            cursor.execute("SELECT team_id, school, grade, class_name, stove_number FROM teams LIMIT 5")
            teams = cursor.fetchall()
            for team in teams:
                print(f"   - {team[0]} ({team[1]} {team[2]} {team[3]} {team[4]})")
        
        # 2. team_divisions 表
        cursor.execute("SELECT COUNT(*) FROM team_divisions")
        divisions_count = cursor.fetchone()[0]
        print(f"\n2. team_divisions 表: {divisions_count} 条记录")
        
        # 3. process_records 表
        cursor.execute("SELECT COUNT(*) FROM process_records")
        process_count = cursor.fetchone()[0]
        print(f"\n3. process_records 表: {process_count} 条记录")
        if process_count > 0:
            cursor.execute("SELECT team_id, start_time, current_stage FROM process_records LIMIT 5")
            processes = cursor.fetchall()
            for proc in processes:
                print(f"   - {proc[0]} (阶段: {proc[2]})")
        
        # 4. stage_records 表
        cursor.execute("SELECT COUNT(*) FROM stage_records")
        stages_count = cursor.fetchone()[0]
        print(f"\n4. stage_records 表: {stages_count} 条记录")
        if stages_count > 0:
            cursor.execute("SELECT stage_name, COUNT(*) FROM stage_records GROUP BY stage_name")
            stages = cursor.fetchall()
            for stage in stages:
                print(f"   - {stage[0]}: {stage[1]} 条记录")
        
        # 5. media_items 表
        cursor.execute("SELECT COUNT(*) FROM media_items")
        media_count = cursor.fetchone()[0]
        print(f"\n5. media_items 表: {media_count} 条记录")
        if media_count > 0:
            cursor.execute("SELECT file_type, COUNT(*) FROM media_items GROUP BY file_type")
            media_types = cursor.fetchall()
            for mt in media_types:
                print(f"   - {mt[0]}: {mt[1]} 个文件")
        
        # 6. summary_data 表
        cursor.execute("SELECT COUNT(*) FROM summary_data")
        summary_count = cursor.fetchone()[0]
        print(f"\n6. summary_data 表: {summary_count} 条记录")
        
        # 7. teacher_evaluations 表
        cursor.execute("SELECT COUNT(*) FROM teacher_evaluations")
        eval_count = cursor.fetchone()[0]
        print(f"\n7. teacher_evaluations 表: {eval_count} 条记录")
        
        conn.close()
        print("\n" + "="*60)
        print("[完成] 检查完成")
        print("="*60)
        
    except Exception as e:
        print(f"[错误] 检查失败: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    check_database()

