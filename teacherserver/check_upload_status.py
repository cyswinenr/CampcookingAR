#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查文件上传状态"""

import sqlite3
import os
import json

db_path = 'data/campcooking.db'
media_dir = 'data/media'

print("=" * 60)
print("文件上传状态检查")
print("=" * 60)

# 1. 检查数据库中的媒体文件
if os.path.exists(db_path):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    cursor.execute("SELECT COUNT(*) FROM media_items")
    total_count = cursor.fetchone()[0]
    print(f"\n[数据库] media_items 表记录数: {total_count}")
    
    if total_count > 0:
        cursor.execute("""
            SELECT file_path, file_type, 
                   CASE 
                       WHEN file_path LIKE '/storage/%' THEN 'Android路径'
                       WHEN file_path LIKE 'storage/%' THEN 'Android路径(相对)'
                       ELSE '服务器路径'
                   END as path_type
            FROM media_items 
            LIMIT 10
        """)
        rows = cursor.fetchall()
        
        android_paths = 0
        server_paths = 0
        
        print("\n前10条记录:")
        for row in rows:
            path, file_type, path_type = row
            print(f"  {path_type}: {os.path.basename(path)} ({file_type})")
            if 'Android' in path_type:
                android_paths += 1
            else:
                server_paths += 1
        
        print(f"\n路径类型统计:")
        print(f"  Android路径: {android_paths}")
        print(f"  服务器路径: {server_paths}")
    else:
        print("  ⚠️ 数据库中没有媒体文件记录")
    
    conn.close()
else:
    print("\n[数据库] ❌ 数据库文件不存在")

# 2. 检查上传的文件
print(f"\n[文件系统] 检查上传目录: {media_dir}")
if os.path.exists(media_dir):
    student_dirs = [d for d in os.listdir(media_dir) if os.path.isdir(os.path.join(media_dir, d))]
    
    if student_dirs:
        print(f"  找到 {len(student_dirs)} 个学生目录:")
        total_files = 0
        for student_dir in student_dirs:
            student_path = os.path.join(media_dir, student_dir)
            files = [f for f in os.listdir(student_path) if os.path.isfile(os.path.join(student_path, f))]
            if files:
                print(f"    {student_dir}: {len(files)} 个文件")
                total_files += len(files)
            else:
                print(f"    {student_dir}: 无文件")
        
        print(f"\n  总计上传文件数: {total_files}")
        
        if total_files == 0:
            print("  [WARN] 没有文件被上传到服务器")
    else:
        print("  [WARN] 没有学生目录")
else:
    print("  ⚠️ 上传目录不存在")

# 3. 检查最近的JSON文件
print(f"\n[JSON数据] 检查最近的数据提交")
data_dir = 'data/students'
if os.path.exists(data_dir):
    student_dirs = [d for d in os.listdir(data_dir) if os.path.isdir(os.path.join(data_dir, d))]
    
    if student_dirs:
        for student_dir in student_dirs[:3]:  # 只检查前3个
            latest_json = os.path.join(data_dir, student_dir, 'latest.json')
            if os.path.exists(latest_json):
                try:
                    with open(latest_json, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                    
                    process_record = data.get('processRecord', {})
                    stages = process_record.get('stages', {})
                    
                    if stages:
                        total_media = 0
                        for stage_name, stage_data in stages.items():
                            media_items = stage_data.get('mediaItems', [])
                            if not media_items:
                                media_items = stage_data.get('media_items', [])
                            total_media += len(media_items) if media_items else 0
                        
                        print(f"  {student_dir}: {len(stages)} 个阶段, {total_media} 个媒体文件")
                    else:
                        print(f"  {student_dir}: [WARN] 没有stages数据")
                except Exception as e:
                    print(f"  {student_dir}: [ERROR] 读取失败: {e}")

# 4. 诊断结果
print("\n" + "=" * 60)
print("诊断结果")
print("=" * 60)

if total_count > 0:
    if android_paths > 0:
        print("[问题] 数据库中有Android路径，说明文件未上传")
        print("   解决方案：")
        print("   1. 确保Android应用已重新编译")
        print("   2. 重新提交数据（会自动上传文件）")
        print("   3. 检查Android Logcat日志，查看上传是否成功")
    elif server_paths > 0:
        print("[OK] 数据库路径正确（服务器路径）")
        if total_files == 0:
            print("[WARN] 但服务器上没有文件，可能文件被删除或路径不匹配")
        else:
            print(f"[OK] 服务器上有 {total_files} 个文件")
else:
    print("[WARN] 数据库中没有媒体文件记录")
    print("   可能原因：")
    print("   1. 数据提交时没有包含stages")
    print("   2. Android应用未重新编译")

print("=" * 60)

