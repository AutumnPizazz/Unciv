---
title: UncivCN 更新日志
---

# UncivCN 更新日志

版本号规则：上游版本 + CN 子版本号（`.1`、`.2`、`.3`…，同一上游版本可发多个 CN 子版本，如 4.20.8.1 → 4.20.8.4；跟进新上游后从 `.1` 重新开始，如 4.21.5 → 4.21.5.1）。

::: tip 上游更新日志
上游（原版）版本记录见 [Unciv 官方更新日志](https://github.com/yairm210/Unciv/blob/master/changelog.md)（GitHub）。
:::

## 未发布

- 修复：更新检查现会确认国内下载服务器已同步
- 多人联机：同步回合现会在所有玩家提交后推进
- 多人联机：同步回合提交现使用独立的原子操作存储

## 4.21.10.3（build 1258）

- 多人联机：新增实验性的同步回合操作协议
- 多人联机：同步回合操作按玩家与序号去重
- 多人联机：同步回合重放单位操作时不会重新随机战斗
- 多人联机：同步回合支持驻防、休眠与跳过单位操作
- 多人联机：同步回合结算现使用服务端原子锁
- 多人联机：同步回合快照现覆盖外交弹窗、事件与特殊单位界面
- 多人联机：自动化、Lua 与触发单位效果现按结果快照重放
- 多人联机：同步回合现会记录城市生产、科技与政策选择
- 多人联机：同步回合现可重放复杂单位操作的模组状态变更
- 模组支持：单位级变量，可为每个单位附加数值计数器（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组支持：单位经验改为变量实现，模组可读写，旧存档自动兼容（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组支持：变量支持城市、文明、全局作用域（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组支持：变量可设上下限、每回合产出、百分比加成（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组支持：变量可购买、可生产转换、可显示在界面（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组注意：文明级变量需显式声明作用域，旧模组需更新
- 修复：变量购买按原值扣、读档后保留、AI 不崩溃
- 模组支持：变量可用于所有数量条件与设定触发（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 合并上游：修复被掠夺道路无法修复、打开百科卡死
- 合并上游：CN 特色功能适配上游重构，行为不变
- 文档站：CoeHarMod 更新日志移至模组板块（详见 [changelog](/Community/Mods/CoeHarMod/changelog/)）
- 文档站：上游更新日志改为顶部链接查看
- 文档站：更新日志条目限 30 字内，重要改动可拆分多条

## 4.21.10.2（build 1257）

- 更新：更新检查与下载按地区分流，大陆玩家走 CN 服务器（详见 [server-ts/README.md](https://github.com/blyrin/unciv-srv/blob/main/README.md)）
- 更新：首次启动询问所在地区，可在「选项 - 高级」修改
- 合并上游：修复观战与城市操作的多处崩溃

## 4.21.10.1（build 1256）

- 模组支持：新增文明级全局变量，可配条件、触发与 Lua（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组支持：变量显示位置可配置（顶栏与资源概览）（详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)）
- 模组支持：条件类 `[amount]`（如 `when above [amount] ...`、血量/移动力/人口阈值）现支持 Countable 表达式（如 `[Cities]`），详见 [Unique parameters](/zh/Modders/Unique-parameters#countable)
- 合并上游：多项界面与崩溃修复、晋升通知合并
- 修复：环形与镜像地图的对称生成异常（详见 [分支特色](/zh/UncivCN/Features)）

## 4.21.8.3（build 1255）

- 联机：新游戏选项「禁止读档」，退出重进无法重来
- 模组支持：新增产能溢出即时转移 unique（详见 [uniques](/zh/Modders/uniques)）

## 4.21.8.2（build 1254）

- 修复：地图钉预览不再提前显示未揭示的资源
- 模组支持：文明6式单位维护费（unique 启用），维护费可见于百科（详见 [Units.json](/zh/Modders/Mod-file-structure/4-Unit-related-JSON-files) 与 [uniques](/zh/Modders/uniques)）
- 模组支持：数量类参数全面支持 Countable 表达式（详见 [Unique parameters](/zh/Modders/Unique-parameters)）
- 模组支持：新增战斗触发、驻防登船等 unique（详见 [uniques](/zh/Modders/uniques)）
- 模组/Lua：Lua 可改地块产出、接管战斗公式（详见 [Lua Modding](/zh/Modders/Lua-Modding)）
- 模组支持：ModOptions.json 新增版本字段（`recommendedGameVersion`、依赖的 `recommendedVersion`），游戏版本不符时给出非阻塞警告，详见 [ModOptions.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files)
- 模组支持：修复 REMOVE_FIELD 合并对 float 字段的崩溃，详见 [MergeActions](/zh/Modders/Mod-file-structure/6-MergeActions)

## 4.21.8.1（build 1253）

- 合并上游：修复多处崩溃，新增「已探索地块」排名
- 难度：国王及以上难度的 AI 不满修正改为 100/90/85/75（原 90/85/75/60）
- 模组支持：单位血量上限可模组化（详见 [Units.json](/zh/Modders/Mod-file-structure/4-Unit-related-JSON-files)）
- 主菜单按钮：Discord 换 QQ 群，新增贴吧入口
- 文档站链接按客户端语言自适应（中文客户端跳 /zh/ 区）
- 游戏内 wiki 链接改为 CN 文档站
- 全项目代码清理与审查修复（删除死代码、修复城市/过滤器等 bug）

## 4.21.7.2（build 1252）

- Android：更新弹窗内直接下载并安装 APK
- Android：下载支持断点续传与一键重装
- 主菜单「下载最新版本」改为列出当前平台安装包并直接下载所选包

## 4.21.7.1（build 1251）

- 合并上游：AI 战争逻辑、轰炸机闪避等多项修复
- Lua API：新增 `unit.getEraNumber()`、`civ.discoverTech()`，详见 [Lua API 参考](/zh/Modders/Lua-API-Reference)
- 模组：CoeHarMod 改为独立仓库的 git 子模块，规则经 Lua 精简

## 4.21.6.6（build 1250）

- 主菜单自动更新检测（镜像源回退）；「模组下载源」更名为「下载源」
- 修复自定义 Lua API 函数参数含函数调用表达式时的运行时崩溃
- Lua API 大扩展（约 100 个新方法）、`ConditionalLuaCheck` unique、`TriggerUponTradeMade` 钩子，详见 [Lua Modding](/zh/Modders/Lua-Modding)
- Lua 地图脚本：模组可在 `scripts/` 提供地图生成器，新增「Lua Generated」地图类型
- Lua 沙箱加固；模组检查器覆盖地图脚本 API

## 4.21.6.5（build 1249）

- Lua 错误弹窗带行号；堵住 `package.loaded` 沙箱逃逸；指令预算阻止死循环卡死
- 新增：联机重开投票（房主开启、全员投票、超时默认同意、自动重开）

## 4.21.6.4（build 1248）

- 新增「模组下载源」设置，支持国内镜像加速
- 开图界面：配置可保存到命名槽位

## 4.21.6.3（build 1247）

- 修复 Docker 构建复制旧名 `Unciv.jar`；修复 Release 缺少 MSI 安装包

## 4.21.6.2（build 1246）

- 补齐 CN 自研功能缺失的简体中文翻译（47 个缺失 + 10 个空词条）

## 4.21.6.1（build 1245）

- 笔记改为按 gameId 存储，不再跨存档泄漏
- 地图钉/单位钉：长按/Alt+点击编辑、备注气泡、崩溃修复、中文词条补齐
- 修复开图界面自定义尺寸重复行；内置规则集与上游对齐
- 合并上游 4.21.6（CPU 性能、AI 工人相邻加成、多人上传失败提示）

## 4.21.5.3（build 1244）

- 修复 CI 测试失败（翻译模板缺空格、测试模组纳入版本库）；部署通知清理

## 4.21.5.2（build 1243）

- 官方 VitePress 文档站上线（中文全文搜索、中英一键切换）
- unique 说明文档中文化
- APK 签名本地化，产物统一命名 UncivCN

## 4.21.5.1（build 1242）

- 合并上游 4.21.5（AI 金币/战争逻辑修复、CPU 性能优化）
- 撤销自带 UCCC 模组；开图设置可剪贴板导出

## 4.21.0.2（2026.8.1）

- 修复 UCCC 模组部分 bug

## 4.21.0.1（2026.8.1）

- 跟进上游近期更新（对称/镜像地图、统计面板）；补全中文本地化
- 联机默认服务器改为 `sp.unciv.cn:30123`；新增人口自动锁定按钮；自带 UCCC 模组
- 联机不再支持读档改变随机数

## 4.20.17.2（2026.7.12）

- 新增单位钉/地图钉；训练移民不再饿死市民

## 4.20.17.1（2026.7.4）

- 跟进上游一个多月的更新

## 4.20.8.4（2026.5.26）

- 模组检查器可筛查 Lua 错误；轮询联机显示在线状态

## 4.20.8.3（2026.5.23）

- 增强 Lua 系统；event 可挂载 Lua

## 4.20.8.2（2026.5.22）

- 模组支持 [Lua 脚本](/zh/Modders/Lua-Modding)
- 修复「众神与国王」精英教育不送伟人；修复 TRY_INJECT 造价覆盖 bug
- Amount 参数兼容 Countables

## 4.20.8.1（2026.5.21）

- 新增 [轮询联机功能](./Polling-multiplayer)

## 4.20.7.4（2026.5.19）

- 中心对称地图资源数量对称分布

## 4.20.7.3（2026.5.19）

- 扩展模组 JSON 系统，见 [MergeAction 教程](/zh/Modders/Mod-file-structure/6-MergeActions)

## 4.20.7.2

**发布日期**：2026.5.19

- 安卓端安装包不再自带 CoeHarMod 模组
- 新增三种镜像地图模式（两瓣/三瓣/六瓣中心对称）

## 4.20.6.3

**发布日期**：2026.5.17

- 新增模组特性：

| 英文原文 | 中文释义 |
| --- | --- |
| `Hidden from city screen` | 不再显示在城市面板中 |
| `Can be built [amount] times in each city` | 可以在单个城市中重复建造[amount]次 |

## 4.20.6.1

**发布日期**：2026.5.16

- 同步两个月来上游更新
- 随机结果可变性收束为设置开关

## 4.19.16.2

**发布日期**：2026.3.2

- 远古遗迹随机结果可通过读档重载改变

## 4.19.16.1

**发布日期**：2026.3.1

- 城邦任务随机结果可通过读档重载改变

## 4.19.15-cn2

**发布日期**：2026.2.28

- 新增范围攻击类模组特性（多目标、伤害随距离递减、自身/反击伤害）
- 统计面板数据导出 CSV

## 4.19.15

**发布日期**：2026.2.26

- 安卓端内置 [CoeHarMod/和合共生](https://github.com/AutumnPizazz/CoeHarMod)
- 相邻城市可交换地块（需在 ModOptions.json 声明 "Allow cities to claim tiles"）
