# 🌟 Hoshiori AI - 智能情感陪伴应用

<div align="center">

**一个基于先进心理学理论和情感计算的 AI 陪伴应用**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](#-english-version) | [中文文档](#-中文文档)

</div>

---

## 📖 中文文档

### 项目简介

Hoshiori AI 是一款创新的 AI 情感陪伴应用，采用先进的心理学理论和情感计算技术,为用户提供真实、自然的对话体验。应用集成了多个 AI 大脑系统,能够理解用户情绪、记忆对话内容、自主学习成长,并根据用户的个性特点提供个性化的陪伴服务。

### ✨ 核心特性

- 🧠 **先进的 AI 大脑系统** - 基于 PAD 情绪模型和认知心理学
- 💭 **智能记忆管理** - 三层记忆架构,永久记住重要内容
- 💎 **心理学增强** - 依恋理论、镜像神经元、峰终定律等
- 🌐 **联网搜索** - 支持天气查询和离线自主搜索
- 🎨 **精美 UI** - 毛玻璃效果、流畅动画、现代设计
- 🔄 **主动对话** - AI 会主动发起对话,不再被动等待
- 🖼️ **个性化定制** - 自定义聊天背景、角色头像、主题风格

### 🎯 主要功能

#### 1. AI 大脑系统
- **PAD 情绪模型** - Pleasure（愉悦度）、Arousal（激活度）、Dominance（支配度）
- **三层记忆架构** - 短期记忆、长期记忆、记忆图谱
- **向量记忆** - 语义相似度搜索,智能匹配
- **认知系统** - 双系统思考、认知一致性检查

#### 2. 心理学增强系统
- **依恋理论** - 自动检测用户依恋风格并调整回复策略
- **自我表露匹配** - 根据用户表露深度匹配回复深度
- **镜像神经元系统** - 自然模仿用户表达方式
- **峰终定律** - 创造对话中的"峰值体验"
- **社会支持系统** - 精准识别用户需求类型

#### 3. 对话功能
- **分段回复** - 模拟真实打字过程
- **主动对话** - AI 主动发起对话
- **情绪分析** - 实时分析用户情绪
- **记忆检索** - 智能搜索相关记忆

#### 4. UI/UX 设计
- **毛玻璃效果** - 现代化视觉设计
- **流畅动画** - 按钮按压、消息滑入等动画
- **自定义主题** - 5个精美预设主题 + 自定义上传
- **自定义头像** - 支持相册选择和拍照上传

---

### 📦 下载安装

不想编译？直接下载安装包体验：

- **[Hoshiori-v1.2.0-debug.apk](https://github.com/PeterWilsono/HoshioriAndroid/releases/download/v1.2.0/Hoshiori-v1.2.0-debug.apk)**（约 20.5 MB，Android 8.0 及以上）
- 更多版本请查看 [Releases 页面](https://github.com/PeterWilsono/HoshioriAndroid/releases)

安装时如系统提示，请允许「安装未知来源应用」；首次打开后在「设置」中填写你的 AI API Key 即可使用。

---

### 🚀 快速开始

#### 环境要求

- **Android Studio**: Arctic Fox (2020.3.1) 或更高版本
- **JDK**: 11 或更高版本
- **Android SDK**: API 26 (Android 8.0) 或更高
- **Gradle**: 7.0 或更高版本
- **Kotlin**: 1.8.0 或更高版本

#### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/PeterWilsono/HoshioriAndroid.git
   cd HoshioriAndroid
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目目录
   - 等待 Gradle 同步完成

3. **配置 API**
   - 在应用设置中配置 AI API（支持 OpenAI、Claude、Gemini 等）
   - ⚠️ **重要**: 请勿在代码中硬编码 API Key

4. **编译运行**
   ```bash
   # Windows
   .\gradlew.bat assembleDebug
   
   # macOS/Linux
   ./gradlew assembleDebug
   ```

#### 配置说明

##### 1. AI API 配置

在应用设置中填写：
- **API 地址**: 你的 AI API 端点
- **API Key**: 你的 API 密钥
- **模型**: 选择使用的模型（如 gpt-4, claude-3, gemini-pro）

支持的 AI 服务：
- **OpenAI**: GPT-3.5, GPT-4, GPT-4-Turbo
- **Anthropic**: Claude-3 (Opus, Sonnet, Haiku)
- **Google**: Gemini Pro, Gemini Ultra
- **其他**: 兼容 OpenAI API 格式的服务

##### 2. 和风天气 API（可选）

1. 注册账号：https://dev.qweather.com/
2. 创建应用获取 API Key
3. 在设置中填入 API Key（域名格式）

---

### 🏗️ 项目结构

```
HoshioriAndroid/
├── app/
│   ├── src/main/
│   │   ├── java/com/hoshiori/ai/
│   │   │   ├── api/              # API 调用
│   │   │   ├── brain/            # AI 大脑系统
│   │   │   │   ├── AutonomousSystem.kt      # 自主智能
│   │   │   │   ├── CognitiveSystem.kt       # 认知系统
│   │   │   │   ├── ConversationManager.kt   # 对话管理
│   │   │   │   ├── EmotionAnalyzer.kt       # 情绪分析
│   │   │   │   ├── MemoryManager.kt         # 记忆管理
│   │   │   │   └── psychology/              # 心理学系统
│   │   │   ├── data/             # 数据管理
│   │   │   ├── network/          # 网络功能
│   │   │   ├── service/          # 后台服务
│   │   │   ├── ui/               # UI 界面
│   │   │   └── optimization/     # Token优化
│   │   └── res/                  # 资源文件
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

### 📚 核心技术

#### 技术栈

- **Kotlin** - 主要开发语言
- **Android Jetpack** - Android 开发框架
  - ViewModel, LiveData, Room, WorkManager
- **OkHttp** - HTTP 客户端
- **Gson** - JSON 解析
- **Glide** - 图片加载
- **Coroutines** - 异步编程

#### 理论基础

- PAD 情绪模型 (Mehrabian & Russell, 1974)
- 依恋理论 (Bowlby, 1969)
- 峰终定律 (Kahneman, 1999)
- 镜像神经元理论 (Rizzolatti & Craighero, 2004)
- 自我表露理论 (Jourard, 1971)

---

### 🔒 安全说明

**⚠️ 重要提示**

1. **API Key 安全**
   - ❌ 不要在代码中硬编码 API Key
   - ✅ 使用应用设置页面输入 API Key
   - ✅ 所有密钥存储在应用私有目录

2. **数据安全**
   - 所有用户数据存储在应用私有目录
   - 其他应用无法访问
   - 卸载应用会自动清除所有数据

3. **代码安全**
   - 已移除所有硬编码的 API 密钥
   - 建议使用环境变量或配置文件管理密钥

---

### 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

#### 贡献流程

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

#### 代码规范

- 使用 Kotlin 官方代码风格
- 添加必要的注释和文档
- 确保代码通过所有测试
- 遵循现有的架构设计

---

### 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

### 🙏 致谢

感谢以下开源项目：

- [Kotlin](https://kotlinlang.org/)
- [Android Jetpack](https://developer.android.com/jetpack)
- [OkHttp](https://square.github.io/okhttp/)
- [Gson](https://github.com/google/gson)
- [Glide](https://github.com/bumptech/glide)

---

### 📞 联系方式

- **项目主页**: https://github.com/PeterWilsono/HoshioriAndroid
- **问题反馈**: https://github.com/PeterWilsono/HoshioriAndroid/issues

---

## 🌍 English Version

### Overview

Hoshiori AI is an innovative AI emotional companion application that employs advanced psychological theories and affective computing technologies to provide users with authentic and natural conversation experiences.

### Key Features

- 🧠 **Advanced AI Brain System** - Based on PAD emotional model and cognitive psychology
- 💭 **Intelligent Memory Management** - Three-layer memory architecture
- 💎 **Psychology Enhancement** - Attachment theory, mirror neurons, peak-end rule
- 🌐 **Web Search** - Weather queries and autonomous offline search
- 🎨 **Beautiful UI** - Glassmorphism effects, smooth animations
- 🔄 **Proactive Conversation** - AI initiates conversations proactively
- 🖼️ **Personalization** - Custom chat backgrounds, avatars, themes

### Quick Start

1. Clone the repository
   ```bash
   git clone https://github.com/PeterWilsono/HoshioriAndroid.git
   ```

2. Open in Android Studio and wait for Gradle sync

3. Configure your AI API in app settings
   - ⚠️ **Important**: Do not hardcode API keys in code

4. Build and run
   ```bash
   ./gradlew assembleDebug
   ```

### Requirements

- Android Studio Arctic Fox or later
- JDK 11+
- Android SDK API 26+
- Kotlin 1.8.0+

### Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**⭐ If this project helps you, please give it a star! ⭐**

Made with ❤️ by Hoshiori Team

</div>
