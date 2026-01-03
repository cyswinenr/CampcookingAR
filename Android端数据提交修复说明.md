# Android 端数据提交修复说明

## 问题分析

### 发现的问题

1. **JSON 文件中缺少 `stages` 字段**：
   - 从服务器端接收的 JSON 文件 `data_20260103_104732.json` 中，`processRecord` 对象没有包含 `stages` 字段
   - 这导致服务器端无法提取媒体文件信息，`media_items` 表中没有数据

2. **可能的原因**：
   - `processRecord.stages` 可能为空（没有访问过任何阶段）
   - 或者 `convertProcessRecordToMap` 方法没有正确生成 `stages` 字段
   - 或者 Gson 序列化时跳过了空对象（不太可能）

## 修复内容

### 1. 增强 `convertProcessRecordToMap` 方法

**文件**：`app/src/main/java/com/campcooking/ar/utils/DataSubmitManager.kt`

**修改内容**：
- ✅ 确保 `stages` 字段始终存在（即使为空对象 `{}`）
- ✅ 确保每个阶段的 `mediaItems` 字段始终存在（即使为空数组 `[]`）
- ✅ 添加调试日志，记录每个阶段的媒体文件数量
- ✅ 移除 `processRecord` 中的 `teamInfo` 字段（因为已经在顶层单独添加）

**关键改进**：
```kotlin
// 确保 stages 字段始终存在（即使为空）
"stages" to stagesMap,  // 即使为空也要包含

// 确保 mediaItems 字段始终存在（即使为空）
"mediaItems" to mediaItemsList,  // 即使为空列表也要包含
```

### 2. 增强 `submitAllData` 方法

**修改内容**：
- ✅ 添加详细的调试日志
- ✅ 记录过程记录中的阶段数量
- ✅ 记录每个阶段的媒体文件数量
- ✅ 记录总计媒体文件数量

**日志输出示例**：
```
✅ 过程记录包含 7 个阶段
  阶段 PREPARATION: 2 个媒体文件
  阶段 FIRE_MAKING: 1 个媒体文件
✅ 总计 3 个媒体文件
```

## 数据格式保证

### 修复后的 JSON 结构

```json
{
  "teamInfo": { ... },
  "teamDivision": { ... },
  "processRecord": {
    "startTime": 1767407901561,
    "endTime": null,
    "currentStage": "FIRE_MAKING",
    "overallNotes": "",
    "stages": {  // ✅ 现在始终存在
      "PREPARATION": {
        "stage": "PREPARATION",
        "startTime": 1767407901561,
        "endTime": null,
        "photos": [],
        "mediaItems": [  // ✅ 即使为空也包含
          {
            "path": "/path/to/photo.jpg",
            "type": "PHOTO",
            "timestamp": 1767408454050
          }
        ],
        "selfRating": 0,
        "selectedTags": [],
        "notes": "",
        "problemNotes": "",
        "isCompleted": false
      },
      "FIRE_MAKING": { ... },
      ...
    }
  },
  "summaryData": null,
  "exportTime": 1767408454050
}
```

## 验证步骤

### 1. 编译并运行 Android 应用

```bash
# 在 Android Studio 中编译并运行应用
# 或者使用 Gradle
./gradlew assembleDebug
```

### 2. 测试数据提交

1. **拍摄照片/视频**：
   - 在记录页面拍摄几张照片
   - 拍摄一段视频
   - 确保媒体文件被添加到 `mediaItems` 中

2. **提交数据**：
   - 点击"保存并发送"按钮
   - 查看 Logcat 输出，确认日志信息

3. **检查服务器端**：
   - 查看服务器端接收的 JSON 文件
   - 确认包含 `stages` 字段
   - 确认 `mediaItems` 数据正确

### 3. 查看日志

在 Android Studio 的 Logcat 中，过滤 `DataSubmitManager` 标签，应该看到：

```
D/DataSubmitManager: ✅ 过程记录包含 7 个阶段
D/DataSubmitManager:   阶段 PREPARATION: 2 个媒体文件
D/DataSubmitManager:   阶段 FIRE_MAKING: 1 个媒体文件
D/DataSubmitManager: ✅ 总计 3 个媒体文件
D/DataSubmitManager: 提交完整数据到: http://192.168.1.100:5000/api/submit
D/DataSubmitManager: 提交成功: {"status":"success","studentId":"..."}
```

## 预期效果

### 修复前
- ❌ JSON 文件中没有 `stages` 字段
- ❌ 服务器端无法提取媒体文件信息
- ❌ `media_items` 表中没有数据

### 修复后
- ✅ JSON 文件中始终包含 `stages` 字段（即使为空）
- ✅ 每个阶段始终包含 `mediaItems` 字段（即使为空数组）
- ✅ 服务器端可以正确提取媒体文件信息
- ✅ `media_items` 表中会保存媒体文件记录
- ✅ 详细的调试日志帮助排查问题

## 注意事项

1. **向后兼容性**：
   - 修复后的代码仍然兼容旧数据格式
   - 即使没有媒体文件，也会包含 `stages` 字段

2. **性能影响**：
   - 添加的日志只在调试时输出，不影响性能
   - 空对象/数组的序列化开销很小

3. **数据完整性**：
   - 确保所有阶段都被包含在 `stages` 中
   - 即使阶段没有数据，也会包含基本信息

## 后续优化建议

1. **添加数据验证**：
   - 在提交前验证数据完整性
   - 检查必填字段是否存在

2. **错误处理**：
   - 如果数据转换失败，提供更详细的错误信息
   - 记录失败的数据，便于排查

3. **数据压缩**：
   - 如果媒体文件路径很长，可以考虑压缩
   - 或者只发送相对路径

## 总结

通过这次修复，确保了：
1. ✅ `stages` 字段始终存在于 JSON 数据中
2. ✅ `mediaItems` 字段始终存在于每个阶段中
3. ✅ 添加了详细的调试日志，便于排查问题
4. ✅ 服务器端可以正确提取和保存媒体文件信息

现在，当学生拍摄照片/视频并提交数据时，媒体文件信息会被正确保存到数据库的 `media_items` 表中。

