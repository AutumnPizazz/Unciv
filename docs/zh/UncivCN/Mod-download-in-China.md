# 国内玩家模组安装指南

> 作者：UncivCN
>
> 更新日期：以 git 提交时间为准

## 问题背景

Unciv 的模组几乎全部发布在 GitHub 上，游戏内置的模组浏览器也通过 GitHub 的接口搜索、预览和下载模组。而 GitHub 在国内的网络环境下访问不稳定：模组列表经常加载失败，下载经常中断。这导致国内玩家安装模组非常困难。

UncivCN 内置了**下载源切换**功能，专门解决这个问题。下载源不仅用于模组列表、预览与下载，游戏更新检测（自动查询最新 Release）同样走所选下载源。

## 游戏内解决方案：切换下载源

在游戏主菜单依次打开 **选项（Options）→ 高级（Advanced）**，找到 **下载源（Download source）**：

| 选项 | 说明 |
|------|------|
| GitHub (official) | 默认值，直连 GitHub，国内网络通常不稳定 |
| gh-proxy.com | 公共 GitHub 加速镜像（实测支持搜索、预览图与下载） |
| ghfast.top | 公共 GitHub 加速镜像 |
| ghproxy.net | 公共 GitHub 加速镜像 |
| Custom | 自定义加速前缀，需在下方输入框填写 |

切换后回到 **模组管理** 页面，点击刷新（重新进入页面即可），模组列表、预览图与下载都会走所选镜像。

### 自动引导

如果模组列表加载失败，游戏会检测当前使用的下载源：

- 若还是官方源，会弹出确认框询问是否**一键切换到镜像下载源（gh-proxy.com）并自动重试**，点确认即可；
- 若已经是镜像源仍失败，会提示前往 选项 → 高级 更换其它镜像。

### 自定义前缀

部分玩家可能有自己搭建或更可靠的加速服务。选择 Custom 后，在下方输入框填入加速前缀（例如 `https://gh-proxy.com/`），点击 Enter 保存。前缀的用法是把 `https://github.com/...`、`https://raw.githubusercontent.com/...` 等 GitHub 链接整体拼在加速域名之后，也就是：

```
https://你的加速域名/https://github.com/作者/模组仓库/archive/refs/heads/main.zip
```

游戏只会对 GitHub 系域名（github.com、raw.githubusercontent.com、avatars.githubusercontent.com、api.github.com 等）添加前缀，其它地址（例如 Gitee 的直接 zip 链接）不受影响。

## 游戏更新检测

进入主菜单后游戏会在后台自动查询 UncivCN 仓库的最新 Release（同样走当前下载源）。发现新版本时，主菜单底部版本号旁会显示“发现新版本”提示，点击版本号可查看新版本信息并打开下载页面。若因网络问题检测失败，会弹出确认框询问是否一键切换镜像下载源（交互与模组列表失败时一致）。

## 手动方式：粘贴加速链接

不切换下载源也可以安装模组。在 模组管理 → 从网络地址获取模组 中，直接粘贴加速后的 zip 链接，例如：

```
https://gh-proxy.com/https://github.com/作者/模组仓库/archive/refs/heads/main.zip
```

Unciv 支持从任意网址直接下载 zip 并解压安装，Gitee 等其它平台的仓库链接同样适用。

::: warning 注意
公共加速镜像均为公益服务，可能限速、失效或更换域名。若内置镜像失效，请先尝试其它内置镜像，或自行搜索当前可用的 GitHub 加速服务填入自定义前缀。
:::
