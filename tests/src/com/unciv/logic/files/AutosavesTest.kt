package com.unciv.logic.files

import com.badlogic.gdx.Files
import com.badlogic.gdx.files.FileHandle
import com.unciv.logic.GameInfo
import com.unciv.models.metadata.GameSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AutosavesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun overlappingAutosavesKeepTheLatestSaveAndTheCorrectTurnBackup() = runBlocking {
        val saveFolder = FileHandle(folder.newFolder(SAVE_FILES_FOLDER))
        val firstReadyToMove = CountDownLatch(1)
        val releaseFirstMove = CountDownLatch(1)
        val isFirstMove = AtomicBoolean(true)
        val autosave = object : FileHandle(saveFolder.child("Autosave").file()) {
            override fun sibling(name: String): FileHandle {
                return object : FileHandle(super.sibling(name).file()) {
                    override fun moveTo(dest: FileHandle) {
                        if (isFirstMove.getAndSet(false)) {
                            firstReadyToMove.countDown()
                            check(releaseFirstMove.await(10, TimeUnit.SECONDS))
                        }
                        super.moveTo(dest)
                    }
                }
            }
        }

        val backend = mock(Files::class.java)
        `when`(backend.local(SAVE_FILES_FOLDER)).thenReturn(saveFolder)
        `when`(backend.local("$SAVE_FILES_FOLDER/Autosave")).thenReturn(autosave)
        val files = spy(UncivFiles(backend))
        doReturn(GameSettings()).`when`(files).getGeneralSettings()
        val autosaves = Autosaves(files)

        val turnFive = GameInfo().apply {
            turns = 5
            currentPlayer = "Rome"
        }
        val turnSix = GameInfo().apply { turns = 6 }
        val first = autosaves.requestAutoSaveUnCloned(turnFive, nextTurn = true)
        try {
            assertTrue("The first save must reach the temporary file move", firstReadyToMove.await(10, TimeUnit.SECONDS))
            val second = autosaves.requestAutoSaveUnCloned(turnSix)
            val finishedEarly = withTimeoutOrNull(1_000) {
                second.join()
                true
            }
            assertNull("The newer save must wait for the current save and its backup", finishedEarly)
        } finally {
            releaseFirstMove.countDown()
            withTimeout(10_000) {
                first.join()
                autosaves.autoSaveJob?.join()
            }
        }

        assertEquals(5, savedTurn(saveFolder.child("Autosave-Rome-5")))
        assertEquals(6, savedTurn(autosave))
        assertFalse(saveFolder.child("Autosave.tmp").exists())
    }

    private fun savedTurn(file: FileHandle): Int = FileConversions.readJson(file.read(), GameInfo::class.java)!!.turns
}
