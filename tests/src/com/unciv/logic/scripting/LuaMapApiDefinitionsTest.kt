package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import com.unciv.testing.GdxTestRunner
import java.nio.file.Files

/**
 * Keeps the map-script EmmyLua type definitions (docs/Modders/lua-map-api.lua) in sync
 * with the engine implementation (LuaMapGenAPI.kt): every API the engine registers must
 * appear in the definitions file, so mod authors' IDE autocompletion never drifts.
 */
@RunWith(GdxTestRunner::class)
class LuaMapApiDefinitionsTest {

    private fun repoRoot() = run {
        val testModFile = Gdx.files.internal("mods/testMOD").file().canonicalFile
        testModFile.parentFile!!.parentFile!!.parentFile!!.parentFile!!
    }

    @Test
    fun mapApiDefinitionsCoverAllRegisteredApis() {
        val root = repoRoot()
        val defsFile = Gdx.files.absolute("${root.path}/docs/Modders/lua-map-api.lua")
        Assert.assertTrue("lua-map-api.lua missing", defsFile.exists())
        val defs = defsFile.readString(Charsets.UTF_8.name())

        val apiSource = Files.readString(root.toPath().resolve("core/src/com/unciv/logic/scripting/LuaMapGenAPI.kt"))

        // Every API registered on the ctx/map/tile tables via t.set("name", ...) must be
        // documented as a @field in the definitions file.
        val registered = Regex("""t\.set\("([A-Za-z]+)"\s*,""")
            .findAll(apiSource)
            .map { it.groupValues[1] }
            .toSet()
        Assert.assertTrue("no APIs extracted from LuaMapGenAPI.kt", registered.size > 30)

        val missing = registered.filter { name ->
            // Fields may be referenced as `---@field name type` (plain fields) or with a
            // `fun(` signature (methods); either form counts as documented.
            Regex("""---@field $name(?: |\()""").find(defs) == null
        }
        Assert.assertTrue(
            "lua-map-api.lua is missing @field entries for: ${missing.sorted().joinToString(", ")}",
            missing.isEmpty()
        )
    }
}
