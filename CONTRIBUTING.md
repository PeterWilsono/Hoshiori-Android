# 贡献指南

感谢你考虑为 Hoshiori AI 做贡献！

## 🤝 如何贡献

### 报告问题

如果你发现了 bug 或有功能建议：

1. 检查 [Issues](https://github.com/yourusername/hoshiori-ai/issues) 中是否已有相同问题
2. 如果没有，创建新的 Issue，包含：
   - 清晰的标题
   - 详细的描述
   - 复现步骤（如果是 bug）
   - 期望行为和实际行为
   - 截图（如果适用）
   - 设备信息（Android 版本、设备型号等）

### 提交代码

1. **Fork 项目**
   ```bash
   git clone https://github.com/yourusername/hoshiori-ai.git
   ```

2. **创建分支**
   ```bash
   git checkout -b feature/AmazingFeature
   # 或
   git checkout -b fix/BugDescription
   ```

3. **进行修改**
   - 遵循代码规范
   - 添加必要的注释
   - 更新相关文档

4. **提交更改**
   ```bash
   git add .
   git commit -m "Add: 简洁的描述"
   # 或
   git commit -m "Fix: 简洁的描述"
   ```

5. **推送到 GitHub**
   ```bash
   git push origin feature/AmazingFeature
   ```

6. **创建 Pull Request**
   - 清晰描述你的更改
   - 关联相关的 Issue
   - 等待代码审查

## 📝 代码规范

### Kotlin 代码风格

- 使用 Kotlin 官方代码风格
- 使用 4 个空格缩进
- 最大行长度：120 字符
- 使用有意义的变量名和函数名

### 注释规范

```kotlin
/**
 * 函数说明
 * 
 * @param paramName 参数说明
 * @return 返回值说明
 */
fun functionName(paramName: Type): ReturnType {
    // 实现
}
```

### 提交信息规范

使用以下前缀：

- `Add:` 新功能
- `Fix:` 修复 bug
- `Update:` 更新现有功能
- `Refactor:` 重构代码
- `Docs:` 文档更新
- `Style:` 代码格式调整
- `Test:` 测试相关
- `Chore:` 构建/工具相关

## 🔒 安全注意事项

**⚠️ 重要：不要提交敏感信息**

- ❌ 不要在代码中硬编码 API Key
- ❌ 不要提交数据库文件
- ❌ 不要提交个人对话记录
- ❌ 不要提交私钥、密码等敏感信息

## 📚 开发指南

### 环境设置

1. 安装 Android Studio Arctic Fox 或更高版本
2. 安装 JDK 11 或更高版本
3. Clone 项目并等待 Gradle 同步

### 项目结构

```
app/src/main/java/com/hoshiori/ai/
├── api/          # API 调用相关
├── brain/        # AI 大脑系统
├── data/         # 数据管理
├── network/      # 网络功能
├── service/      # 后台服务
├── ui/           # UI 界面
└── optimization/ # Token 优化
```

### 测试

在提交 PR 之前，请确保：

- 代码能够编译通过
- 功能测试正常
- 没有引入新的警告
- 遵循现有的架构设计

## 📋 行为准则

- 尊重所有贡献者
- 接受建设性批评
- 关注项目利益
- 保持友好的讨论氛围

## ❓ 需要帮助？

如果你有任何问题，可以：

- 查看 [README.md](README.md)
- 查看 [功能说明.md](功能说明.md)
- 在 Issues 中提问

感谢你的贡献！❤️
