# 在 MacOS 上安装

目前有几种在 MacOS 上安装 Unciv 的方式。

建议不要从源码安装，因为最终结果是一样的。

## 使用 MacPorts 安装

详情见[这里](https://ports.macports.org/port/unciv/)——只需在命令行运行 `sudo port install unciv` 即可！

不需要预装 JDK。

## 使用 JAR 安装

1. 如果你的 Mac 上还没有安装 Java 8 或 OpenJDK（11 和 18 版本也能用），要么
  * 从[官方网站](https://java.com/en/download/)下载。下载完成后打开它，按屏幕提示操作。
  * 如果你用 [Homebrew](https://brew.sh/)，直接运行 `brew install java`
2. 装好 Java 后，就可以下载最新的 Unciv JAR 了。可以从 Github 的 [releases](https://github.com/yairm210/Unciv/releases) 页面下载名为 *Unciv.jar* 的文件。
3. 要运行游戏，需要在终端运行 `java -jar Unciv.jar`。
4. 或者，你可以创建一个包含该命令的 'Unciv.sh' 文件，然后运行这个新文件，这样可以创建快捷方式等。

_（遗憾的是，用这种方法在 MacOS 上安装时 Unciv 不会自动更新，所以每次想更新游戏都得从 Github 下载最新的 Unciv.jar。）_

## 从源码安装

如何从源码安装 Unciv 请参见[不使用 Android Studio 进行本地构建](../Developers/Building-Locally.md)。不建议使用这种方法，因为它和第一种方法结果相同，却要复杂得多，而且过程中容易出错。

_（遗憾的是，用这种方法在 MacOS 上安装时 Unciv 不会自动更新，所以每次想更新游戏都得重复这些步骤。）_
