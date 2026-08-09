package com.unciv.models.ruleset


/**
 * 语义化版本号：数字段（`n.n.n` 或 `n.n.n.n`），可带 `-patchN` 后缀。
 *
 * `-patchN` 作为附加段参与比较：`4.21.5.3` < `4.21.5.3-patch1` < `4.21.5.3-patch2`。
 * 段数不足时补 0 参与比较（`1.2` == `1.2.0`）。
 */
class ModVersion private constructor(private val parts: IntArray) : Comparable<ModVersion> {

    override fun compareTo(other: ModVersion): Int {
        val maxLen = maxOf(parts.size, other.parts.size)
        for (i in 0 until maxLen) {
            val a = parts.getOrElse(i) { 0 }
            val b = other.parts.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    override fun toString() = parts.joinToString(".")

    override fun equals(other: Any?) = other is ModVersion && compareTo(other) == 0
    override fun hashCode() = parts.contentHashCode()

    companion object {
        private val regex = Regex("""^(\d+(?:\.\d+)*?)(?:-patch(\d+))?$""")

        /** 未声明版本时的默认值 */
        val DEFAULT = parse("0.0.1")!!

        /** 解析 `n.n.n` / `n.n.n.n` / `n.n.n.n-patchN`；空白或格式非法返回 null */
        fun parse(text: String): ModVersion? {
            if (text.isBlank()) return null
            val match = regex.matchEntire(text.trim()) ?: return null
            val numericParts = match.groupValues[1].split(".").map { it.toInt() }
            val patch = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt()
            return ModVersion(if (patch == null) numericParts.toIntArray() else (numericParts + patch).toIntArray())
        }

        /** 解析失败时回退默认版本 [DEFAULT] */
        fun parseOrDefault(text: String): ModVersion = parse(text) ?: DEFAULT
    }
}

/**
 * 版本范围：`min~max`（闭区间）。
 *
 * 支持形式：
 * - `"1.0.0~2.0.0"` 双边
 * - `"1.0.0~"` 仅下限
 * - `"~2.0.0"` 仅上限
 * - `"1.2.0"` 精确版本（min == max）
 * - 空白 = 任意版本
 *
 * [rawText] 保留声明原文，用于给模组作者的警告显示。
 */
class ModVersionRange private constructor(
    val min: ModVersion?,
    val max: ModVersion?,
    val rawText: String
) {
    fun contains(version: ModVersion): Boolean {
        if (min != null && version < min) return false
        if (max != null && version > max) return false
        return true
    }

    companion object {
        /** 任意版本（未声明范围） */
        val ANY = ModVersionRange(null, null, "")

        /**
         * 解析版本范围字符串；非法格式返回 null。
         * 空/空白返回 [ANY]。
         */
        fun parse(text: String): ModVersionRange? {
            if (text.isBlank()) return ANY
            val trimmed = text.trim()
            val tildeIndex = trimmed.indexOf('~')
            if (tildeIndex < 0) {
                // 精确版本
                val version = ModVersion.parse(trimmed) ?: return null
                return ModVersionRange(version, version, trimmed)
            }
            val minText = trimmed.substring(0, tildeIndex).trim()
            val maxText = trimmed.substring(tildeIndex + 1).trim()
            val min = if (minText.isEmpty()) null else ModVersion.parse(minText) ?: return null
            val max = if (maxText.isEmpty()) null else ModVersion.parse(maxText) ?: return null
            return ModVersionRange(min, max, trimmed)
        }
    }
}
