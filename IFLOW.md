# Unciv 项目配置

## 项目概述

Unciv 是一个开源的、注重可 Mod 性的 Android 和桌面端《文明5》复刻版，使用 LibGDX 框架开发。这是一个跨平台项目，支持 Android、Desktop (Windows/Linux/macOS) 和 Server 构建。

## 技术栈

- **语言**: Kotlin 2.1.21
- **构建工具**: Gradle 8.11.1
- **游戏框架**: LibGDX 1.14.0
- **异步处理**: Kotlin Coroutines 1.8.1
- **网络**: Ktor 3.2.3 (客户端和服务器)
- **序列化**: Kotlinx Serialization
- **Android**: Android Gradle Plugin 8.9.3, Android 16.0 (Baklava)
- **JNA**: 5.17.0 (用于 Windows 系统功能)
- **纯度检查**: purity-plugin 1.3.2 (自定义插件)
- **代码质量**: Detekt 1.23.8

## 项目结构

```
Unciv/
├── core/              # 核心游戏逻辑和共享代码 (Kotlin)
├── desktop/           # 桌面端特定代码 (Kotlin)
├── android/           # Android 端特定代码 (Kotlin + Android)
├── server/            # 多人游戏服务器 (Kotlin + Ktor)
├── tests/             # 单元测试
├── android/assets/    # 游戏资源 (图片、JSON、音效等)
├── buildSrc/          # Gradle 构建逻辑
├── docs/              # 项目文档
└── extraImages/       # 额外图片资源
```

### 模块说明

- **core**: 包含所有游戏逻辑、模型、规则集、UI 组件等。这是纯 Kotlin 模块，不依赖特定平台。
- **desktop**: 桌面端启动器、系统特定功能（剪贴板、Discord RPC 等）。
- **android**: Android 端配置、权限、启动器。
- **server**: 基于 Ktor 的多人游戏服务器，支持 WebSocket 和 REST API。
- **tests**: 使用 JUnit 4 和 Mockito 的单元测试。

## 代码风格规范

### 命名约定

- **类名**: PascalCase (例如: `Civilization`, `City`, `Unique`)
- **函数名**: camelCase (例如: `getWorkerAutomation`, `hasFlag`)
- **变量名**: camelCase (例如: `gameInfo`, `currentTurn`)
- **常量**: UPPER_SNAKE_CASE (例如: `GAME_SPEED_MULTIPLIER`)
- **包名**: 全小写，按功能分层 (例如: `com.unciv.logic.civilization`)

### Kotlin 代码规范

1. **包导入顺序**: 标准库 -> 第三方库 -> 项目内部
   ```kotlin
   import kotlin.math.max
   import com.badlogic.gdx.math.Vector2
   import com.unciv.logic.civilization.Civilization
   ```

2. **类定义**:
   - 使用 `class` 定义普通类
   - 使用 `data class` 定义数据类
   - 使用 `object` 定义单例
   - 使用 `enum class` 定义枚举
   - 使用 `interface` 定义接口

3. **可见性修饰符**:
   - 默认使用 `public`
   - 内部使用 `private`
   - 跨模块使用 `internal`
   - 避免使用 `protected` (项目倾向于使用 `internal` 或扩展函数)

4. **属性定义**:
   - 使用 `val` 表示不可变属性
   - 使用 `var` 表示可变属性
   - 使用 `lateinit` 延迟初始化非空属性
   - 使用 `@Transient` 标记不需要序列化的属性

5. **函数定义**:
   - 使用表达式函数简化单行函数
   - 使用默认参数减少重载
   - 使用命名参数提高可读性

6. **空安全**:
   - 优先使用非空类型
   - 必要时使用可空类型 `Type?`
   - 使用安全调用 `?.` 和 Elvis 操作符 `?:`

7. **集合操作**:
   - 优先使用不可变集合 (`listOf`, `mapOf`)
   - 使用 Kotlin 标准库函数 (`map`, `filter`, `flatMap` 等)
   - 使用序列处理大数据集

### 特殊注释和注解

1. **@Transient**: 标记不应该被序列化的属性
   ```kotlin
   @Transient
   lateinit var gameInfo: GameInfo
   ```

2. **@Readonly**: 标记只读方法 (purity-plugin)
   ```kotlin
   @Readonly
   fun hasFlag(flag: UniqueFlag) = type != null && type.flags.contains(flag)
   ```

3. **@Cache**: 标记缓存方法 (purity-plugin)
   ```kotlin
   @Cache
   fun getCachedValue(): Int
   ```

4. **@VisibleForTesting**: 标记测试可见的方法
   ```kotlin
   @VisibleForTesting
   internal fun testMethod()
   ```

5. **实验性功能**:
   ```kotlin
   @ExperimentalContracts
   ```

### 文档注释

- 公共 API 使用 KDoc 格式
- 简单属性不需要文档
- 复杂逻辑添加行内注释说明"为什么"而非"是什么"

## 核心设计原则

### 1. Crashing Early Philosophy
当游戏进入不正确状态时，应该立即崩溃，而不是继续运行。这样可以让问题更容易被发现和修复。

### 2. Modding Philosophy
- 最小化对象数量，最大化交互组合
- 参数化 Unique > 多个 Unique
- 条件应该是 Conditionals，可以应用到所有 Unique
- 触发式 Unique 和 unique triggers - 所有组合
- 单位行动修饰符，而非特定单位行动的特殊属性

### 3. AI Playing to Win
AI 应该像真实玩家一样努力获胜，不会因为"原则"拒绝交易，会在你军事薄弱时攻击你。

## 构建和运行

### Desktop 运行
```bash
./gradlew desktop:run
```

### Desktop 构建 JAR
```bash
./gradlew desktop:dist
# 输出: desktop/build/libs/Unciv.jar
```

### Android 构建
需要配置 `local.properties` 文件或设置 `ANDROID_HOME` 环境变量：
```
sdk.dir = /path/to/android/sdk
```

### 运行测试
```bash
./gradlew :tests:test
```

### Server 运行
```bash
./gradlew server:run
```

### Server 构建 JAR
```bash
./gradlew server:dist
# 输出: server/build/libs/UncivServer.jar
```

## 测试

- 使用 JUnit 4 进行单元测试
- 使用 Mockito 进行模拟
- 测试文件位于 `tests/` 模块
- 在 Android Studio 中配置 "Unit Tests" 运行配置来运行测试

## 代码质量

### Detekt
项目使用 Detekt 进行代码质量检查。
- 配置文件位于 `.github/workflows/detekt_config/`
- 生成警告报告: 使用 `detekt-warnings.yml`
- 生成错误报告: 使用 `detekt-errors.yml`

### Purity Plugin
自定义插件用于检测纯函数和副作用：
- `@Readonly`: 标记只读方法
- `@Cache`: 标记缓存方法
- 配置了已知的纯函数类和只读函数

## 序列化

- 使用 Kotlinx Serialization 进行 JSON 序列化
- 游戏状态通过 `UncivJson` 序列化
- 所有需要序列化的类必须实现 `IsPartOfGameInfoSerialization` 接口
- 使用 `@Transient` 标记不需要序列化的属性

## 翻译

- 翻译文件位于 `android/assets/jsons/translations/`
- 使用 `tr()` 函数获取翻译字符串
- 支持 `template.properties` 格式
- 翻译完成度: `android/assets/jsons/translations/completionPercentages.properties`

### 翻译占位符系统

**占位符标签的作用**

`[]` 内的内容是**给人类翻译者看的标签**，用于区分一句话中的多个占位符。

**示例：**
```
"The city of [city] has built [building] in [turn]"
```

这里有三个占位符：`[city]`、`[building]`、`[turn]`，标签告诉翻译者每个位置应该填什么类型的内容。

**翻译文件格式**

翻译文件中**必须保持相同的标签结构**：

```
The city of [city] has built [building] in [turn] = [city]建造了[building]，用时[turn]回合
```

**fillPlaceholders 的工作方式**

`fillPlaceholders()` 会：
1. 从原文中提取每个 `[]` 内的标签
2. 按顺序用提供的参数替换翻译中对应的标签

**代码示例：**
```kotlin
val message = "The city of [city] has built [building]".fillPlaceholders("北京", "图书馆")
// 结果：The city of 北京 has built 图书馆
message.tr()
// 最终显示：北京建造了图书馆
```

**注意事项：**

1. 不要使用空的 `[]`，必须使用有意义的标签，如 `[amount]`、`[city]`、`[building]` 等
2. 所有语言翻译文件必须保持相同的标签结构
3. 如果一句话有多个占位符，必须使用不同的标签来区分
4. 测试 `allTranslationsHaveNoExtraPlaceholders` 和 `allTranslationsHaveCorrectPlaceholders` 会验证占位符的一致性

## Mod 系统

- Mod 位于 `android/assets/mods/`
- Mod 可以扩展规则集、添加单位、建筑等
- 通过 `Unique` 系统实现高度可定制性
- Mod 选项通过 `ModOptions` 配置

## 资源文件

### 图片资源
- Atlas 文件位于 `android/assets/` (如 `Icons.atlas`, `NationIcons.atlas`)
- 源图片位于 `android/Images.*` 目录

### 音效
- 位于 `android/assets/sounds/`

### 规则集 JSON
- 位于 `android/assets/jsons/`
- 包括建筑、单位、政策、科技等定义

## Android Studio 推荐设置

1. **Commit 设置**: Settings > Version Control > Commit > Advanced Commit
   - 关闭 "Analyze code"
   - 关闭 "Use non-modal commit interface"

2. **Kotlin 缩进**: Settings > Editor > Code Style > Kotlin > Tabs and Indents
   - Continuation Indent: 4

3. **保存设置**: Settings > Editor > General > On Save
   - 取消勾选 "Remove trailing spaces on: [...]" (用于翻译文件)
   - 注意: 提交前需要手动删除非翻译文件的尾随空格

4. **排除目录**: 右键 `android/assets/SaveFiles` -> "Mark directory as" > Excluded
   - 对 `android/assets/mods` 和其他测试文件做同样处理

## 重要文件说明

- `build.gradle.kts`: 根项目构建配置
- `settings.gradle.kts`: 项目模块配置
- `gradle.properties`: 依赖版本和 Gradle 配置
- `local.properties`: 本地配置 (SDK 路径等，不提交到 Git)
- `GameSettings.json`: 游戏设置 (运行时生成)
- `keystore.jks`: Android 签名密钥 (不提交到 Git)

## 开发注意事项

1. **平台兼容性**: 核心代码必须在所有平台 (Desktop/Android/Server) 上运行
2. **序列化**: 所有游戏状态变更必须正确处理序列化
3. **性能**: 注意避免在游戏循环中创建大量临时对象
4. **内存**: 使用对象池或缓存减少内存分配
5. **线程安全**: 游戏逻辑主要在主线程执行，异步操作需要谨慎处理
6. **Mod 兼容性**: 修改规则集相关代码时考虑 Mod 兼容性

## Git 工作流

- 主分支: `master`
- 功能分支: `feature/*`
- 修复分支: `hotfix/*`
- 所有更改通过 Pull Request 合并
- CI/CD 通过 GitHub Actions 自动构建和测试

## 版本管理

- 版本号定义在 `buildSrc/src/com/unciv/build/BuildConfig.kt`
- 遵循语义化版本
- 变更日志记录在 `changelog.md`

## 环境变量

- `ANDROID_HOME`: Android SDK 路径 (可选，也可通过 `local.properties` 配置)
- Gradle JVM 参数已在 `gradle.properties` 中配置

## 常见问题

1. **Android SDK 未找到**: 设置 `local.properties` 或 `ANDROID_HOME` 环境变量
2. **内存不足**: 增加 Gradle JVM 堆内存 (已在 `gradle.properties` 中配置为 4GB)
3. **JDK 版本**: 需要 JDK 11 或更高版本
4. **Android Studio 同步失败**: 检查 Android SDK Build-Tools 版本

## 相关资源

- 官方文档: https://yairm210.github.io/Unciv/
- GitHub: https://github.com/yairm210/Unciv
- Discord: https://discord.gg/bjrB4Xw
- Civ V Wiki: https://civilization.fandom.com/