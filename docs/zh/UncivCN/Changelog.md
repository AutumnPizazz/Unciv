---
title: UncivCN 更新日志
---

# UncivCN 更新日志

版本号规则：上游版本 + CN 子版本号（`.1`、`.2`、`.3`…，同一上游版本可发多个 CN 子版本，如 4.20.8.1 → 4.20.8.4；跟进新上游后从 `.1` 重新开始，如 4.21.5 → 4.21.5.1）。

详细历史记录见 [UncivCN 更新日志（社区归档）](/zh/UncivCN/Changelog)。

## 未发布（Unreleased）

## v4.21.6.4（build 1248）

- CI：修复 GitHub Release 说明文本为空——现在按发布 tag 从 CN 中英文更新日志（docs/UncivCN + docs/zh/UncivCN）提取对应版本小节，中英双语写入 Release 说明（此前 CN 四段式版本号在上游 changelog.md 中不存在，导致正文为空）
- 模组管理：新增“模组下载源”设置（选项 → 高级），国内等无法稳定访问 GitHub 的玩家可将模组列表、预览图与下载全部切到公共 GitHub 加速镜像（gh-proxy.com / ghfast.top / ghproxy.net，或任意自定义前缀）；官方源加载模组列表失败时，游戏会弹窗询问一键切换镜像源并自动重试；新增“国内玩家模组安装指南”文档页
- 移除新建游戏页"重置为默认"按钮：读取配置列表新增内置且不可删除的"默认配置"条目，可恢复完全相同的默认选项（保留原确认弹窗）
- 新建游戏页新增"保存当前配置/读取已保存配置"按钮（位于剪贴板按钮右侧）：配置以与剪贴板导出相同的格式存入 SaveFiles/GameSetup 下的命名槽位，读取弹窗可选择或删除槽位；窄屏时按钮独占一行，位于剪贴板行与开始游戏行之间
- 新建游戏页的剪贴板复制/粘贴按钮从左上角（游戏选项列顶部）移到右下角"开始游戏"旁：宽屏与开始游戏同一行、位于其左侧；窄屏单独一行放在开始游戏上方，避免撑破底部栏
- 文档：Coding-standards 新增发版检查清单与踩坑经验（tag 推送、文档站本地验证、禁止裸尖括号、Maven Central 403、Unciv.jar 旧名残留、continue-on-error 掩盖失败、CN 词条翻译完整性）

## v4.21.6.3（build 1247）

- 修复 Docker 构建：Dockerfile 复制的 `Unciv.jar` 在 CN 分支实际产物名为 `UncivCN.jar`（改名后镜像构建一直失败）
- 修复 MSI 发布：`wix build` 默认按源文件名输出 `unciv.msi`，而上传路径是 `UncivCN.msi`；`build-msi` job 带 `continue-on-error: true`，历次 Release 从未包含 MSI——现在显式指定输出文件名

## v4.21.6.2（build 1246）

- 修复 CN 自研功能词条缺失简体中文翻译（47 个缺失 + 10 个空翻译）：剪贴板复制/粘贴游戏设置、轮询联机（轮询间隔、刷新、在线玩家、回合计时）、镜像地图对称模式、自动锁定、地图钉笔记、读档随机数变动、Countables、寒带/螺旋地图类型等——此前显示为英文
## v4.21.6.1（build 1245）

- 修复笔记跨存档泄漏：笔记改为按 gameId 存储（"notes_{gameId}"）而非存档文件名——同一局游戏的手动存档/自动存档/另存为共享笔记，不同游戏严格隔离（此前共享的 "Autosave" 名称会导致笔记在不同游戏的自动存档间串档，且未保存的新游戏笔记会丢失）；旧 "{存档名}_notes" 文件首次加载时自动迁移，删档时两种命名都会清理，存档列表过滤新命名
- 地图钉/单位钉交互重构：手机长按、桌面 Alt+点击即可编辑备注，无需任何开关（普通点击永不被劫持，旧开关保留为备选）；备注气泡截断为 8 字符，点击气泡弹出全文查看（支持编辑/删除）；地块笔记开关改用星标图标，两个开关桌面悬停有文字提示
- 修复地图钉/单位钉窗口本地化：补齐笔记相关 UI 词条的中文翻译（"为地块添加笔记"、"为[unitName]添加笔记"、"添加笔记"、"笔记"、"显示单位/地块笔记"）——此前显示为英文
- 修复打开地图钉编辑弹窗时崩溃：地块材质包预览使用的 TileMapView 按地块数分配数组，真实地块（非零索引）会越界（现按最大索引扩容，并新增回归测试）
- 地图钉编辑弹窗：标题去掉地块坐标，左侧占位图标改为选中地块的材质包实时预览（地形/资源/改良/河流贴图）
- 内置基础规则集（Vanilla / Gods & Kings）与上游对齐：移除 G&K ModOptions 中 CN 独有的 "Allow cities to claim tiles"（tile-claim 改为模组通过 ModOptions 自行声明启用）与 CN 独有的大军事家 8 回合黄金时代 unique（上游 #13308 已移除）
- 修复开图界面：世界大小设为自定义后反复切换对称性会堆积重复的半径/宽度/高度输入行（六边形/矩形尺寸表重建时未清空）
- 新特性：`ModOptions.json` 模组版本要求——`modVersion`（n.n.n，不填默认 0.0.1）、`gameVersionRange`（min~max，不填默认全版本可用）、`modDependencies`（精确或范围版本要求）；不满足时在模组管理器、新建游戏模组选择与模组检查器中显示警告，不阻止使用
- 修复文档站死链接：中文 UncivCN 页面 14 处 `](` 损坏链接，以及 Modders 文档（中英，含 `UniqueType.kt` / `Countables.kt` / `UniqueDocsWriter` / `MergeActionDocsWriter` 生成器）约 250 处标题锚点错误（VitePress slug 格式）；已用全新 VitePress 构建产物逐链接验证
- 合并上游 4.21.6：CPU 性能优化（城市基线只算一次，部分存档下一回合提速约 20%）、AI 工人考虑未来的相邻加成、多人上传失败视觉提示、聊天昵称显示文明颜色、OneTimeGainStat 参数改为 `[civWideStat]` 并对非文明级产出给出 modding 警告、测试运行器重构（详见上游 [changelog.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/changelog.md)）
- 修复自定义域名下文档站无样式：站点产物部署到 `Unciv/` 子目录并加根跳转页
- 修复导航栏 logo 路径重复 `/Unciv` 前缀
- 游戏内版本号改为构建时自动从 `BuildConfig.kt` 同步到 `UncivGame.kt`（此前手动发版容易漏同步，导致游戏内显示的版本号滞后）
- `AGENTS.md` 瘦身为纯行为规则；构建命令、项目结构、游戏状态模型、资源路径等开发参考移入 Coding-standards，该文档升级为工程手册（中英同步）
- 新增合并上游规范：自行解决与母仓库 master 的合并冲突并完整接入新特性；双方实现方式冲突时先向用户提问定夺

## v4.21.5.3（build 1244）

- 修复 CI 测试失败：13 行翻译模板缺末尾空格（TranslationTests 挂掉）；测试模组 testMOD / testMapScript 此前被 .gitignore 忽略，导致 CI 上 Lua 脚本与合并动作测试大量失败，现已纳入版本库并补充 testMOD/jsons/Buildings.json
- Deploy 工作流不再向 Discord 推送发布通知（GitHub Release 上传保留）

## v4.21.5.2（build 1243）

- 官方文档站上线：全新 VitePress 文档站（替换 mkdocs），支持中文全文搜索、中英文一键切换、unique 列表一键复制
  - 英文区完整复用上游文档；中文区为完整翻译镜像（Modders / Developers / Translating / Other 等全部页面）
  - 新增 UncivCN 专区（分支介绍 / 新特性 / 更新日志 / 差异对照 / 代码规范 / 轮询联机）与社区内容区（原版攻略 / 模组专区 / 源码分析 / 上游更新日志），均提供中英双语
- unique 说明文档中文化：新增 docDescriptionZh 机制，模组制作者可在文档站查看中文 unique 说明
- 构建与发布流程适配：APK 签名本地化（zipalign + apksigner V3）、产物命名 UncivCN；新增签名回归 CI

## v4.21.5.1（build 1242）


- 合并上游 4.21.5（AI 金币/战争逻辑修复、CPU 性能优化、城市邦开局优化等，详见上游 [changelog.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/changelog.md)）
- 撤销自带 UCCC 模组改动，游戏本体不再打包任何模组
- 新增新建游戏设置导出/导入剪贴板功能
- 调整人口锁地按钮逻辑
- 修复统计显示级联 bug，与上游 master 对齐

## v4.21.0.2（2026.8.1）

- 修复 UCCC 模组部分 bug

## v4.21.0.1（2026.8.1）

- 跟进 Unciv 原版近期更新（含对称/镜像地图、统计面板等上游合入功能）
- 由于与上游对胜利面板均作类似功能改动，回退「禁用文明积分面板时同步禁用实验性统计面板」功能，以上游为准
- 补全缺失的中文本地化
- 联机默认服务器改为 `http://sp.unciv.cn:30123`
- 新增人口自动锁定快捷按钮（城市屏幕右下角「自动锁定」，永久切换不锁/锁人口）
- 自带 UCCC 模组
- 随机数可通过读档改变功能联机时不再可用

## v4.20.17.2（2026.7.12）

- 新增单位钉、地图钉功能（详见 [新特性](./Features#单位钉-地图钉-4-20-17-2)）
- 取消旋转对称地图对半径的奇偶限制
- 禁用文明积分面板时同步禁用实验性统计面板
- 实现游戏版本兼容性隔离（旧版无法加载新版存档）
- 训练移民等食物→产能单位时避免市民饿死

## v4.20.17.1（2026.7.4）

- 跟进 Unciv 原版一个多月的更新

## v4.20.8.4（2026.5.26）

- 原生模组检查器可筛查模组的 Lua 错误
- 轮询联机功能可查看其他玩家的在线状态

## v4.20.8.3（2026.5.23）

- 增强 Lua 系统
- event 可挂载 Lua

## v4.20.8.2（2026.5.22）

- 模组支持 [Lua 脚本](/zh/Modders/Lua-Modding)
- 修复「众神与国王」精英教育政策不送伟人的 bug
- Amount 参数兼容 Countables，新增多个 Countables 参数类型
- 修复 TRY_INJECT 单位时将造价覆盖为 0 的 bug

## v4.20.8.1（2026.5.21）

- 重磅更新！新增 [轮询联机功能](./Polling-multiplayer)

## v4.20.7.4（2026.5.19）

- 中心对称地图现在让资源数量也对称分布

## v4.20.7.3（2026.5.19）

- 扩展模组 JSON 系统，详见 [MergeAction 教程](/zh/Modders/Mod-file-structure/6-MergeActions)

---

## v4.20.7.2

**发布日期**：2026.5.19

- 安卓端安装包不再自带模组 CoeHarMod/和合共生
- 新增三种镜像地图模式，按两瓣/三瓣/六瓣中心对称分布


## v4.20.6.3

**发布日期**：2026.5.17

- 新增模组特性：

| 英文原文                                       | 中文释义                  |
| ------------------------------------------ | --------------------- |
| `Hidden from city screen`                  | 不再显示在城市面板中            |
| `Can be built [amount] times in each city` | 可以在单个城市中重复建造[amount]次 |


## v4.20.6.1

**发布日期**：2026.5.16

- 同步两个月来 Unciv 原版的更新内容
- 将前几个版本对随机性结果可变性的改动收束到设置页面，可选择是否启用


## v4.19.16.2

**发布日期**：2026.3.2

- 远古遗迹产出随机性结果可以通过读档重载改变


## v4.19.16.1

**发布日期**：2026.3.1

- 城邦任务随机性结果可以通过读档重载改变


## v4.19.15-cn2

**发布日期**：2026.2.28

- 新增模组特性：

| 英文原文                                                                                                          | 中文释义                                                       |
| ------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| `Attacks also target [mapUnitFilter] units within [positiveAmount] tiles`                                     | 攻击也会同时攻击 [positiveAmount] 格内满足 [mapUnitFilter] 的单位         |
| `Attacks also target [mapUnitFilter] units within [positiveAmount] tiles, with damage decreasing by distance` | 攻击也会同时攻击 [positiveAmount] 格内满足 [mapUnitFilter] 的单位，伤害随距离递减 |
| `Takes [relativeAmount]% damage from own area attacks`                                                        | 受到自身范围攻击时，仅承受 [relativeAmount]% 的伤害                        |
| `Takes [relativeAmount]% counter damage from each unit hit by its area attacks`                               | 其范围攻击每击中一个单位，自身就会承受该单位 [relativeAmount]% 的反击伤害             |

- 添加统计面板数据导出为 csv 表格的功能


## v4.19.15

**发布日期**：2026.2.26

- 为安卓端安装包加入内置模组 [CoeHarMod/和合共生](https://github.com/AutumnPizazz/CoeHarMod)
- 允许相邻城市切换地块，需要在模组的 ModOptions.json 文件中声明 uniques "Allow cities to claim tiles"


