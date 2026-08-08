# Unique 替换流程

我们经常想改写某个 Unique，或给它加一个扩展用途的参数。

但我们得给模组制作者留出时间，让他们把旧 unique 换成新的。
这意味着会有一段重叠期——通常约一个"完整小版本"，即 20 个版本，大约 2.5 个月。
在此期间，新旧 unique 都必须能工作；过了这段时间，我们就应该能找到旧 unique 并妥善地消灭它。

流程如下：

- 把旧 unique 重命名为 `"<old-unique-name>Old"`（包括 uniques 内的引用，shift+f6）
- 在旧 unique 正上方创建新 unique，使用旧名字
- 凡是使用旧 unique 的地方**加上新 unique**，而不是替换
- 给旧 unique 加 @Deprecated 注解，带替换文本——这能让模组制作者自动把旧 unique 替换成新的
  - 弃用级别必须是 `DeprecationLevel.WARNING`——或者干脆不写级别，因为默认就是这个

时间过去之后，我们要：

- 把带弃用注解的旧 unique 移到 "deprecated and removed" 区块的最顶部，按时间倒序排列
- 从代码中删除旧 unique 的所有使用
- 把弃用级别改为 ERROR（这样我们就无法再引入旧用法）
