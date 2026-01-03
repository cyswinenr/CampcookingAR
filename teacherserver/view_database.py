#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库查看工具
用于查看SQLite数据库中的数据
"""

import sqlite3
import json
import os
from config import Config
from datetime import datetime

# 阶段名称映射
STAGE_NAMES = {
    'PREPARATION': '准备阶段',
    'FIRE_MAKING': '生火',
    'COOKING_RICE': '煮饭',
    'COOKING_DISHES': '炒菜',
    'SHOWCASE': '成果展示',
    'CLEANING': '卫生清洁',
    'COMPLETED': '整体表现'
}


def format_timestamp(ts):
    """格式化时间戳"""
    if ts:
        try:
            return datetime.fromtimestamp(ts / 1000).strftime('%Y-%m-%d %H:%M:%S')
        except:
            return str(ts)
    return '-'


def view_database():
    """查看数据库内容"""
    db_path = Config.DATABASE_PATH
    
    if not os.path.exists(db_path):
        print(f"❌ 数据库文件不存在: {db_path}")
        print(f"   请先运行 '初始化数据库.bat' 创建数据库")
        return
    
    print("=" * 80)
    print(f"数据库文件: {db_path}")
    print("=" * 80)
    print()
    
    try:
        conn = sqlite3.connect(db_path)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # 1. 查看所有表
        print("📊 数据库表列表:")
        print("-" * 80)
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
        tables = [row[0] for row in cursor.fetchall()]
        for i, table in enumerate(tables, 1):
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            print(f"  {i}. {table:30} ({count} 条记录)")
        print()
        
        # 2. 查看 teams 表
        print("=" * 80)
        print("👥 teams 表 - 团队信息")
        print("=" * 80)
        cursor.execute("SELECT * FROM teams ORDER BY created_at DESC LIMIT 10")
        teams = cursor.fetchall()
        if teams:
            for team in teams:
                print(f"\n团队ID: {team['team_id']}")
                print(f"  学校: {team['school']}")
                print(f"  年级: {team['grade']}")
                print(f"  班级: {team['class_name']}")
                print(f"  炉号: {team['stove_number']}")
                print(f"  成员数: {team['member_count']}")
                print(f"  成员: {team['member_names']}")
                print(f"  创建时间: {format_timestamp(team['created_at'])}")
                print(f"  更新时间: {format_timestamp(team['updated_at'])}")
        else:
            print("  (无数据)")
        print()
        
        # 3. 查看 team_divisions 表
        print("=" * 80)
        print("👨‍👩‍👧‍👦 team_divisions 表 - 团队分工")
        print("=" * 80)
        cursor.execute("SELECT * FROM team_divisions ORDER BY updated_at DESC LIMIT 10")
        divisions = cursor.fetchall()
        if divisions:
            for div in divisions:
                print(f"\n团队ID: {div['team_id']}")
                print(f"  项目组长: {div['group_leader'] or '-'}")
                print(f"  烹饪组: {div['group_cooking'] or '-'}")
                print(f"  汤饭组: {div['group_soup_rice'] or '-'}")
                print(f"  生火组: {div['group_fire'] or '-'}")
                print(f"  卫生组: {div['group_health'] or '-'}")
                print(f"  更新时间: {format_timestamp(div['updated_at'])}")
        else:
            print("  (无数据)")
        print()
        
        # 4. 查看 process_records 表
        print("=" * 80)
        print("📝 process_records 表 - 过程记录")
        print("=" * 80)
        cursor.execute("SELECT * FROM process_records ORDER BY updated_at DESC LIMIT 10")
        process_records = cursor.fetchall()
        if process_records:
            for pr in process_records:
                print(f"\n记录ID: {pr['id']}")
                print(f"  团队ID: {pr['team_id']}")
                print(f"  开始时间: {format_timestamp(pr['start_time'])}")
                print(f"  结束时间: {format_timestamp(pr['end_time']) if pr['end_time'] else '进行中'}")
                print(f"  当前阶段: {STAGE_NAMES.get(pr['current_stage'], pr['current_stage']) if pr['current_stage'] else '-'}")
                print(f"  总体备注: {pr['overall_notes'] or '-'}")
                print(f"  创建时间: {format_timestamp(pr['created_at'])}")
                print(f"  更新时间: {format_timestamp(pr['updated_at'])}")
        else:
            print("  (无数据)")
        print()
        
        # 5. 查看 stage_records 表
        print("=" * 80)
        print("📋 stage_records 表 - 阶段记录")
        print("=" * 80)
        cursor.execute("""
            SELECT sr.*, pr.team_id 
            FROM stage_records sr
            LEFT JOIN process_records pr ON sr.process_record_id = pr.id
            ORDER BY sr.created_at DESC 
            LIMIT 20
        """)
        stage_records = cursor.fetchall()
        if stage_records:
            for sr in stage_records:
                print(f"\n记录ID: {sr['id']}")
                print(f"  过程记录ID: {sr['process_record_id']}")
                print(f"  团队ID: {sr['team_id'] or '-'}")
                print(f"  阶段名称: {STAGE_NAMES.get(sr['stage_name'], sr['stage_name'])} ({sr['stage_name']})")
                print(f"  开始时间: {format_timestamp(sr['start_time'])}")
                print(f"  结束时间: {format_timestamp(sr['end_time']) if sr['end_time'] else '进行中'}")
                print(f"  自评: {'★' * sr['self_rating'] if sr['self_rating'] > 0 else '未评价'}")
                print(f"  完成状态: {'已完成' if sr['is_completed'] else '未完成'}")
                print(f"  做得好的地方: {sr['notes'][:50] + '...' if sr['notes'] and len(sr['notes']) > 50 else (sr['notes'] or '-')}")
                print(f"  需要改进: {sr['problem_notes'][:50] + '...' if sr['problem_notes'] and len(sr['problem_notes']) > 50 else (sr['problem_notes'] or '-')}")
                # 解析标签
                if sr['selected_tags']:
                    try:
                        tags = json.loads(sr['selected_tags'])
                        print(f"  标签: {', '.join(tags) if tags else '-'}")
                    except:
                        print(f"  标签: {sr['selected_tags']}")
                print(f"  创建时间: {format_timestamp(sr['created_at'])}")
        else:
            print("  (无数据)")
        print()
        
        # 6. 查看 media_items 表
        print("=" * 80)
        print("📷 media_items 表 - 媒体文件")
        print("=" * 80)
        cursor.execute("SELECT COUNT(*) as total FROM media_items")
        total_media = cursor.fetchone()['total']
        print(f"  总媒体文件数: {total_media}")
        if total_media > 0:
            cursor.execute("""
                SELECT mi.*, sr.stage_name, pr.team_id
                FROM media_items mi
                LEFT JOIN stage_records sr ON mi.stage_record_id = sr.id
                LEFT JOIN process_records pr ON sr.process_record_id = pr.id
                ORDER BY mi.created_at DESC 
                LIMIT 10
            """)
            media_items = cursor.fetchall()
            for mi in media_items:
                print(f"\n文件ID: {mi['id']}")
                print(f"  文件路径: {mi['file_path']}")
                print(f"  文件类型: {mi['file_type']}")
                print(f"  文件大小: {mi['file_size'] or '-'} 字节")
                print(f"  阶段: {STAGE_NAMES.get(mi['stage_name'], mi['stage_name']) if mi['stage_name'] else '-'}")
                print(f"  团队ID: {mi['team_id'] or '-'}")
                print(f"  课后总结问题: {mi['summary_question'] or '-'}")
        print()
        
        # 7. 查看 summary_data 表
        print("=" * 80)
        print("📄 summary_data 表 - 课后总结")
        print("=" * 80)
        cursor.execute("SELECT * FROM summary_data ORDER BY updated_at DESC LIMIT 10")
        summaries = cursor.fetchall()
        if summaries:
            for s in summaries:
                print(f"\n团队ID: {s['team_id']}")
                print(f"  问题1: {s['answer1'][:50] + '...' if s['answer1'] and len(s['answer1']) > 50 else (s['answer1'] or '-')}")
                print(f"  问题2: {s['answer2'][:50] + '...' if s['answer2'] and len(s['answer2']) > 50 else (s['answer2'] or '-')}")
                print(f"  问题3: {s['answer3'][:50] + '...' if s['answer3'] and len(s['answer3']) > 50 else (s['answer3'] or '-')}")
                print(f"  更新时间: {format_timestamp(s['updated_at'])}")
        else:
            print("  (无数据)")
        print()
        
        # 8. 查看 teacher_evaluations 表
        print("=" * 80)
        print("⭐ teacher_evaluations 表 - 教师评价")
        print("=" * 80)
        cursor.execute("SELECT * FROM teacher_evaluations ORDER BY updated_at DESC LIMIT 10")
        evaluations = cursor.fetchall()
        if evaluations:
            for e in evaluations:
                print(f"\n团队ID: {e['team_id']}")
                print(f"  阶段: {STAGE_NAMES.get(e['stage_name'], e['stage_name']) if e['stage_name'] else '总体评价'}")
                print(f"  评分: {'★' * e['rating'] if e['rating'] > 0 else '未评分'}")
                print(f"  评价: {e['comment'][:50] + '...' if e['comment'] and len(e['comment']) > 50 else (e['comment'] or '-')}")
                print(f"  优点: {e['strengths'][:50] + '...' if e['strengths'] and len(e['strengths']) > 50 else (e['strengths'] or '-')}")
                print(f"  改进: {e['improvements'][:50] + '...' if e['improvements'] and len(e['improvements']) > 50 else (e['improvements'] or '-')}")
                print(f"  更新时间: {format_timestamp(e['updated_at'])}")
        else:
            print("  (无数据)")
        print()
        
        # 9. 统计信息
        print("=" * 80)
        print("📊 数据统计")
        print("=" * 80)
        cursor.execute("SELECT COUNT(*) FROM teams")
        teams_count = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM process_records")
        process_count = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM stage_records")
        stages_count = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM media_items")
        media_count = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM summary_data")
        summary_count = cursor.fetchone()[0]
        
        print(f"  团队数: {teams_count}")
        print(f"  过程记录数: {process_count}")
        print(f"  阶段记录数: {stages_count}")
        print(f"  媒体文件数: {media_count}")
        print(f"  课后总结数: {summary_count}")
        print()
        
        conn.close()
        
    except Exception as e:
        print(f"❌ 查看数据库失败: {str(e)}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    view_database()
    print()
    input("按回车键退出...")

