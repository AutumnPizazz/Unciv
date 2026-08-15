package com.unciv.testing

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import java.io.File

/**
 * Access to test-only assets bundled with the tests module.
 *
 * [testMod] lives in `tests/src/test/resources/testMOD` on purpose - it is a fixture for the
 * test suite, not a runtime mod, and must never be packaged into the game's assets
 * (`android/assets/mods/` ships to players and would show up in their mod list).
 *
 * It is located via the classpath so tests work both from Gradle (resources land in
 * `tests/build/resources/test/`) and from IDEs (resources land in the module's output dir).
 * The raw [File] is needed because libgdx's classpath [FileHandle]s cannot resolve `.file()`.
 */
object TestAssets {

    /** The bundled testMOD directory (`tests/src/test/resources/testMOD`) as a filesystem file. */
    fun testModDirFile(): File {
        val resourceUrl = TestAssets::class.java.classLoader.getResource("testMOD")
            ?: error("testMOD not found on the tests classpath - is tests/src/test/resources on it?")
        return File(resourceUrl.toURI())
    }

    /** The bundled testMOD directory as a libgdx [FileHandle] (absolute), for Ruleset.load etc. */
    fun testModDir(): FileHandle = Gdx.files.absolute(testModDirFile().path)

    /** Repository root, located by walking up from the testMOD resources until `settings.gradle.kts` is found. */
    fun repoRoot(): File {
        var dir = testModDirFile().canonicalFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists())
            dir = dir.parentFile
        return dir ?: error("Repository root not found while walking up from ${testModDirFile()}")
    }
}
