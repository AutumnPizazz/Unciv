---
title: UncivCN 分支介绍
---

# UncivCN 分支介绍

## 这是什么

**UncivCN** 是开源游戏 [Unciv](https://github.com/yairm210/Unciv)（LibGDX 编写的《文明5》复刻，支持 Android / Desktop / Server）的社区分支，
针对中文玩家深度定制：在原版基础上新增联机、地图、模组、UI 等多项特性，并完整跟进上游更新。

- 上游仓库：https://github.com/yairm210/Unciv
- UncivCN 仓库：https://github.com/AutumnPizazz/Unciv
- 当前版本：**4.21.6.2**（基于上游 4.21.6）

## 与上游的关系

- **同步跟进**：定期将上游 master 合并进 UncivCN，游戏版本号 = 上游版本 + CN 子版本号（如 4.21.5 → 4.21.5.1；同一上游版本可发 .1/.2/.3…，如 4.20.8.1 → 4.20.8.4），始终基于最新上游开发
- **CN 独有改动**：所有 UncivCN 特性均以增量方式实现，上游功能不受影响
- **分道扬镳**：UncivCN 自 2026 年从上游独立演进（[4c23bb4dc](https://github.com/AutumnPizazz/Unciv/commit/4c23bb4dc)），拥有独立版本号与发布节奏

## 主要特性速览

| 类别 | 特性 |
|---|---|
| 联机 | [轮询联机](./Polling-multiplayer)、默认服务器（sp.unciv.cn）、联机禁用读档随机数 |
| 地图 | 三种镜像地图模式、环形地图、取消半径奇偶限制 |
| 模组 | [Lua 脚本](./Features#lua-模组系统-4-20-8-2-起-持续增强)、[MergeAction 扩展 JSON 系统](./Features#mergeaction-扩展-json-系统-4-20-7-3)、tile-claim 地块归属 |
| UI | 单位钉/地图钉、人口自动锁定按钮、设置导出/导入剪贴板 |
| 其他 | 游戏版本兼容性隔离、随机数读档可变（单机） |

详细说明见 [新特性](./Features) 与 [与上游差异](./Differences)。

## 构建方式

```bash
./gradlew desktop:run        # 运行桌面版
./gradlew :tests:test        # 运行全部测试
./gradlew desktop:dist       # 构建 JAR → desktop/build/libs/Unciv.jar
./gradlew server:run         # 运行多人服务器
```

Android 构建需 `local.properties`（`sdk.dir`）或 `ANDROID_HOME` 环境变量。

版本号定义在 `buildSrc/src/main/kotlin/BuildConfig.kt`，变更记录见 [更新日志](./Changelog)。
