#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试团队列表分页功能
"""

import requests
import json

def test_pagination():
    """测试分页API"""
    print("=" * 80)
    print("测试团队列表分页功能")
    print("=" * 80)

    try:
        base_url = "http://localhost:5000/api/evaluation/teams"

        # 测试第1页
        print("\n【测试第1页】")
        url1 = f"{base_url}?page=1&page_size=5"
        print(f"URL: {url1}")
        response1 = requests.get(url1, timeout=5)

        if response1.status_code == 200:
            data1 = response1.json()
            teams1 = data1.get('teams', [])
            pagination1 = data1.get('pagination', {})

            print(f"✅ 成功！")
            print(f"   团队数量: {len(teams1)}")
            print(f"   分页信息: 第{pagination1.get('currentPage')}页 / 共{pagination1.get('totalPages')}页")
            print(f"   总团队数: {pagination1.get('totalCount')}")
            print(f"\n   第1页团队列表:")
            for i, team in enumerate(teams1, 1):
                print(f"     {i}. {team.get('teamName')}")

        else:
            print(f"❌ 失败: {response1.status_code}")

        # 测试第2页（如果有）
        if pagination1.get('totalPages', 0) > 1:
            print("\n【测试第2页】")
            url2 = f"{base_url}?page=2&page_size=5"
            print(f"URL: {url2}")
            response2 = requests.get(url2, timeout=5)

            if response2.status_code == 200:
                data2 = response2.json()
                teams2 = data2.get('teams', [])
                pagination2 = data2.get('pagination', {})

                print(f"✅ 成功！")
                print(f"   团队数量: {len(teams2)}")
                print(f"   分页信息: 第{pagination2.get('currentPage')}页 / 共{pagination2.get('totalPages')}页")
                print(f"\n   第2页团队列表:")
                for i, team in enumerate(teams2, 1):
                    print(f"     {i}. {team.get('teamName')}")

                # 验证排序
                print("\n【验证炉号排序】")
                all_teams = teams1 + teams2
                stove_numbers = []
                for team in all_teams:
                    import re
                    stove = team.get('stoveNumber', '')
                    match = re.search(r'(\d+)', stove)
                    if match:
                        stove_numbers.append(int(match.group(1)))

                print(f"   提取的炉号: {stove_numbers}")
                if stove_numbers == sorted(stove_numbers):
                    print(f"   ✅ 炉号已按从小到大排序")
                else:
                    print(f"   ❌ 炉号排序错误")

            else:
                print(f"❌ 失败: {response2.status_code}")

        print("\n" + "=" * 80)
        print("✅ 分页功能测试完成！")
        print("\n预期效果:")
        print("  ✓ 团队按炉号从小到大排序（1号炉、2号炉、...）")
        print("  ✓ 每页显示5个团队")
        print("  ✓ 可以通过上一页/下一页按钮切换页面")
        print("  ✓ 保存后不会自动返回第一页")
        print("=" * 80)

        return True

    except requests.exceptions.ConnectionError:
        print("❌ 无法连接到服务器")
        print("请确保教师端服务器正在运行在 http://localhost:5000")
        return False
    except Exception as e:
        print(f"❌ 测试失败: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    test_pagination()
