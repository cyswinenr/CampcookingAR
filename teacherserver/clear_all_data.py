#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
直接清空所有数据的脚本
包括数据库和文件系统数据
"""

import os
import shutil
import sqlite3
import logging
from config import Config

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def clear_database():
    """清空数据库"""
    db_path = Config.DATABASE_PATH
    
    if not os.path.exists(db_path):
        logger.info("数据库文件不存在，跳过数据库清空")
        return 0
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 禁用外键约束
        cursor.execute("PRAGMA foreign_keys = OFF")
        
        # 获取所有表名
        cursor.execute("""
            SELECT name FROM sqlite_master 
            WHERE type='table' AND name NOT LIKE 'sqlite_%'
        """)
        tables = [row[0] for row in cursor.fetchall()]
        
        if not tables:
            logger.info("数据库中没有表，跳过清空")
            conn.close()
            return 0
        
        # 清空所有表
        cleared_count = 0
        for table in tables:
            cursor.execute(f"DELETE FROM {table}")
            count = cursor.rowcount
            cleared_count += count
            logger.info(f"已清空表 {table}: {count} 条记录")
        
        # 重新启用外键约束
        cursor.execute("PRAGMA foreign_keys = ON")
        
        # 提交事务
        conn.commit()
        conn.close()
        
        logger.info(f"✅ 数据库清空完成，共清空 {len(tables)} 个表，{cleared_count} 条记录")
        return cleared_count
        
    except Exception as e:
        logger.error(f"❌ 清空数据库失败: {str(e)}", exc_info=True)
        return -1


def clear_directory(directory_path, description):
    """清空目录"""
    if not os.path.exists(directory_path):
        logger.info(f"{description}目录不存在，跳过清空")
        return 0
    
    try:
        deleted_count = 0
        
        # 遍历目录中的所有项目
        for item in os.listdir(directory_path):
            item_path = os.path.join(directory_path, item)
            
            try:
                if os.path.isdir(item_path):
                    shutil.rmtree(item_path)
                    deleted_count += 1
                    logger.info(f"已删除目录: {item}")
                elif os.path.isfile(item_path):
                    os.remove(item_path)
                    deleted_count += 1
                    logger.info(f"已删除文件: {item}")
            except Exception as e:
                logger.error(f"删除 {item} 失败: {str(e)}")
        
        logger.info(f"✅ {description}清空完成，共删除 {deleted_count} 项")
        return deleted_count
        
    except Exception as e:
        logger.error(f"❌ 清空{description}失败: {str(e)}", exc_info=True)
        return -1


def clear_all_data():
    """清空所有数据"""
    print("=" * 60)
    print("开始清空所有数据...")
    print("=" * 60)
    print()
    
    total_cleared = 0
    
    # 1. 清空数据库
    print("1. 清空数据库...")
    db_count = clear_database()
    if db_count >= 0:
        total_cleared += db_count
    print()
    
    # 2. 清空学生数据目录
    print("2. 清空学生数据目录...")
    students_count = clear_directory(Config.DATA_DIR, "学生数据")
    if students_count >= 0:
        total_cleared += students_count
    print()
    
    # 3. 清空评价数据目录
    print("3. 清空评价数据目录...")
    eval_count = clear_directory(Config.EVALUATION_DIR, "评价数据")
    if eval_count >= 0:
        total_cleared += eval_count
    print()
    
    # 4. 清空导出数据目录（可选）
    print("4. 清空导出数据目录...")
    export_count = clear_directory(Config.EXPORT_DIR, "导出数据")
    if export_count >= 0:
        total_cleared += export_count
    print()
    
    # 5. 验证清空结果
    print("=" * 60)
    print("验证清空结果...")
    print("=" * 60)
    
    # 验证数据库
    db_path = Config.DATABASE_PATH
    if os.path.exists(db_path):
        try:
            conn = sqlite3.connect(db_path)
            cursor = conn.cursor()
            cursor.execute("""
                SELECT name FROM sqlite_master 
                WHERE type='table' AND name NOT LIKE 'sqlite_%'
            """)
            tables = [row[0] for row in cursor.fetchall()]
            
            all_empty = True
            for table in tables:
                cursor.execute(f"SELECT COUNT(*) FROM {table}")
                count = cursor.fetchone()[0]
                if count > 0:
                    logger.warning(f"⚠️  表 {table} 仍有 {count} 条记录")
                    all_empty = False
                else:
                    logger.info(f"✓ 表 {table} 已清空")
            
            conn.close()
            
            if all_empty:
                logger.info("✅ 数据库验证通过：所有表已清空")
            else:
                logger.warning("⚠️  数据库验证失败：部分表仍有数据")
        except Exception as e:
            logger.error(f"验证数据库失败: {str(e)}")
    else:
        logger.info("数据库文件不存在，跳过验证")
    
    # 验证文件系统
    print()
    print("验证文件系统...")
    
    # 验证学生数据目录
    if os.path.exists(Config.DATA_DIR):
        remaining = len([d for d in os.listdir(Config.DATA_DIR) 
                         if os.path.isdir(os.path.join(Config.DATA_DIR, d))])
        if remaining == 0:
            logger.info(f"✅ 学生数据目录已清空")
        else:
            logger.warning(f"⚠️  学生数据目录仍有 {remaining} 个子目录")
    else:
        logger.info("学生数据目录不存在")
    
    # 验证评价数据目录
    if os.path.exists(Config.EVALUATION_DIR):
        remaining = len([f for f in os.listdir(Config.EVALUATION_DIR) 
                         if os.path.isfile(os.path.join(Config.EVALUATION_DIR, f))])
        if remaining == 0:
            logger.info(f"✅ 评价数据目录已清空")
        else:
            logger.warning(f"⚠️  评价数据目录仍有 {remaining} 个文件")
    else:
        logger.info("评价数据目录不存在")
    
    print()
    print("=" * 60)
    print(f"清空操作完成！共处理 {total_cleared} 项数据")
    print("=" * 60)


if __name__ == '__main__':
    try:
        # 确认操作
        print()
        print("⚠️  警告：此操作将清空所有数据！")
        print("包括：")
        print("  - 数据库中的所有表数据")
        print("  - 学生数据目录中的所有文件")
        print("  - 评价数据目录中的所有文件")
        print("  - 导出数据目录中的所有文件")
        print()
        
        confirm = input("确定要继续吗？(输入 'YES' 确认): ")
        
        if confirm != 'YES':
            print("操作已取消")
            exit(0)
        
        print()
        clear_all_data()
        
    except KeyboardInterrupt:
        print("\n\n操作被用户中断")
        exit(1)
    except Exception as e:
        logger.error(f"执行失败: {str(e)}", exc_info=True)
        exit(1)

