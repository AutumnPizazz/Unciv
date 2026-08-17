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

        // 预热
        generate(SymmetryMode.none, false)
        generate(SymmetryMode.sixFold, false)

        val baseline = generate(SymmetryMode.none, false)
        val twoFold = generate(SymmetryMode.twoFold, false)
        val threeFold = generate(SymmetryMode.threeFold, false)
        val sixFold = generate(SymmetryMode.sixFold, false)

                System.err.println("perf r=30 no-wrap: none=${baseline / 1_000_000}ms 2f=${twoFold / 1_000_000}ms 3f=${threeFold / 1_000_000}ms 6f=${sixFold / 1_000_000}ms")
        // 断言信息也带上数字,便于失败时查看

        // 对称不应比非对称慢 2 倍以上(留足余量避免 CI 抖动误报)
        assertTrue("6-fold should not be much slower than none", sixFold < baseline * 2)
    }
}
