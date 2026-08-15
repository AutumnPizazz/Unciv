package com.unciv.models.ruleset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModVersionTests {

    //region ModVersion parsing
    @Test
    fun `parse accepts n-dot-separated versions`() {
        assertEquals(listOf(1, 2, 3), ModVersion.parse("1.2.3")!!.parts())
        assertEquals(listOf(4, 21, 5, 3), ModVersion.parse("4.21.5.3")!!.parts())
        assertEquals(listOf(0, 0, 1), ModVersion.parse("0.0.1")!!.parts())
    }

    @Test
    fun `parse accepts patch suffix as extra segment`() {
        assertEquals(listOf(4, 21, 5, 3, 1), ModVersion.parse("4.21.5.3-patch1")!!.parts())
        assertEquals(listOf(4, 21, 5, 3, 2), ModVersion.parse("4.21.5.3-patch2")!!.parts())
    }

    @Test
    fun `parse rejects invalid formats`() {
        assertNull(ModVersion.parse(""))
        assertNull(ModVersion.parse("   "))
        assertNull(ModVersion.parse("abc"))
        assertNull(ModVersion.parse("1.2.x"))
        assertNull(ModVersion.parse("1.2.3-patch"))
        assertNull(ModVersion.parse("1.2.3-patchx"))
        assertNull(ModVersion.parse("1.2.3-patch1.1"))
    }

    @Test
    fun `parse accepts any number of segments`() {
        assertEquals(listOf(1), ModVersion.parse("1")!!.parts())
        assertEquals(listOf(1, 2), ModVersion.parse("1.2")!!.parts())
        assertEquals(listOf(1, 2, 3, 4, 5), ModVersion.parse("1.2.3.4.5")!!.parts())
    }

    @Test
    fun `parseOrDefault falls back to default`() {
        assertEquals(ModVersion.DEFAULT, ModVersion.parseOrDefault(""))
        assertEquals(ModVersion.DEFAULT, ModVersion.parseOrDefault("nonsense"))
    }
    //endregion

    //region ModVersion comparison
    @Test
    fun `shorter segments compare as padded zeros`() {
        assertTrue(ModVersion.parse("1.2")!! < ModVersion.parse("1.2.1")!!)
        assertTrue(ModVersion.parse("1.2")!! == ModVersion.parse("1.2.0")!!)
        assertTrue(ModVersion.parse("1.2.3")!! < ModVersion.parse("1.2.10")!!)
    }

    @Test
    fun `patch suffix orders after base version`() {
        val base = ModVersion.parse("4.21.5.3")!!
        val patch1 = ModVersion.parse("4.21.5.3-patch1")!!
        val patch2 = ModVersion.parse("4.21.5.3-patch2")!!
        val next = ModVersion.parse("4.21.6")!!
        assertTrue(base < patch1)
        assertTrue(patch1 < patch2)
        assertTrue(patch2 < next)
    }

    @Test
    fun `compareVersions helpers work on same version`() {
        assertTrue(ModVersion.parse("4.21.5.3")!! == ModVersion.parse("4.21.5.3")!!)
        assertFalse(ModVersion.parse("4.21.5.3")!! < ModVersion.parse("4.21.5.3")!!)
    }
    //endregion

    //region ModVersionRange parsing
    @Test
    fun `range parse handles both-sided bounds`() {
        val range = ModVersionRange.parse("1.0.0~2.0.0")!!
        assertEquals(ModVersion.parse("1.0.0"), range.min)
        assertEquals(ModVersion.parse("2.0.0"), range.max)
    }

    @Test
    fun `range parse handles one-sided bounds`() {
        val onlyMin = ModVersionRange.parse("1.0.0~")!!
        assertNull(onlyMin.max)
        val onlyMax = ModVersionRange.parse("~2.0.0")!!
        assertNull(onlyMin.max)
        assertNull(onlyMax.min)
    }

    @Test
    fun `range parse handles exact version and empty`() {
        val exact = ModVersionRange.parse("1.2.0")!!
        assertEquals(ModVersion.parse("1.2.0"), exact.min)
        assertEquals(ModVersion.parse("1.2.0"), exact.max)
        assertEquals(ModVersionRange.ANY, ModVersionRange.parse(""))
        assertEquals(ModVersionRange.ANY, ModVersionRange.parse("  "))
    }

    @Test
    fun `range parse rejects invalid bounds`() {
        assertNull(ModVersionRange.parse("abc"))
        assertNull(ModVersionRange.parse("1.0.0~abc"))
        assertNull(ModVersionRange.parse("abc~2.0.0"))
    }
    //endregion

    //region ModVersionRange containment
    @Test
    fun `range contains versions within bounds`() {
        val range = ModVersionRange.parse("1.0.0~2.0.0")!!
        assertTrue(range.contains(ModVersion.parse("1.0.0")!!))
        assertTrue(range.contains(ModVersion.parse("1.5.0")!!))
        assertTrue(range.contains(ModVersion.parse("2.0.0")!!))
        assertFalse(range.contains(ModVersion.parse("0.9.9")!!))
        assertFalse(range.contains(ModVersion.parse("2.0.1")!!))
    }

    @Test
    fun `one-sided ranges contain correctly`() {
        val onlyMin = ModVersionRange.parse("1.0.0~")!!
        assertTrue(onlyMin.contains(ModVersion.parse("1.0.0")!!))
        assertTrue(onlyMin.contains(ModVersion.parse("9.9.9")!!))
        assertFalse(onlyMin.contains(ModVersion.parse("0.9.9")!!))

        val onlyMax = ModVersionRange.parse("~2.0.0")!!
        assertTrue(onlyMax.contains(ModVersion.parse("0.0.1")!!))
        assertTrue(onlyMax.contains(ModVersion.parse("2.0.0")!!))
        assertFalse(onlyMax.contains(ModVersion.parse("2.0.1")!!))
    }

    @Test
    fun `any range contains everything`() {
        assertTrue(ModVersionRange.ANY.contains(ModVersion.parse("0.0.0")!!))
        assertTrue(ModVersionRange.ANY.contains(ModVersion.parse("99.99.99-patch9")!!))
    }

    @Test
    fun `exact range matches only that version`() {
        val exact = ModVersionRange.parse("1.2.0")!!
        assertTrue(exact.contains(ModVersion.parse("1.2.0")!!))
        assertFalse(exact.contains(ModVersion.parse("1.2.1")!!))
    }

    @Test
    fun `patch suffixes participate in range checks`() {
        val range = ModVersionRange.parse("4.21.5.3~4.21.5.3-patch1")!!
        assertTrue(range.contains(ModVersion.parse("4.21.5.3")!!))
        assertTrue(range.contains(ModVersion.parse("4.21.5.3-patch1")!!))
        assertFalse(range.contains(ModVersion.parse("4.21.5.3-patch2")!!))
    }
    //endregion

    //region ModOptions warnings
    private fun modOptions(
        version: String = "1.2.3",
        recommended: String = "",
        dependencies: List<Pair<String, String>> = emptyList()
    ): ModOptions {
        val options = ModOptions()
        options.modVersion = version
        options.recommendedGameVersion = recommended
        for ((depName, depVersion) in dependencies) {
            val dependency = ModDependency()
            dependency.name = depName
            dependency.version = depVersion
            options.modDependencies += dependency
        }
        return options
    }

    @Test
    fun `no recommended game version warning when not declared`() {
        assertNull(modOptions().getGameVersionWarning("4.21.7.1"))
        assertNull(modOptions().getGameVersionWarning(""))
    }

    @Test
    fun `recommended game version matches exactly`() {
        val options = modOptions(recommended = "4.21.7.1")
        assertNull(options.getGameVersionWarning("4.21.7.1"))
    }

    @Test
    fun `recommended game version warns on mismatch`() {
        val options = modOptions(version = "0.1.0", recommended = "4.21.7.1")
        assertEquals(
            "Mod '[ModOptions]' version [0.1.0] recommends game version [4.21.7.1], you are running [4.21.7.2]",
            options.getGameVersionWarning("4.21.7.2")
        )
        assertEquals(
            "Mod '[ModOptions]' version [0.1.0] recommends game version [4.21.7.1], you are running [4.21.6]",
            options.getGameVersionWarning("4.21.6")
        )
    }

    @Test
    fun `recommended game version warns on invalid declaration`() {
        val options = modOptions(recommended = "not-a-version")
        assertEquals(
            "Invalid recommendedGameVersion '[not-a-version]' in mod '[ModOptions]'",
            options.getGameVersionWarning("4.21.7.1")
        )
    }

    @Test
    fun `no dependency warnings when none declared`() {
        assertTrue(modOptions().getUnsatisfiedDependencies(mapOf("UCCC" to "1.0.0")).isEmpty())
    }

    @Test
    fun `missing dependency is unsatisfied`() {
        val options = modOptions(dependencies = listOf("UCCC" to ""))
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(emptyMap()).map { it.name })
        assertTrue(options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.0.0")).isEmpty())
    }

    @Test
    fun `dependency version range checked against loaded version`() {
        val options = modOptions(dependencies = listOf("UCCC" to "1.0.0~2.0.0"))
        assertTrue(options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.5.0")).isEmpty())
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "2.1.0")).map { it.name })
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "0.9.0")).map { it.name })
    }

    @Test
    fun `dependency exact version required`() {
        val options = modOptions(dependencies = listOf("UCCC" to "1.2.0"))
        assertTrue(options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.2.0")).isEmpty())
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.2.1")).map { it.name })
    }

    @Test
    fun `invalid dependency version requirement is unsatisfied`() {
        val options = modOptions(dependencies = listOf("UCCC" to "abc"))
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.2.3")).map { it.name })
    }
    //endregion

    private fun ModVersion.parts() = toString().split(".").map { it.toInt() }
}
