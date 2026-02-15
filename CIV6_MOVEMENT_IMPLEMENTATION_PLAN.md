# Unciv 文明6风格移动力系统实现计划书

## 一、目标概述

在Unciv中添加"文明6风格移动力"游戏设置选项，实现以下核心规则：

1. 劫掠消耗3移动力
2. 跨河消耗3移动力
3. 地形移动力损耗累计（丘陵+树林=3）
4. 行动消耗所有剩余移动力
5. 移动力不足时不能移动（必须有足够移动力才能进入地形）
6. 满移动力单位总能移动1格（特殊情况）

## 二、当前Unciv移动力系统分析

### 核心机制（文明5风格）：
- **事后扣除模式**：单位先进入地块，然后扣除移动力
- **路径规划**：使用A*算法计算路径，累计移动力消耗
- **移动验证**：`canMoveTo()` 检查是否可进入地块（不考虑移动力是否足够）
- **移动力检查**：在 `moveToTile()` 中实时检查剩余移动力，不足则停止

### 关键文件：
1. **MovementCost.kt** - 计算相邻地块间的移动力消耗
2. **UnitMovement.kt** - 处理单位移动逻辑、路径规划
3. **MapUnit.kt** - 单位属性（currentMovement等）
4. **GameParameters.kt** - 游戏参数设置

## 三、实现方案

### 阶段一：简单规则

#### 1.1 劫掠消耗3移动力
- **文件**：`UnitActionsPillage.kt`
- **当前逻辑**：劫掠消耗1移动力
- **新逻辑**：根据 `civ6MovementCosts` 设置，劫掠消耗3或1移动力
- **实现位置**：`getPillageAction()` 函数中的 `unit.useMovementPoints(1f)` 调用
- **代码变更**：
  ```kotlin
  val freePillage = unit.hasUnique(UniqueType.NoMovementToPillage, checkCivInfoUniques = true)
  if (!freePillage) {
      // Civ 6 style: pillaging costs 3 movement points instead of 1
      val pillageCost = if (unit.civ.gameInfo.gameParameters.civ6MovementCosts) 3f else 1f
      unit.useMovementPoints(pillageCost)
  }
  ```

#### 1.2 跨河消耗3移动力
- **文件**：`MovementCost.kt`
- **当前逻辑**：跨河消耗100移动力（整回合）
- **新逻辑**：根据 `civ6MovementCosts` 设置，跨河消耗3或100移动力
- **实现位置**：`getMovementCostBetweenAdjacentTiles()` 函数
- **代码变更**：
  ```kotlin
  if (areConnectedByRiver) {
      // Civ 6 style: crossing rivers costs 3 movement points instead of entire turn
      if (civ.gameInfo.gameParameters.civ6MovementCosts) return 3f + extraCost
      return 100f  // Rivers take the entire turn to cross
  }
  ```

#### 1.3 地形移动力损耗累计
- **文件**：`MovementCost.kt`
- **当前逻辑**：取最大值（max(丘陵2, 树林2) = 2）
- **新逻辑**：累加（丘陵2 + 树林2 = 3）
- **实现位置**：`getMovementCostBetweenAdjacentTiles()` 函数
- **代码变更**：
  ```kotlin
  // 文明5风格（当前）
  val terrainCost = if (to.isHill()) 2f else 1f
  if (to.hasForest()) terrainCost = maxOf(terrainCost, 2f)

  // 文明6风格（新增）
  if (civ.gameInfo.gameParameters.civ6MovementCosts) {
      var terrainCost = 1f
      if (to.isHill()) terrainCost += 1f  // 丘陵额外+1
      if (to.hasForest() || to.hasJungle() || to.hasMarsh()) terrainCost += 1f  // 特征额外+1
      return terrainCost + extraCost
  }
  ```

#### 1.4 行动消耗所有移动力
- **文件**：`MapUnit.kt` 的 `useMovementPoints()` 方法
- **当前逻辑**：消耗指定数量的移动力
- **新逻辑**：某些行动（攻击、劫掠）消耗所有剩余移动力
- **实现方式**：
  ```kotlin
  fun useMovementPoints(amount: Float, consumeAll: Boolean = false) {
      if (consumeAll) {
          currentMovement = 0f
      } else {
          currentMovement -= amount
      }
      if (currentMovement < Constants.minimumMovementEpsilon) currentMovement = 0f
  }
  ```
- **调用位置**：
  - `UnitActionsPillage.kt` - 劫掠
  - `Battle.kt` - 攻击
  - 其他行动相关文件

---

### 阶段二：核心规则重构（复杂）

#### 2.1 移动力不足时不能移动

这是文明6移动力系统最核心的变化，需要**大规模重构**：

**问题分析**：
- 当前Unciv使用"事后扣除"模式：进入地块后才扣除移动力
- 文明6使用"事前预扣"模式：必须有足够移动力才能进入地块
- 这影响整个路径规划系统

**实现方案**：

##### 方案A：修改路径规划算法（推荐）
在 `UnitMovement.getDistanceToTiles()` 中添加检查：

```kotlin
// 在计算移动力消耗时
if (civ.gameInfo.gameParameters.civ6MovementCosts) {
    // 文明6风格：必须有足够移动力才能进入
    if (totalDistanceToTile > usableMovement) {
        // 不能进入此地块，跳过
        continue
    }
} else {
    // 文明5风格：可以进入，消耗所有移动力
    if (totalDistanceToTile > usableMovement - Constants.minimumMovementEpsilon) {
        totalDistanceToTile = usableMovement
    }
}
```

##### 方案B：修改canMoveTo()方法
添加移动力检查参数：

```kotlin
fun canMoveTo(
    tile: Tile,
    assumeCanPassThrough: Boolean = false,
    allowSwap: Boolean = false,
    includeOtherEscortUnit: Boolean = true,
    checkMovement: Boolean = false  // 新增参数
): Boolean {
    // 原有检查...

    // 新增：文明6风格移动力检查
    if (checkMovement && civ.gameInfo.gameParameters.civ6MovementCosts) {
        val movementCost = getMovementCostTo(tile)
        if (currentMovement < movementCost - Constants.minimumMovementEpsilon) {
            return false
        }
    }

    return true
}
```

**影响范围**：
- `UnitMovement.getDistanceToTiles()` - 路径规划
- `UnitMovement.moveToTile()` - 移动执行
- `UnitMovement.canMoveTo()` - 移动验证
- A*算法逻辑

#### 2.2 满移动力单位总能移动1格

这是文明6的例外规则，防止单位被困住：

**实现逻辑**：
```kotlin
fun getMovementCostBetweenAdjacentTiles(...): Float {
    // ... 正常计算移动力消耗

    // 文明6特殊规则：满移动力单位总能移动1格
    if (civ.gameInfo.gameParameters.civ6MovementCosts &&
        unit.currentMovement >= unit.getMaxMovement() - Constants.minimumMovementEpsilon) {
        // 如果这是本回合第一次移动，强制消耗所有移动力但允许移动
        return unit.getMaxMovement().toFloat()
    }

    return movementCost
}
```

**注意事项**：
- 只在单位有完整移动力时生效
- 消耗整回合移动力
- 适用于任何相邻地块

---

### 阶段三：UI设置

#### 3.1 游戏设置选项
- **文件**：`GameOptionsTable.kt`
- **位置**：高级设置（Advanced Settings）
- **显示文本**："Civ 6 Movement Costs"
- **实现方式**：
  ```kotlin
  private fun Table.addCiv6MovementCostsCheckbox() =
      addCheckbox("Civ 6 Movement Costs", gameParameters.civ6MovementCosts)
      { gameParameters.civ6MovementCosts = it }
  ```
- **添加位置**：`update()` 方法中的 ExpanderTab 内容

#### 3.2 工具提示
添加详细说明，告知玩家各项规则的变化：
```
启用文明6风格移动力：
- 劫掠消耗3移动力（文明5为1）
- 跨河消耗3移动力（文明5消耗整回合）
- 地形损耗累计（丘陵+树林=3，文明5为2）
- 行动消耗所有移动力
- 必须有足够移动力才能进入地形
- 满移动力单位总能移动1格
```

---

## 四、实施步骤

### Step 1: 添加游戏设置参数（30分钟）
- 修改 `GameParameters.kt`
- 添加 `civ6MovementCosts` 布尔属性
- 在 `clone()` 方法中添加复制逻辑

### Step 1.5: 添加新的Unique类型（30分钟）
- 修改 `UniqueType.kt`
- 添加 `PillageMovementCostReduction` unique类型
- 添加 `RiverCrossingMovementCostReduction` unique类型
- 更新UniqueParameterType.kt（如需要）
- 确保向后兼容现有的 `NoMovementToPillage`

### Step 2: 添加UI设置选项（30分钟）
- 修改 `GameOptionsTable.kt`
- 添加复选框函数
- 在高级设置中添加调用

### Step 3: 实现劫掠消耗3移动力（30分钟）
- 修改 `UnitActionsPillage.kt`
- 根据 `civ6MovementCosts` 设置调整移动力消耗

### Step 4: 实现跨河消耗3移动力（30分钟）
- 修改 `MovementCost.kt`
- 根据 `civ6MovementCosts` 设置调整跨河消耗

### Step 5: 实现地形移动力累计（1-2小时）
- 修改 `MovementCost.kt`
- 添加丘陵、树林、雨林、沼泽的累加逻辑
- 测试各种地形组合

### Step 6: 实现行动消耗所有移动力（30分钟）
- 修改 `useMovementPoints()` 方法
- 修改攻击、劫掠等行动的调用
- 确保不影响其他不需要消耗所有移动力的情况

### Step 7: 实现移动力预扣检查（3-5小时）⚠️
- 修改 `getDistanceToTiles()` 算法
- 修改 `moveToTile()` 逻辑
- 修改 `canMoveTo()` 检查
- **这是最复杂的一步，需要仔细测试**

### Step 8: 实现满移动力例外规则（1小时）
- 在 `getMovementCostBetweenAdjacentTiles()` 中添加检查
- 确保只在本回合第一次移动时生效
- 测试被困单位情况

### Step 9: 综合测试（2-3小时）
- 测试所有移动力规则
- 测试路径规划
- 测试特殊情况（单位被困、跨河、劫掠等）
- 测试与现有功能的兼容性

### Step 10: Unique系统测试（1-2小时）
- 测试 `NoMovementToPillage` 在文明6模式下的兼容性
- 测试新的 `PillageMovementCostReduction` unique
- 测试 `IgnoreHillMovementCost` 在文明6模式下的行为
- 测试 `Double movement` unique与地形累计的交互
- 测试 `RoadsConnectAcrossRivers` 在文明6模式下的行为
- 测试多种unique组合的效果
- 确保所有测试通过，无回归问题

---

## 五、风险与注意事项

### 高风险项：
1. **路径规划重构**（Step 7）
   - 可能影响AI移动逻辑
   - 可能影响单位自动寻路
   - 需要大量测试

2. **兼容性问题**
   - 现有Mod可能依赖文明5风格移动力
   - 需要考虑向后兼容性

### 测试重点：
- 单位在复杂地形中的移动
- 路径规划的准确性
- AI单位的移动行为
- 多回合路径的可行性
- 特殊情况（单位被困、Zone of Control等）

---

## 六、时间估算

| 阶段 | 任务 | 预计时间 | 优先级 |
|------|------|----------|--------|
| 准备 | 添加游戏设置参数 | 30分钟 | 高 |
| 准备 | 添加Unique类型定义 | 30分钟 | 高 |
| 准备 | 添加UI设置选项 | 30分钟 | 高 |
| 阶段一 | 劫掠消耗3移动力（含Unique支持） | 1小时 | 高 |
| 阶段一 | 跨河消耗3移动力（含Unique支持） | 1小时 | 高 |
| 阶段一 | 地形移动力累计（含Unique兼容） | 1-2小时 | 高 |
| 阶段一 | 行动消耗所有移动力 | 30分钟 | 高 |
| 阶段二 | 移动力预扣检查 | 3-5小时 | 高⚠️ |
| 阶段二 | 满移动力例外规则 | 1小时 | 中 |
| 测试 | 综合测试 | 2-3小时 | 高 |
| 测试 | Unique系统测试 | 1-2小时 | 高 |
| **总计** | | **11.5-16.5小时** | |

---

## 七、建议实施顺序

### 选项A：分阶段实施（推荐）
1. **第一阶段**：实现简单规则（劫掠、跨河、地形累计、行动消耗）
   - 风险低，易于测试
   - 可以独立发布

2. **第二阶段**：实现核心规则（移动力预扣、满移动力例外）
   - 风险高，需要充分测试
   - 作为单独的功能发布

### 选项B：一次性实施
- 一次性实现所有规则
- 风险较高，但功能完整
- 需要更长的测试周期

---

## 八、代码修改清单

### 必须修改的文件：
1. `GameParameters.kt` - 添加设置参数
2. `GameOptionsTable.kt` - 添加UI选项
3. `UniqueType.kt` - 添加新的Unique类型（PillageMovementCostReduction、RiverCrossingMovementCostReduction）
4. `UnitActionsPillage.kt` - 劫掠移动力逻辑
5. `MovementCost.kt` - 跨河、地形损耗计算
6. `MapUnit.kt` - useMovementPoints方法
7. `UnitMovement.kt` - 路径规划、移动逻辑
8. `Battle.kt` - 攻击消耗移动力
9. 相关测试文件

### 可选修改：
- `MapPathing.kt` - 路径规划辅助
- `UnitAutomation.kt` - AI移动逻辑
- `WorkerAutomation.kt` - 工人自动移动
- `IFLOW.md` - 更新文档说明

---

## 九、Unique系统兼容性

### 9.1 现有Unique系统分析

Unciv使用Unique系统来定义各种游戏规则和能力，移动力相关的Unique包括：

#### 现有移动力相关Unique：
- `NoMovementToPillage` - 劫掠不消耗移动力
- `IgnoreHillMovementCost` - 忽略丘陵移动力消耗
- `Double movement in [terrain]` - 特定地形移动力减半
- `AttackAcrossRiver` - 跨河攻击无惩罚
- `RoadsConnectAcrossRivers` - 道路连接跨河

### 9.2 劫掠移动力Unique扩展方案

#### 9.2.1 兼容现有Unique
现有的 `NoMovementToPillage` unique 需要在文明6模式下继续生效，即：
- 文明5模式：劫掠消耗1移动力 → `NoMovementToPillage` 使其消耗0
- 文明6模式：劫掠消耗3移动力 → `NoMovementToPillage` 使其消耗0

#### 9.2.2 新增Unique类型
扩展原有的 `NoMovementToPillage` 为更灵活的参数化unique：

**新增UniqueType**：
```kotlin
// 在 UniqueType.kt 中添加
PillageMovementCostReduction(
    "Pillaging costs [-positiveAmount] movement",
    UniqueTarget.Unit,
    UniqueTarget.Global
)
```

**实现逻辑**：
```kotlin
// 在 UnitActionsPillage.kt 中
fun getPillageMovementCost(unit: MapUnit): Float {
    val baseCost = if (unit.civ.gameInfo.gameParameters.civ6MovementCosts) 3f else 1f

    // 检查 NoMovementToPillage（向后兼容）
    if (unit.hasUnique(UniqueType.NoMovementToPillage, checkCivInfoUniques = true)) {
        return 0f
    }

    // 检查新的移动力减少unique
    var reduction = 0f
    for (unique in unit.getMatchingUniques(UniqueType.PillageMovementCostReduction, checkCivInfoUniques = true)) {
        reduction += unique.params[0].toFloat()
    }

    // 应用减少量，但不低于0
    return maxOf(0f, baseCost - reduction)
}

// 使用示例
val pillageCost = getPillageMovementCost(unit)
if (pillageCost > 0f) {
    unit.useMovementPoints(pillageCost)
}
```

**Unique使用示例**：
```
# 现有写法（继续有效）
<Promotion>
    <UniqueType>NoMovementToPillage</UniqueType>
</Promotion>

# 新写法（更灵活）
<Promotion>
    <UniqueType>Pillaging costs [-1] movement</UniqueType>
</Promotion>

# 文明6模式下的有效写法
<Promotion>
    <UniqueType>Pillaging costs [-2] movement</UniqueType>
</Promotion>
```

### 9.3 跨河移动力Unique扩展

#### 9.3.1 新增Unique类型
允许Mod定义跨河移动力消耗的修改：

**新增UniqueType**：
```kotlin
RiverCrossingMovementCostReduction(
    "Crossing rivers costs [-positiveAmount] movement",
    UniqueTarget.Unit,
    UniqueTarget.Global
)
```

**实现逻辑**：
```kotlin
// 在 MovementCost.kt 中
fun getRiverCrossingCost(unit: MapUnit): Float {
    val baseCost = if (unit.civ.gameInfo.gameParameters.civ6MovementCosts) 3f else 100f

    // 如果是文明5模式且单位有特殊能力，可以减少跨河消耗
    if (!unit.civ.gameInfo.gameParameters.civ6MovementCosts) {
        // 文明5模式：某些unique可以完全忽略跨河消耗
        if (unit.hasUnique(UniqueType.AttackAcrossRiver)) {
            return 0f  // 完全忽略
        }
    }

    // 检查跨河移动力减少unique
    var reduction = 0f
    for (unique in unit.getMatchingUniques(UniqueType.RiverCrossingMovementCostReduction, checkCivInfoUniques = true)) {
        reduction += unique.params[0].toFloat()
    }

    return maxOf(0f, baseCost - reduction)
}
```

### 9.4 地形移动力Unique兼容

现有的地形相关Unique需要与文明6的新逻辑兼容：

#### 现有Unique：
- `IgnoreHillMovementCost` - 忽略丘陵移动力
- `Double movement in [terrain]` - 特定地形移动力减半

#### 文明6模式下的调整：
```kotlin
// 在 MovementCost.kt 中
if (civ.gameInfo.gameParameters.civ6MovementCosts) {
    var terrainCost = 1f

    // 检查忽略丘陵消耗
    if (unit.hasUnique(UniqueType.IgnoreHillMovementCost) && to.isHill()) {
        // 忽略丘陵的额外消耗
    } else if (to.isHill()) {
        terrainCost += 1f
    }

    // 检查地形特征消耗
    val hasFeature = to.hasForest() || to.hasJungle() || to.hasMarsh()
    if (hasFeature) {
        // 检查双倍移动unique
        if (hasDoubleMovement(unit, to)) {
            terrainCost += 0.5f  // 双倍移动：额外消耗从1减少到0.5
        } else {
            terrainCost += 1f
        }
    }

    return terrainCost + extraCost
}
```

### 9.5 向后兼容性保证

为了确保现有Mod在启用文明6移动力模式时仍能正常工作：

1. **NoMovementToPillage** - 继续生效，劫掠消耗变为0
2. **IgnoreHillMovementCost** - 在文明6模式下，忽略丘陵的+1消耗
3. **Double movement** - 在文明6模式下，将额外消耗从1减少到0.5
4. **RoadsConnectAcrossRivers** - 继续生效，道路连接时忽略跨河消耗
5. **AttackAcrossRiver** - 继续生效，攻击时无跨河惩罚（战斗相关，不影响移动力）

### 9.6 Unique测试清单

实施时需要测试以下Unique组合：

| Unique类型 | 文明5模式 | 文明6模式 | 备注 |
|-----------|----------|----------|------|
| NoMovementToPillage | 劫掠消耗0 | 劫掠消耗0 | 向后兼容 |
| PillageMovementCostReduction[-1] | 劫掠消耗0 | 劫掠消耗2 | 新功能 |
| PillageMovementCostReduction[-3] | 劫掠消耗0 | 劫掠消耗0 | 完全抵消 |
| IgnoreHillMovementCost | 丘陵消耗1 | 丘陵消耗1 | 文明6下+1被忽略 |
| Double movement in Woods | 树林消耗1 | 树林+丘陵消耗2.5 | 文明6下0.5+1=1.5 |
| RoadsConnectAcrossRivers | 跨河消耗道路消耗 | 跨河消耗道路消耗 | 不受影响 |
| AttackAcrossRiver | 跨河攻击无惩罚 | 跨河攻击无惩罚 | 不受影响 |

### 9.7 实现注意事项

1. **Unique参数验证**：确保移动力减少参数为正数
2. **最小值限制**：移动力消耗不能为负数，最小为0
3. **缓存优化**：考虑对Unique检查结果进行缓存，避免重复计算
4. **日志记录**：添加调试日志，记录Unique应用情况
5. **Mod文档**：在IFLOW.md中添加新Unique的使用说明

---

## 十、后续优化建议

1. **性能优化**：路径规划可能变慢，需要优化算法
2. **UI反馈**：在地块上显示预计消耗的移动力
3. **Mod支持**：允许Mod自定义移动力规则
4. **设置细化**：允许玩家单独启用/禁用各项规则
5. **Unique扩展**：考虑为其他行动（攻击、建设）添加类似的移动力修改unique

---

## 十一、参考文档

- [Civilization 6 Movement Wiki](https://civilization.fandom.com/wiki/Movement_(Civ6))
- [Civilization 6 Terrain Wiki](https://well-of-souls.com/civ/civ6_terrain.html)
- Unciv IFLOW.md - 项目代码规范
- Unciv CUSTOM_DAMAGE_FORMULA_DESIGN.md - 设计文档参考