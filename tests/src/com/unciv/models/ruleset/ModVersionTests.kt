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
            dependency.recommendedVersion = depVersion
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
    fun `dependency recommended version matched exactly`() {
        val options = modOptions(dependencies = listOf("UCCC" to "1.5.0"))
        assertTrue(options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.5.0")).isEmpty())
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.5.1")).map { it.name })
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.4.9")).map { it.name })
    }

    @Test
    fun `invalid recommended dependency version is unsatisfied`() {
        val options = modOptions(dependencies = listOf("UCCC" to "abc"))
        assertEquals(listOf("UCCC"), options.getUnsatisfiedDependencies(mapOf("UCCC" to "1.2.3")).map { it.name })
    }
    //endregion

    private fun ModVersion.parts() = toString().split(".").map { it.toInt() }
}
