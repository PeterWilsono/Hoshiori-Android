# 资源文件管理指南

## 📋 概述

本项目的资源文件组织良好，遵循Android标准命名规范。虽然文件数量较多，但这是正常的，因为：

1. **功能丰富**：项目包含单聊、群聊、角色管理、主题切换等多个功能
2. **UI精致**：每个功能都有精心设计的UI，需要多个资源文件
3. **命名规范**：所有文件都遵循清晰的命名规范，易于查找

## 📊 资源统计

- **drawable**: ~35个文件（背景、图标）
- **layout**: ~19个文件（Activity、Dialog、Item、Fragment）
- **其他**: menu、values、xml等

## 🎯 命名规范

### drawable 命名
```
bg_*        - 背景资源
ic_*        - 图标资源
*_enhanced  - 增强版本
```

### layout 命名
```
activity_*  - Activity布局
dialog_*    - Dialog布局
item_*      - RecyclerView Item布局
fragment_*  - Fragment布局
```

## 📁 文件组织

### Android限制
Android **不支持**在res/drawable和res/layout下创建子文件夹。所有文件必须放在根目录。

### 我们的解决方案
1. **统一命名前缀**：通过前缀快速识别文件类型
2. **资源索引文档**：查看`资源文件索引.md`了解每个文件的用途
3. **IDE支持**：Android Studio提供强大的资源搜索和预览功能

## 🔍 快速查找资源

### 方法1：使用Android Studio
1. 按`Ctrl+Shift+N`（Windows）或`Cmd+Shift+O`（Mac）
2. 输入资源名称（支持模糊搜索）
3. 快速跳转到文件

### 方法2：使用前缀过滤
在文件浏览器中：
- 输入`bg_`查看所有背景
- 输入`ic_`查看所有图标
- 输入`activity_`查看所有Activity布局

### 方法3：查看索引文档
打开`资源文件索引.md`，按功能查找资源。

## 🛠️ 维护建议

### 添加新资源
```
1. 遵循命名规范
2. 在资源索引.md中添加说明
3. 确保资源被正确引用
```

### 修改资源
```
1. 使用"Find Usages"查找所有引用
2. 测试所有受影响的界面
3. 更新索引文档
```

### 删除资源
```
1. 确认资源未被使用
2. 使用Android Studio的"Remove Unused Resources"
3. 从索引文档中移除
```

## 🧹 定期清理

### 每个版本发布前
1. 运行`Analyze > Run Inspection by Name > Unused resources`
2. 检查并删除未使用的资源
3. 更新资源索引文档

### 使用Lint检查
```bash
./gradlew lint
```
查看报告中的"UnusedResources"部分。

## 💡 最佳实践

### ✅ 推荐做法
- 使用矢量图（XML）而不是位图（PNG）
- 为不同屏幕密度提供适当的资源
- 使用主题和样式复用资源
- 保持命名一致性

### ❌ 避免做法
- 不要创建子文件夹（Android不支持）
- 不要使用中文命名
- 不要保留未使用的资源
- 不要重复定义相同的资源

## 📚 相关文档

- [资源文件索引.md](./资源文件索引.md) - 详细的资源文件列表和说明
- [Android官方文档 - 应用资源](https://developer.android.com/guide/topics/resources/providing-resources)

## 🎨 资源设计规范

### 颜色
所有颜色定义在`values/colors.xml`中，使用语义化命名：
```xml
<color name="primary">#6366F1</color>
<color name="text_primary">#1F2937</color>
```

### 尺寸
所有尺寸定义在布局文件中，使用dp单位：
```xml
android:padding="12dp"
android:textSize="15sp"
```

### 图标
- 使用24dp × 24dp的标准尺寸
- 使用矢量图（XML）格式
- 保持简洁的设计风格

## 🔄 版本控制

### Git提交规范
```
feat(res): 添加新的主题背景资源
fix(res): 修复按钮背景显示问题
refactor(res): 重构drawable资源命名
```

### 资源变更记录
重要的资源变更应该在CHANGELOG.md中记录。

---

## ❓ 常见问题

**Q: 为什么不创建子文件夹？**
A: Android资源系统不支持子文件夹。所有资源必须在根目录。

**Q: 文件太多怎么办？**
A: 这是正常的。使用命名规范和IDE搜索功能可以轻松管理。

**Q: 如何知道哪些资源未使用？**
A: 使用Android Studio的Lint检查或"Remove Unused Resources"功能。

**Q: enhanced版本和普通版本有什么区别？**
A: enhanced版本通常有更好的视觉效果，如更丰富的渐变、阴影等。

---

最后更新：2026-01-11
维护者：Hoshiori Team
