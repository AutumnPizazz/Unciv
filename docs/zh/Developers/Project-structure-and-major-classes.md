---
title: 项目结构
---

# 项目结构

Unciv 项目的核心架构和主要类。

## 项目概述

Unciv 是一个使用 LibGDX 框架开发的开源策略游戏，支持 Android 和桌面平台。Go 联机服务端由独立的闭源项目维护。

## 目录结构

```
Unciv/
├── core/              # 核心游戏逻辑
├── desktop/           # 桌面端代码
├── android/           # Android 端代码
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

## 核心类

序列化状态以 `GameInfo` 为根，包含玩家 `Civilization`、城市 `City` 和地图 `TileMap`（地块实体为 `Tile`）；`Ruleset` 从资源文件加载，不属于存档状态。主要界面是 `WorldScreen` 和 `CityScreen`。

详见[项目结构与主要类](../../Developers/Project-structure-and-major-classes.md)。
