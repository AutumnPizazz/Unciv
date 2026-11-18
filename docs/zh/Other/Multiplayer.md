# 多人游戏

UncivCN 多人游戏通过同一台服务器交换存档。你可以使用默认服务器，也可以在多人游戏选项中设置兼容的服务器地址。要自建服务器，请看[架设多人游戏服务器](#架设多人游戏服务器)。

## 如何游玩

0. 确保所有人都使用同一个多人服务器（`主菜单 -> 选项 -> 多人游戏`）
0. 让所有玩家把他们的用户 ID 发给你（`主菜单 -> 多人游戏 -> 复制用户 ID`）。
   * （可选）把这些用户 ID 加入你的好友列表（`主菜单 -> 多人游戏 -> 好友列表`）。
0. 在 `主菜单 -> 开始新游戏` 中，勾选左侧的 `在线多人游戏`。在右侧添加更多人类玩家，输入你想一起玩的玩家的用户 ID。点击 `开始游戏！`。
0. 游戏 ID 会自动复制到你的剪贴板。（如果弄丢了，可以从 `主菜单 -> 多人游戏 -> 复制游戏 ID` 重新获取。）把这个游戏 ID 发给其他玩家。
0. 其他玩家需要进入 `主菜单 -> 多人游戏 -> 添加多人游戏`，输入你发给他们的游戏 ID。然后他们就可以从多人游戏界面加入游戏了。

房主开启重开投票后，仍存活的玩家可在指定回合从局内菜单发起投票。存活玩家全部投票或到达截止时间时结算；超时未投票视为同意，票数平局也通过。UncivSrv 会在玩家重连或服务端重启后保留投票状态。投票通过后按原配置创建新局。

## 架设多人游戏服务器

Go 联机服务端（UncivSrv）已闭源，由独立仓库维护。UncivCN 停止分发 Go 服务端 Docker 镜像和 GitHub Release 下载。

如需自建服务器，请从下方选用兼容实现并按其文档部署。不同服务器对同步回合、重开投票等 UncivCN 功能的支持可能不同。开局前，所有玩家应在**选项 → 多人游戏**中填写同一服务器地址，并点击**检查服务器连接**。

## 第三方（非官方）自建 Unciv 服务器软件

* [https://github.com/Mape6/Unciv_server](https://github.com/Mape6/Unciv_server)（Python）
* [https://gitlab.com/azzurite/unciv-server](https://gitlab.com/azzurite/unciv-server)（NodeJS）
* [https://github.com/oynqr/rust_unciv_server](https://github.com/oynqr/rust_unciv_server)（Rust）
* [https://github.com/touhidurrr/UncivServer.xyz](https://github.com/touhidurrr/UncivServer.xyz)（TypeScript | Bun）

## 第三方（非官方）公共 Unciv 服务器

这些服务器由社区运营，**不是**官方服务器。这些服务器可能（暂时或永久）不可用，并丢失你的游戏存档。它们还可能收集你的 IP、游戏频率或其他数据。只有在你接受这些风险并信任服务器所有者时才使用。

* [`https://uncivserver.xyz/`](https://uncivserver.xyz/) - 由 [@touhidurrr](https://github.com/touhidurrr) 在[他们的 Discord](https://discord.gg/H9em4ws8XP) 运营（[源码](https://github.com/touhidurrr/UncivServer.xyz/)）
