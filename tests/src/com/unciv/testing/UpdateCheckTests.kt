package com.unciv.testing

import com.unciv.logic.UpdateChecker
import com.unciv.logic.compareVersionStrings
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class UpdateCheckTests {
    @Test
    fun versionComparison() {
        // CN fork scheme: four segments (upstream major.minor.patch.cn-subversion)
        Assert.assertEquals(0, compareVersionStrings("4.21.6.5", "4.21.6.5"))
        Assert.assertTrue(compareVersionStrings("4.21.6.6", "4.21.6.5") > 0)
        Assert.assertTrue(compareVersionStrings("4.21.6.5", "4.21.6.6") < 0)
        Assert.assertTrue(compareVersionStrings("4.21.7.1", "4.21.6.9") > 0)

        // Upstream release tags: patch suffix and optional 'v' prefix
        Assert.assertTrue(compareVersionStrings("4.21.7-patch2", "4.21.7") > 0)
        Assert.assertTrue(compareVersionStrings("4.21.7-patch2", "4.21.7-patch10") < 0)
        Assert.assertTrue(compareVersionStrings("v4.21.6", "4.21.5.9") > 0)

        // Missing trailing segments count as zero
        Assert.assertTrue(compareVersionStrings("4.21.6.5", "4.21.6") > 0)
        Assert.assertTrue(compareVersionStrings("4.21.6", "4.21.6.5") < 0)
    }

    @Test
    fun isNewerThanCurrent() {
        Assert.assertTrue(UpdateChecker.isNewerThan("4.21.6.6", "4.21.6.5"))
        Assert.assertFalse(UpdateChecker.isNewerThan("4.21.6.5", "4.21.6.5"))
        Assert.assertFalse(UpdateChecker.isNewerThan("4.21.6.4", "4.21.6.5"))
    }
}
