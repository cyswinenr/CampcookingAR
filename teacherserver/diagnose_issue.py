#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
诊断媒体文件数据问题
检查所有 JSON 文件，找出问题根源
"""

import json
import os
from datetime import datetime
from pathlib import Path

DATA_DIR = 'data/students'

def check_all_json_files():
    """检查所有 JSON 文件"""
    print("=" * 60)
    print("诊断媒体文件数据问题")
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
        
        print(f"\n[学生] {student_dir}")
        print(f"   JSON 文件数量: {len(json_files)}")
        
        # 按时间排序
        json_files.sort(reverse=True)
        
        # 检查每个文件
        files_with_stages = 0
        files_without_stages = 0
        
        for json_file in json_files:
            json_path = os.path.join(student_path, json_file)
            
            try:
                with open(json_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                
                process_record = data.get('processRecord')
                if process_record:
                    has_stages = 'stages' in process_record
                    
                    if has_stages:
                        files_with_stages += 1
                        stages = process_record.get('stages', {})
                        total_media = 0
                        for stage_name, stage_data in stages.items():
                            media_items = stage_data.get('mediaItems', [])
                            if not media_items:
                                media_items = stage_data.get('media_items', [])
                            total_media += len(media_items) if media_items else 0
                        
                        if total_media > 0:
                            print(f"   [OK] {json_file}: 包含 stages, {len(stages)} 个阶段, {total_media} 个媒体文件")
                        else:
                            print(f"   [OK] {json_file}: 包含 stages, {len(stages)} 个阶段, 但无媒体文件")
                    else:
                        files_without_stages += 1
                        if json_file == 'latest.json' or files_without_stages <= 3:
                            print(f"   [ERROR] {json_file}: 没有 stages 字段")
                            print(f"      processRecord 的键: {list(process_record.keys())}")
                else:
                    files_without_stages += 1
                    if json_file == 'latest.json':
                        print(f"   [ERROR] {json_file}: 没有 processRecord")
                        
            except Exception as e:
                print(f"   [ERROR] 读取 {json_file} 失败: {str(e)}")
        
        print(f"\n   [统计] 包含 stages: {files_with_stages} 个文件")
        print(f"   [统计] 不包含 stages: {files_without_stages} 个文件")
        
        if files_without_stages > 0 and files_with_stages == 0:
            print(f"   [结论] [WARN] 所有文件都没有 stages 字段！")
            print(f"   [原因] Android 端没有发送 stages 数据")
            print(f"   [解决] 需要检查 Android 端代码和数据提交逻辑")
        elif files_with_stages > 0:
            print(f"   [结论] ✅ 有文件包含 stages 字段")
            print(f"   [说明] 最新提交可能没有包含 stages")

def check_android_code_fix():
    """检查 Android 端代码修复状态"""
    print("\n" + "=" * 60)
    print("检查 Android 端代码修复")
    print("=" * 60)
    
    android_file = '../app/src/main/java/com/campcooking/ar/utils/DataSubmitManager.kt'
    
    if os.path.exists(android_file):
        print(f"[OK] 找到 Android 代码文件")
        
        with open(android_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检查关键修复
        checks = [
            ('"stages" to stagesMap', '包含 stages 字段'),
            ('确保 stages 字段始终存在', '有注释说明'),
            ('Log.d(TAG, "✅ 过程记录包含', '有调试日志'),
        ]
        
        for check_str, desc in checks:
            if check_str in content:
                print(f"   [OK] {desc}")
            else:
                print(f"   [WARN] 未找到: {desc}")
    else:
        print(f"[WARN] 未找到 Android 代码文件: {android_file}")
        print(f"   请手动检查 Android 端代码是否已修复")

def main():
    check_all_json_files()
    check_android_code_fix()
    
    print("\n" + "=" * 60)
    print("诊断建议")
    print("=" * 60)
    print("1. 如果所有 JSON 文件都没有 stages 字段：")
    print("   - Android 端没有发送 stages 数据")
    print("   - 需要检查 Android 端代码是否已修复")
    print("   - 需要重新编译并运行 Android 应用")
    print("")
    print("2. 如果部分文件有 stages 字段：")
    print("   - 说明修复是有效的")
    print("   - 最新提交可能没有包含 stages（学生没有访问过阶段）")
    print("")
    print("3. 检查服务器日志：")
    print("   - 查看服务器运行日志")
    print("   - 确认是否显示'包含 stages 字段: True'")
    print("")
    print("4. 检查 Android 端：")
    print("   - 查看 Android Logcat 日志")
    print("   - 确认是否显示'✅ 过程记录包含 X 个阶段'")

if __name__ == '__main__':
    main()

