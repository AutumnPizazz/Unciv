# 多人游戏

Unciv 的多人游戏基于简单的存档文件上传/下载，因此默认基于免费 Dropbox 账户。不过，很多人都在用这个默认方案，所以能否稳定访问并不确定。要自建服务器，请看[架设多人游戏服务器](#架设多人游戏服务器)。

## 如何游玩

0. 确保所有人都使用同一个多人服务器（`主菜单 -> 选项 -> 多人游戏`）
0. 让所有玩家把他们的用户 ID 发给你（`主菜单 -> 多人游戏 -> 复制用户 ID`）。
   * （可选）把这些用户 ID 加入你的好友列表（`主菜单 -> 多人游戏 -> 好友列表`）。
0. 在 `主菜单 -> 开始新游戏` 中，勾选左侧的 `在线多人游戏`。在右侧添加更多人类玩家，输入你想一起玩的玩家的用户 ID。点击 `开始游戏！`。
0. 游戏 ID 会自动复制到你的剪贴板。（如果弄丢了，可以从 `主菜单 -> 多人游戏 -> 复制游戏 ID` 重新获取。）把这个游戏 ID 发给其他玩家。
0. 其他玩家需要进入 `主菜单 -> 多人游戏 -> 添加多人游戏`，输入你发给他们的游戏 ID。然后他们就可以从多人游戏界面加入游戏了。

## 架设多人游戏服务器

由于 Dropbox API 的某些限制，加上当前玩家的涌入，我们已经多次达到 Dropbox 不可用的地步。

因此，你现在可以在任何能运行 Java 程序的电脑上架设自己的 Unciv 服务器。

本指南面向对电脑软件有中等技术水平、能够上网搜索学习未知内容的人。如果你完全是新手，不投入较多时间学习的话可能跟不上。

如果你精通服务器架设，文末还有另一份指南。

### 操作步骤

开始之前，你需要安装 [Node.js 20+](https://nodejs.org/) 与 [pnpm 10+](https://pnpm.io/)，或者安装 [Docker](https://www.docker.com/)。服务器由 TypeScript 版 [UncivSrv](https://github.com/blyrin/unciv-srv) 提供，位于仓库的 `server-ts/` 子模块中。

在仓库根目录打开终端，进入子模块并启动：

```bash
cd server-ts
pnpm install
pnpm build && pnpm start
```

如果使用 Docker，则改为：

```bash
cd server-ts
docker compose up -d --build
```

你的服务器就启动了！

要检查是否一切正常，可以在同一台电脑上启动 Unciv，进入 "选项 > 多人游戏"，在 "服务器地址" 中输入 `http://localhost:11451`，点击 "检查服务器连接"。你应该会看到 "成功！" 的结果，说明一切正常！

要连接本地网络之外的其他设备，或让服务器可以从互联网访问，你需要一个公网 IP。如果你的 ISP 已经提供了公网 IP，在路由器上转发你的服务器端口（默认 11451），你的服务器就暴露到互联网上了！这种情况下你也可以使用 `http://<你的公网IP>:<你转发的端口>`。例如，如果你有 IP `203.0.113.1` 并把服务器端口转发到 `1234`，你的服务器就可以从互联网通过 `http://203.0.113.1:1234` 访问。另外，由于 `HTTP` 协议默认端口是 `80`，如果你把服务器转发到 `80` 端口，就不需要指定端口了。例如，如果你把服务器端口转发到公网 IP 的 `80` 端口，你的服务器将暴露在 `http://<你的公网IP>`，即 `http://203.0.113.1`。

在另一台设备上，输入你的服务器 URL（`http://<你的IP地址>:<你选择的端口>`），在新设备上点击 '检查连接'，如果得到同样的 "成功！" 结果——恭喜，你们连接到同一个服务器了，可以开始多人游戏了！

请注意：
* 未连接到同一服务器的设备*无法*一起参与多人游戏
* 在很多地方，你的外网 IP 会定期变化。如果是这样，你要么一直更新 IP，要么使用动态 DNS 之类的服务。
* 要从 `80` 或 `443` 之类的特殊端口启动服务器，需要管理员权限。如果要用这些端口，请以管理员身份运行 PowerShell。不过，如果你用路由器做端口转发，其实不需要这样做。你可以从 `11451` 端口启动服务器，再转发到 `80`。

### 给有架设经验的人

* 安装 Node.js 20+ 与 pnpm 10+（或使用 Docker）
* 获取 [UncivSrv](https://github.com/blyrin/unciv-srv) 源码（本仓库 `server-ts/` 子模块即为其快照）
* 用 `.env` 文件配置——可选项见 `server-ts/example.env`
    * 服务器监听地址/端口、数据库路径、注册模式（`open`/`approval`/`closed`）、聊天开关等均在 `.env` 中设置。
* 启动：`pnpm build && pnpm start`（开发时 `pnpm dev`）
    * 存储使用 SQLite，自动执行数据库迁移。
    * 玩家密码以 scrypt 哈希保存，不再是明文文件。

## 第三方（非官方）自建 Unciv 服务器软件

* [https://github.com/blyrin/unciv-srv](https://github.com/blyrin/unciv-srv)（TypeScript，本仓库 `server-ts/` 子模块；功能最全）
* [https://github.com/Mape6/Unciv_server](https://github.com/Mape6/Unciv_server)（Python）
* [https://gitlab.com/azzurite/unciv-server](https://gitlab.com/azzurite/unciv-server)（NodeJS）
* [https://github.com/oynqr/rust_unciv_server](https://github.com/oynqr/rust_unciv_server)（Rust）
* [https://github.com/touhidurrr/UncivServer.xyz](https://github.com/touhidurrr/UncivServer.xyz)（TypeScript | Bun）

## 第三方（非官方）公共 Unciv 服务器

这些服务器由社区运营，**不是**官方服务器。这些服务器可能（暂时或永久）不可用，并丢失你的游戏存档。它们还可能收集你的 IP、游戏频率或其他数据。只有在你接受这些风险并信任服务器所有者时才使用。

* [`https://uncivserver.xyz/`](https://uncivserver.xyz/) - 由 [@touhidurrr](https://github.com/touhidurrr) 在[他们的 Discord](https://discord.gg/H9em4ws8XP) 运营（[源码](https://github.com/touhidurrr/UncivServer.xyz/)）
