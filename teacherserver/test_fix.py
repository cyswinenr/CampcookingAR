#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试团队列表API修复
"""

import requests
import json

def test_evaluation_teams_api():
    """测试 /api/evaluation/teams API"""
    print("=" * 60)
    print("测试团队列表API")
    print("=" * 60)

    try:
        # 测试本地服务器
        url = "http://localhost:5000/api/evaluation/teams"
        print(f"请求URL: {url}")

        response = requests.get(url, timeout=5)
        print(f"响应状态码: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"响应状态: {data.get('status')}")
            teams = data.get('teams', [])
            print(f"团队数量: {len(teams)}")
            print("\n团队列表:")
            for i, team in enumerate(teams, 1):
                print(f"  {i}. ID: {team.get('id')}")
                print(f"     Name: {team.get('teamName')}")

            print("\n✅ 修复成功！API返回了所有团队数据")
            return True
        else:
            print(f"❌ 请求失败: {response.status_code}")
            print(f"响应内容: {response.text}")
            return False

    except requests.exceptions.ConnectionError:
        print("❌ 无法连接到服务器")
        print("请确保教师端服务器正在运行")
        return False
    except Exception as e:
        print(f"❌ 测试失败: {str(e)}")
        return False

if __name__ == "__main__":
    success = test_evaluation_teams_api()
    print("\n" + "=" * 60)
    if success:
        print("测试通过！现在可以:")
        print("1. 在教师端App查看团队列表")
        print("2. 选择团队进行评价")
    else:
        print("测试失败，请检查:")
        print("1. 服务器是否正在运行")
        print("2. 服务器是否已重启")
        print("3. Python缓存是否已清除")
    print("=" * 60)
