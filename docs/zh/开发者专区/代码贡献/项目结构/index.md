---
title: 项目结构
---

# 项目结构

Unciv 项目的核心架构和主要类。

## 项目概述

Unciv 是一个使用 LibGDX 框架开发的开源策略游戏，支持 Android、Desktop 和 Server 平台。

## 目录结构

```
Unciv/
├── core/              # 核心游戏逻辑
├── desktop/           # 桌面端代码
├── android/           # Android 端代码
├── server/            # 服务器端代码
├── tests/             # 测试代码
└── buildSrc/          # 构建脚本
```

## 主要模块

### core 模块

包含所有游戏逻辑、模型、规则集等，是纯 Kotlin 模块，不依赖特定平台。

### desktop 模块

桌面端启动器和系统特定功能。

### android 模块

Android 端配置、权限、启动器。

### server 模块

基于 Ktor 的多人游戏服务器。

## 核心类

详见 [Unciv 项目文档](https://yairm210.github.io/Unciv/Developers/Project-structure-and-major-classes/)。