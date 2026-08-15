package com.unciv.models.ruleset

import com.badlogic.gdx.Gdx
import com.unciv.models.ruleset.validation.RulesetValidator
import com.unciv.testing.GdxTestRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

/** Mod root folder checks: JSON files must live in `jsons/`, except the whitelisted tooling configs. */
@RunWith(GdxTestRunner::class)
class RulesetFileLocationTests {

    private fun validateModRoot(vararg rootFiles: String): List<String> {
        val tempDir = Files.createTempDirectory("modFileLocation")
        try {
            for (fileName in rootFiles)
                Files.write(tempDir.resolve(fileName), "{}".toByteArray())
            val ruleset = Ruleset().apply {
                name = "TestMod"
                folderLocation = Gdx.files.absolute(tempDir.toAbsolutePath().toString())
            }
            return RulesetValidator.create(ruleset).getErrorList()
                .filter { it.text.contains("root folder") }
                .map { it.text }
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        }
    }

    @Test
    fun `ruleset json in root folder is reported as misplaced`() {
        val errors = validateModRoot("Buildings.json")
        assertTrue(errors.any { it.contains("Buildings.json") && it.contains("'jsons' folder") })
    }

    @Test
    fun `luarc json in root folder is not reported`() {
        val errors = validateModRoot(".luarc.json")
        assertFalse(errors.any { it.contains(".luarc.json") })
    }

    @Test
    fun `atlas files in root folder are not reported`() {
        val errors = validateModRoot("AtlasTest.json")
        assertFalse(errors.any { it.contains("AtlasTest.json") })
    }

    @Test
    fun `whitelist covers the LuaLS config file`() {
        assertTrue(".luarc.json" in RulesetValidator.knownRootJsonFiles)
    }
}
