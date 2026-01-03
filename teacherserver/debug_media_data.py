#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试媒体文件数据
检查最近接收的数据中是否包含 stages 和 mediaItems
"""

import json
import os
import sqlite3
from datetime import datetime
from pathlib import Path

# 数据目录
DATA_DIR = 'data/students'
DB_PATH = 'data/campcooking.db'

def check_json_files():
    """检查 JSON 文件中的 stages 数据"""
    print("=" * 60)
    print("检查 JSON 文件中的 stages 数据")
    print("=" * 60)
    
    if not os.path.exists(DATA_DIR):
        print(f"[ERROR] 数据目录不存在: {DATA_DIR}")
        return
    
    student_dirs = [d for d in os.listdir(DATA_DIR) if os.path.isdir(os.path.join(DATA_DIR, d))]
    
    if not student_dirs:
        print("[ERROR] 没有找到学生数据目录")
        return
    
    for student_dir in student_dirs:
        student_path = os.path.join(DATA_DIR, student_dir)
        json_files = [f for f in os.listdir(student_path) if f.endswith('.json')]
        
        if not json_files:
            continue
        
        # 获取最新的 JSON 文件
        json_files.sort(reverse=True)
        latest_json = json_files[0]
        json_path = os.path.join(student_path, latest_json)
        
        print(f"\n[学生] {student_dir}")
        print(f"   文件: {latest_json}")
        
        try:
            with open(json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            process_record = data.get('processRecord')
            if not process_record:
                print("   [ERROR] 没有 processRecord 数据")
                continue
            
            stages = process_record.get('stages')
            if not stages:
                print("   [ERROR] processRecord 中没有 'stages' 字段")
                print(f"   processRecord 的键: {list(process_record.keys())}")
                continue
            
            print(f"   [OK] 找到 {len(stages)} 个阶段")
            
            total_media = 0
            for stage_name, stage_data in stages.items():
                media_items = stage_data.get('mediaItems', [])
                if not media_items:
                    media_items = stage_data.get('media_items', [])
                
                media_count = len(media_items) if media_items else 0
                total_media += media_count
                
                if media_count > 0:
                    print(f"      [OK] {stage_name}: {media_count} 个媒体文件")
                    for idx, media_item in enumerate(media_items[:3]):  # 只显示前3个
                        print(f"         媒体 {idx+1}: path={media_item.get('path', 'N/A')[:50]}..., type={media_item.get('type', 'N/A')}")
                else:
                    print(f"      [WARN] {stage_name}: 没有媒体文件")
            
            print(f"   [统计] 总计: {total_media} 个媒体文件")
            
        except Exception as e:
            print(f"   [ERROR] 读取文件失败: {str(e)}")

def check_database():
    """检查数据库中的媒体文件数据"""
    print("\n" + "=" * 60)
    print("检查数据库中的媒体文件数据")
    print("=" * 60)
    
    if not os.path.exists(DB_PATH):
        print(f"[ERROR] 数据库文件不存在: {DB_PATH}")
        return
    
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        
        # 检查 media_items 表
        cursor.execute("SELECT COUNT(*) FROM media_items")
        media_count = cursor.fetchone()[0]
        print(f"\n[统计] media_items 表记录数: {media_count}")
        
        if media_count > 0:
            cursor.execute("""
                SELECT mi.id, mi.stage_record_id, sr.stage_name, mi.file_path, mi.file_type, mi.timestamp
                FROM media_items mi
                LEFT JOIN stage_records sr ON mi.stage_record_id = sr.id
                ORDER BY mi.created_at DESC
                LIMIT 10
            """)
            
            print("\n前10条媒体文件记录:")
            for row in cursor.fetchall():
                print(f"  ID: {row[0]}, 阶段: {row[2]}, 路径: {row[3][:50]}..., 类型: {row[4]}")
        else:
            print("[ERROR] media_items 表中没有数据")
        
        # 检查 stage_records 表
        cursor.execute("SELECT COUNT(*) FROM stage_records")
        stage_count = cursor.fetchone()[0]
        print(f"\n[统计] stage_records 表记录数: {stage_count}")
        
        if stage_count > 0:
            cursor.execute("""
                SELECT sr.id, sr.stage_name, sr.process_record_id,
                       (SELECT COUNT(*) FROM media_items mi WHERE mi.stage_record_id = sr.id) as media_count
                FROM stage_records sr
                ORDER BY sr.created_at DESC
                LIMIT 10
            """)
            
            print("\n前10条阶段记录及其媒体文件数量:")
            for row in cursor.fetchall():
                print(f"  阶段ID: {row[0]}, 阶段名: {row[1]}, 媒体文件数: {row[3]}")
        
        conn.close()
        
    except Exception as e:
        print(f"[ERROR] 检查数据库失败: {str(e)}")

def main():
    print("=" * 60)
    print("媒体文件数据调试工具")
    print(f"时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    check_json_files()
    check_database()
    
    print("\n" + "=" * 60)
    print("调试完成")
    print("=" * 60)

if __name__ == '__main__':
    main()

