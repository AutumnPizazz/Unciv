# 构建与测试 Android 版本

本文仍在完善中——欢迎贡献。其中大部分信息并非 Unciv 特有，可公开获取。

## 运行配置

成功的 Gradle sync 应该会自动创建 "android" 的运行配置。
如果没有，你可以尝试自己创建一个（不过如果配置缺失是因为 Gradle sync 不完整，那你很可能根本无法选择模块）：

- 在 Android Studio 中，Run > Edit configurations（务必先让 Gradle sync 成功完成）。
    -   点击 "+" 添加新配置
    -   选择 "Android App"
    -   给配置起个名字，我们建议 "Android"
    -   模块设为 `Unciv.android.main`
    -   在 Miscellaneous 标签页，我们建议勾选两个 logcat 选项
    -   在 Debugger 标签页，我们建议勾选 `Automatically attach on Debug.waitForDebugger()`
    -   就这样，其余保持默认即可。

## 实体设备

在实体设备上调试其实最容易。
Studio 运行时会启动 adb，任何新接入且允许调试的设备都会要求确认你电脑的指纹（本教程用 USB 线，IP 是另一回事）。
一旦 adb 能看到设备、设备也授权了你的电脑，它就会出现在 "android" 运行配置右侧的设备下拉框中并被预选，然后你就可以像桌面版一样开始调试了。
**注意**：调试会话不会因为你从 Unciv 菜单选择 Exit 而结束——把它从最近任务列表划掉才能结束调试会话。不建议在 Studio 里点停止按钮。这是 Android 的特性。

### Linux 上的 `adb`

要在 Linux 上用 USB 调试，可能需要准备权限。具体要求取决于发行版——不确定就搜索在线资料。
以 Mint 22.1 为例步骤如下，其他发行版应该很容易对应：
- 确保你是 `plugdev` 组的成员（bash：`groups`）。Mint 默认就是，如果不是就添加自己（`sudo usermod -a -G plugdev $USER`）。
- 确保存在匹配的 udev 规则。这取决于你要连接的设备厂商。要查厂商 ID，在设备连接时运行：`lsusb`。
  找到你的设备，记下 "ID" 后面第一个 4 位十六进制代码。很多情况下是 `18d1`——例如 LineageOS 设备大多会伪装成 18d1，即使硬件不同。
- 查看 `/etc/udev/rules.d/` 里已有的规则。典型文件名是 `51-android.rules`。这些是文本文件且都会被处理，所以根据名字猜，不确定就全部检查。
- 编辑或创建规则文件。在 Mint 22+ 上可以用 sudo 运行 xed，否则可以选 nano 等其他编辑器。`sudo xed /etc/udev/rules.d/51-android.rules`
- 典型情况下文件只有一行：`SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"`。对，把那个 `18d1` 换成你的厂商 ID。如果文件里已有其他厂商 ID 的行，就再加一行。
- 保存文件~然后重启。~开个玩笑——我们是 Linux，所以 `sudo udevadm control --reload-rules && sudo udevadm trigger` 就够了。
- 试试 `adb kill-server && adb devices`：你的设备应该出现在列表里，Studio 也能与它通信了。
- 你可能需要从设备上重新授权你的电脑。确保没有通知栏或其他系统 UI 挡住授权弹窗。
- 如果还有问题，确保没有旧权限下残留的 gradle daemon：`./gradlew --status`。如果有，杀掉它们：`./gradlew --stop`（或直接跳过 status）。

## 构建 APK

Android Studio 有菜单 "Build -> Build Bundle(s) / APK(s) -> Build APK(s)。"
这会构建一个可直接安装的 APK，完成后会弹消息询问是否在文件管理器中显示该文件。
***重要***：这种本地构建的 APK 是 debug 签名，与商店下载的 Unciv 不通用。你不能用其中一个更新另一个，也不能不卸载就切换——否则会丢失所有数据。
命令行等价操作是：`./gradlew android:assembleDebug`。然后到 `android/build/outputs/apk/debug` 自己找 APK。

## 虚拟设备（AVD）

（待补充）
- 安装 Emulator
- Intel HAXM：Intel 已弃用但仍推荐
- 下载系统镜像
    - 选择：与宿主机架构匹配、不含 Google、旧版更快……？
- 配置 AVD
- 在 AVD 上调试

## Unciv 的日志输出

Unciv 的日志系统运行在 Android SDK 日志系统之上，在传给系统之前会过滤并打标签。

与桌面版一样，它有 'release' 模式，会丢弃所有来自 Unciv 代码的日志。
当实际 APK 的 manifest 说 debuggable=false 时即判定为 release——这里讨论的所有可能性就都是 debug 构建。
从 Studio 运行时，用 Run 还是 Debug 按钮都一样——两者都部署 debug 构建，区别只在于是否立即附加调试器。
从 Studio 构建的 APK 也永远是 debug 构建。

因此，除非你运行商店版本，日志总是开启的。
你可以通过提供 intent extra 覆盖：在运行配置的 "General" 标签页的 "Launch Flags" 字段加上：`--ez debugLogging false`。
不用 Studio 也可以借助 activity manager 控制：
```
adb shell am start com.unciv.app/com.unciv.app.AndroidLauncher --ez debugLogging true
```
（或在设备终端直接 `am start...`）可以为 release（商店）构建打开日志。

通过给 Java 虚拟机提供 `-D` 选项实现的日志过滤功能，据我们所知在 Android 上无法控制。
（待补充——在桌面/Studio 的 wiki 文章里记录这些）

## 读取 logcat

（待补充）
- Studio
    - 如果 logcat 窗口不见了：View - Tool Windows - Logcat
- Studio 的过滤
    - 调试 Unciv 时，Logcat 窗口会预置匹配的过滤器，但该工具实际上能显示整个系统日志，包括其他应用的。
    - 用 `package:com.unciv.app tag:Unciv` 作为过滤器，可以只看 Unciv 自己日志系统的输出。
- 设备上的 logcat 应用
    - `com.pluscubed.matloglibre`？已过时。
- logcat 应用需要 root 或特定授权
    - `adb shell pm grant <logcat app's package id> android.permission.READ_LOGS`
