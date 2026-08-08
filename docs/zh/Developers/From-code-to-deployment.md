---
title: 构建和部署
---

# 构建和部署

Unciv 项目的构建流程和部署指南。

## 本地构建

### 前置要求

- JDK 11 或更高版本
- Android SDK（用于 Android 构建）

### Desktop 构建

```bash
./gradlew desktop:dist
```

### Android 构建

```bash
./gradlew android:assembleRelease
```

### Server 构建

```bash
./gradlew server:dist
```

## 测试

```bash
./gradlew :tests:test
```

## 详见

[Unciv 构建和部署文档](https://yairm210.github.io/Unciv/Developers/From-code-to-deployment/)