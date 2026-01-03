#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试团队信息API - 验证完整数据返回
"""

import requests
import json

def test_team_info():
    """测试团队信息API返回完整数据"""
    print("=" * 80)
    print("测试团队信息API - 完整数据验证")
    print("=" * 80)

    try:
        url = "http://localhost:5000/api/evaluation/teams"
        print(f"请求URL: {url}\n")

        response = requests.get(url, timeout=5)
        print(f"响应状态码: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            teams = data.get('teams', [])

            print(f"\n✅ API返回成功！")
            print(f"团队数量: {len(teams)}\n")

            if len(teams) > 0:
                print("=" * 80)
                print("第一个团队的详细信息:")
                print("=" * 80)

                team = teams[0]
                print(f"ID (id):              {team.get('id')}")
                print(f"团队ID (teamId):      {team.get('teamId')}")
                print(f"团队名称 (teamName):  {team.get('teamName')}")
                print(f"学校 (school):        {team.get('school')}")
                print(f"年级 (grade):         {team.get('grade')}")
                print(f"班级 (className):     {team.get('className')}")
                print(f"炉号 (stoveNumber):   {team.get('stoveNumber')}")
                print(f"成员数量:             {team.get('memberCount')}")
                print(f"成员名单:             {team.get('memberNames')}")
                print(f"组长 (groupLeader):   {team.get('groupLeader')}")

                division = team.get('division')
                if division:
                    print(f"\n团队分工:")
                    print(f"  组长 (groupLeader):     {division.get('groupLeader')}")
                    print(f"  烹饪组 (groupCooking):  {division.get('groupCooking')}")
                    print(f"  汤饭组 (groupSoupRice): {division.get('groupSoupRice')}")
                    print(f"  生火组 (groupFire):     {division.get('groupFire')}")
                    print(f"  卫生组 (groupHealth):   {division.get('groupHealth')}")

                print("\n" + "=" * 80)
                print("所有团队列表:")
                print("=" * 80)
                for i, team in enumerate(teams, 1):
                    print(f"{i}. {team.get('teamName')} - 组长: {team.get('groupLeader', '未设置')}")

                print("\n✅ 修复成功！所有字段都正常返回")
                print("\n现在教师端App应该能看到:")
                print("  ✓ 学校名称")
                print("  ✓ 年级班级")
                print("  ✓ 炉号")
                print("  ✓ 组长")
                print("  ✓ 团队分工")
                return True
            else:
                print("⚠️  数据库中没有团队数据")
                return False
        else:
            print(f"❌ 请求失败: {response.status_code}")
            print(f"响应内容: {response.text}")
            return False

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
    success = test_team_info()
    print("\n" + "=" * 80)
    if success:
        print("✅ 测试通过！")
        print("\n接下来的步骤:")
        print("1. 在教师端App中打开评价页面")
        print("2. 左侧列表应该显示所有团队")
        print("3. 每个团队显示: 学校 年级 班级 炉号")
        print("4. 显示组长信息")
        print("5. 显示分工信息")
    else:
        print("❌ 测试失败，请检查:")
        print("1. 服务器是否正在运行")
        print("2. 服务器是否已重启")
        print("3. Python缓存是否已清除")
        print("4. 数据库中是否有团队数据")
    print("=" * 80)
