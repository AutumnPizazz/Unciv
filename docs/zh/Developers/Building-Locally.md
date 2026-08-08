# 本地构建

读完本指南，你将能够从代码本地运行 Unciv，从而做出修改并在本地测试。

## 使用 Android Studio

-   安装 [Android Studio](https://developer.android.com/studio) - 免费且超棒！注意下载量很大！
-   安装 Git，这是我们协作的方式。UI 可选，Android Studio 内置的 Git 工具就很好用 :)
-   获取代码
    -   创建一个 [Github 账户](https://github.com/join)，如果还没有的话
    -   [Fork 仓库](https://github.com/yairm210/Unciv/fork) - 这会在你的账户下创建一份代码"副本"，位于 `https://github.com/<你的用户名>/Unciv`
-   在 Android Studio 中加载项目
    - File -> New -> Project from Version Control -> GitHub
    - 输入你的 GitHub 用户名和密码
    - 选择仓库并点击克隆 - GitHub 仓库会作为新项目创建在 Android Studio 中。
-   Gradle 会尝试初始同步。如果你是第一次用 Android Studio，可能需要接受 Android Build-tools 许可证，不同设备上操作方式不同，请搜索你所用系统的解决方案。
    -   新安装可能无法完成初始同步 - 错误形式是 `Unable to find method ''void org.apache.commons.compress.archivers.zip.ZipFile.<init>(java.nio.channels.SeekableByteChannel)''`。如果遇到这个问题，进入 File > Settings > Languages & Frameworks > Android SDK
        - 点击 "SDK Platforms"
        - 点击 "Android 16.0 ("Baklava")"
          ![image](../../assets/Android_SDK_Platforms.png)
          （可选：勾选 'Show Package Details' 只选 Platform SDK，不带 Sources 或系统镜像，可以省点空间）
        - 点击 "SDK Tools"
        - 在右下角选择 "Show Package Details"
        - 在 "Android SDK Build-Tools" 下选择版本 35.0.0
          ![image](../../assets/Android_SDK_Tools.png)
        - 点击 "Apply"
        - 重启 Android Studio
-   耐心等待初始 Gradle 同步完成。之后的同步不会这么慢。留意状态栏或 "Build" 工具窗格。
-   如果一切顺利，你现在会有三个 "运行配置"（看顶栏绿色的）："android"、"Desktop" 和 "Run unit tests"。
    -   如果 "android" 缺失，很可能是你的 Android SDK 配置没有设置 ANDROID_HOME 环境变量。设置它，重启 Studio 并重新同步 Gradle（顶栏那个大象加箭头的按钮）。
    -   如果 "Desktop" 缺失，手动创建一个：
        -   选择 Run > Edit configurations（从主菜单或配置下拉框）
        -   点击 "+" 添加新配置
        -   选择 "Application"
        -   给配置起个名字，我们建议 "Desktop"
        -   把模块类路径（Java 选择框右侧的框）设为 `Unciv.desktop.main`（Bumblebee 及以下版本为 `Unciv.desktop`），主类设为 `com.unciv.app.desktop.DesktopLauncher`，工作目录设为 `$ProjectFileDir$/android/assets`，点 OK 关闭窗口
            - 设置一些 VM 选项可能有用——在运行配置编辑器中用 Alt-V 或通过 Modify Options 菜单激活该字段，然后加 `-Xmx4096m -Xms256m -XX:MaxMetaspaceSize=256m` 给调试中的游戏多一点内存。或者用 `-DnoLog=` 或 `-DonlyLog=` 选项控制控制台日志。详见 [Log.kt](https://github.com/yairm210/Unciv/blob/master/core/src/com/unciv/utils/Log.kt) 的注释。
            - 如果出现 `../../docs/uniques.md (No such file or directory)` 错误，说明你忘了设置工作目录！
            ![image](../../assets/Desktop_Build.png)
    -   如果 "Run unit tests" 缺失 - 找到顶层的 "tests" 文件夹，右键 -> Modify Run Configuration...
-   选择 Desktop 配置（或你起的其他名字），点击绿色箭头运行！或者用旁边的按钮——那只六条腿两只触角的小虫子——开始调试。
-   一些推荐的 Android Studio 设置：
    - 进入 Settings > Version Control > Commit > Advanced Commit，关闭 'Analyze code'
    - 同一页面，建议关闭 "Use non-modal commit interface"。这会把 "Local Changes" 和 "Console" 标签放回 Git 工具窗格（以及做 shelf 后的 Shelf）。这些可能很难找——如果觉得不需要可以忽略。
    - Settings > Editor > Code Style > Kotlin > Tabs and Indents > Continuation Indent: 4
      ![image](https://user-images.githubusercontent.com/44038014/169315352-9ba0c4cf-307c-44d1-b3bc-2a58752c6854.png)
    - Settings > Editor > General > On Save > 取消勾选 Remove trailing spaces on: [...] 防止它移除 template.properties 翻译文件中必要的尾随空格
      ![image](https://user-images.githubusercontent.com/44038014/169316243-07e36b8e-4c9e-44c4-941c-47e634c68b4c.png)
      > 重要：取消勾选会产生烦人的合并冲突。提交（非翻译文件）前移除那些尾随空格。更多信息见[代码标准](https://yairm210.github.io/Unciv/Developers/Coding-standards/)。（UncivCN 分支文档站为 VitePress，中文版见 [/zh/Developers/Coding-standards](/zh/Developers/Coding-standards)。）
    - 右键点击 `android/assets/SaveFiles` 文件夹（一旦你有了它），"Mark directory as" > Excluded
      - 如果下载了模组，对 `android/assets/mods` 文件夹以及你创建的不属于公共项目的任何其他文件也这样做。
      - 这会[禁用索引](https://www.jetbrains.com/help/idea/indexing.html#exclude)以提升性能。

Unciv 用 Gradle 来指定依赖和运行方式。在后台，Gradle 小精灵们会去取包（一次性工作），完成后就会构建项目！

Unciv 使用 Gradle 8.11.1 和 Android Gradle Plugin 8.9.1。可以在 File > Project Structure > Project 中查看。

> 注意：高级构建命令（如下一段所述），特别是 `gradlew desktop:dist` 构建 jar，在 Android Studio 的终端（Alt+F12）里运行完全没有问题，大部分依赖已经就绪。

## 不用 Android Studio

- 确保已安装 JDK 11 或更高版本
- 克隆项目（见上面的初始步骤）
- 在 Unciv 文件夹打开终端，运行以下命令

### Windows (CMD)

-   运行：`gradlew desktop:run`
-   构建：`gradlew desktop:dist`

### Linux / macOS / Windows (PowerShell)

-   运行：`./gradlew desktop:run`
-   构建：`./gradlew desktop:dist`

如果 Mac/Linux 上终端返回 `Permission denied` 或 `Command not found`，先运行 `chmod +x ./gradlew`。*这是一次性操作。*

如果报错找不到 Android SDK 文件夹，运行下面的命令安装：

`sudo apt update && sudo apt install android-sdk`（Debian、Ubuntu、Mint 等）

然后，在 `local.properties` 文件中设置 SDK 位置，添加：

`sdk.dir = /path/to/android/sdk` - 例如 `/usr/lib/android-sdk`

如果启动时报 JDK 版本错误，从[这里](https://adoptium.net/temurin/releases/)安装 JDK。

> 注意：Gradle 可能需要几分钟下载文件
构建完成后，输出的 .JAR 文件应该在 `/desktop/build/libs/Unciv.jar`

要进行实际开发，你可能需要下载 Android Studio 自己构建——见上文 :)

## 在 Android 上调试

有时桌面版调试不够，你需要在 Android 设备上调试 Unciv。
介绍见 [测试 Android 构建](Testing-Android-Builds.md)。

## 下一步

恭喜！Unciv 现在应该已经在你的电脑上运行了！现在我们可以开始改代码了，之后再看你的改动如何进入主仓库！

现在是个好时机去了解[项目结构总览！](Project-structure-and-major-classes.md)

### 单元测试

你可以（有些情况下*应该*）在本地运行甚至调试单元测试。

-   仓库包含一个 "Run unit tests" 运行配置。如果缺失：
-   在 Android Studio 中，Run > Edit configurations。
    -   点击 "+" 添加新配置
    -   选择 "Gradle" 并命名为 "Run unit tests"
    -   在 "Gradle Project" 下从下拉框选择 "Unciv"（或直接输入），把 "Tasks" 设为 `:tests:test`，"Arguments" 设为 `--tests "com.unciv.*"`，点 OK 关闭窗口。
-   选择 "Run unit tests" 配置，点击绿色箭头运行！或者像上面那样开始调试会话。

### Lint 检查

Detekt 检查代码异味和其他 lint 问题。
要生成 Detekt 报告：

- 下载 [detekt-cli](https://github.com/detekt/detekt/releases/latest)（zip 文件）并解压
- 在 Unciv 根目录打开终端，运行以下命令之一生成报告。注意：Windows 下把 `detekt-cli` 换成 `detekt-cli.bat`。
    - 警告：`PATH/TO/DETEKT/detekt-cli --parallel --report html:detekt/reports.html --config .github/workflows/detekt_config/detekt-warnings.yml`
    - 错误：`PATH/TO/DETEKT/detekt-cli --parallel --report html:detekt/reports.html --config .github/workflows/detekt_config/detekt-errors.yml`
- 报告生成在 `detekt/reports.html`

### 测试文档站改动

这个文档站是从[主仓库的 'docs' 文件夹](https://github.com/yairm210/Unciv/tree/master/docs)自动构建的，
所以要改进它，你通常要在本地 Unciv 克隆的分支上处理那些文件。

然而，典型的编辑器预览并不能作为权威，因为文件要经过构建流程处理。
在 UncivCN 分支，文档站使用 VitePress（英文区复用 `docs/`，中文区在 `docs/zh/`）。
要彻底检查，请在本地运行构建并在浏览器中查看结果：

- 一键构建 + 预览：双击 `docs-vitepress/build.bat`（Windows）
- 或手动执行：
  - `./gradlew desktop:generateDocs`（生成 uniques 等 Kotlin 文档）
  - 在 `docs-vitepress/` 下 `npm ci && npm run docs:build`
  - 产物在 `docs-vitepress/.vitepress/dist`，可用 `npm run docs:preview` 预览（地址 `http://localhost:4173/Unciv/`）

### 清理过时文件

时不时地，Unciv 会升级主要工具版本——主要是 Gradle、Android SDK Platform 和 Android SDK Build-Tools。
新版本和支持文件会自动下载，但旧版本不会自动清理，中间构建文件也不会。
这可能会在你的系统上留下几 GB 的死文件。如果它们烦到你了，可以这样清理：

-   从 SDK manager 移除过时的 Android SDK Platform 和 Build-Tools 版本（记住所有项目共享这些，所以如果还有其他项目，保留它们的要求）。
-   关闭 Android Studio 后（Windows 上可能还得手动结束残留的 Gradle daemon）：
    -   从 Unciv/.gradle、~/.gradle/caches（Windows 上为 %HOME%\.gradle\caches）和 ~/.gradle/daemon 删除以过时 Gradle 版本命名的子文件夹
    -   要彻底但更昂贵的清理，把 ~/.gradle/caches 整个清掉，只保留标记文件。
      这会强制下一次 gradle 同步重新下载大量支持文件，但这样也能清掉对已淘汰的 kotlin、Gdx 等支持库的残留。

另外，git 把"你的改动安全"置于效率之上，到了极端的程度，导致一些膨胀。
 `git gc` 会自动做，但很克制，手动运行也无妨。
更彻底的做法是偶尔在 Studio 的终端（或 Unicv 项目文件夹内的任何 shell）运行 `git gc --prune=now --aggressive`，
但要先确保清理掉所有过时的分支，且剩余分支与它们对应的远程分支同步（如果只有本地分支，则基于 master）。

### UncivServer

源码中自带的简易多人服务器主机可以像游戏本体一样调试或运行：
-   在 Android Studio 中，Run > Edit configurations。
    -   点击 "+" 添加新配置
    -   选择 "Application" 并命名为 "UncivServer"
    -   模块设为 `Unciv.server.main`（Bumblebee 及以下版本的 Studio 为 `Unciv.server`），主类设为 `com.unciv.app.server.UncivServer`，工作目录设为 `<repo_folder>/android/assets/`，点 OK 关闭窗口。
-   选择 UncivServer 配置，点击绿色箭头运行！或者像上面那样开始调试会话。

要构建 jar 文件，参见[不用 Android Studio](#不用-android-studio)，把 'desktop' 换成 'server'。即运行 `./gradlew server:dist`，完成后到 /server/build/libs/ 找 UncivServer.jar
