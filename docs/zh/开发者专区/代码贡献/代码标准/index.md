---
title: 代码标准
---

# 代码标准

Unciv 项目的编码规范和最佳实践。

## Kotlin 代码规范

### 命名约定

- **类名**：PascalCase（例如：`Civilization`、`City`）
- **函数名**：camelCase（例如：`getWorkerAutomation`、`hasFlag`）
- **变量名**：camelCase（例如：`gameInfo`、`currentTurn`）
- **常量**：UPPER_SNAKE_CASE（例如：`GAME_SPEED_MULTIPLIER`）

### 注释

- 公共 API 使用 KDoc 格式
- 复杂逻辑添加行内注释说明"为什么"而非"是什么"

## 详见

[Unciv 编码标准文档](https://yairm210.github.io/Unciv/Developers/Coding-standards/)