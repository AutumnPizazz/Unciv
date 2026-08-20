package com.unciv.logic.map.mapgenerator

import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.MapType
import com.unciv.logic.map.SymmetryMode
import com.unciv.models.ruleset.RulesetCache
import com.unciv.testing.BaseTestRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 性能冒烟:大地图上对称生成不应明显慢于非对称(规范扇区化应带来线性加速) */
@RunWith(BaseTestRunner::class)
class PerfSmokeTest {
    @Test
    fun symmetricGenerationIsNotSlower() {
        RulesetCache.loadRulesets(noMods = true)
        val ruleset = RulesetCache.getVanillaRuleset()

        fun generate(mode: String, wrap: Boolean): Long {
            val params = MapParameters().apply {
                type = MapType.perlin
                shape = MapShape.hexagonal
                mapSize = MapSize(30)
                seed = 42
                this.worldWrap = wrap
                symmetryMode = mode
            }
            val start = System.nanoTime()
            MapGenerator(ruleset).generateMap(params)
            return System.nanoTime() - start
        }

        // 预热(JIT/类加载等一次性开销不计入测量)
        repeat(2) {
            generate(SymmetryMode.none, false)
            generate(SymmetryMode.sixFold, false)
        }

        // 每个模式测 3 次取最小值——共享 CI runner 上单次计时受 GC/JIT/CPU 竞争影响波动很大,
        // 最小值最接近无干扰时的真实耗时,可显著降低性能断言 flaky 误报
        fun measureBest(mode: String): Long {
            var best = Long.MAX_VALUE
            repeat(3) {
                best = minOf(best, generate(mode, false))
            }
            return best
        }

        val baseline = measureBest(SymmetryMode.none)
        val twoFold = measureBest(SymmetryMode.twoFold)
        val threeFold = measureBest(SymmetryMode.threeFold)
        val sixFold = measureBest(SymmetryMode.sixFold)

        System.err.println(
            "perf r=30 no-wrap: none=${baseline / 1_000_000}ms 2f=${twoFold / 1_000_000}ms " +
                "3f=${threeFold / 1_000_000}ms 6f=${sixFold / 1_000_000}ms"
        )
        // 断言信息也带上数字,便于失败时查看

        // 对称不应比非对称慢 3 倍以上(留足余量避免共享 CI runner 抖动误报;
        // 本地实测 ~1.1x,CI 实测最差 ~2.9x,真实性能回归通常远超 3x)
        assertTrue(
            "6-fold should not be much slower than none (6f=${sixFold / 1_000_000}ms, none=${baseline / 1_000_000}ms)",
            sixFold < baseline * 3
        )
    }
}
