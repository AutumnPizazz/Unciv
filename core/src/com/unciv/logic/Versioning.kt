package com.unciv.logic

import com.unciv.UncivGame
import com.unciv.models.translations.tr
import yairm210.purity.annotations.Pure

/**
 *  Wrapper for a release version.
 *
 *  - An instance holding current values is created as [UncivGame.VERSION] via a workflow editing source.
 *  - Formatter: [toNiceString] - localized
 *
 *  @property text corresponds to BuildConfig.appVersion
 *  @property number corresponds to BuildConfig.appCodeNumber
 */
data class Version(
    val text: String,
    val number: Int
) : IsPartOfGameInfoSerialization {
    @Suppress("unused") // used by json serialization
    internal constructor() : this("", -1)

    @Pure fun toNiceString() = "[$text] (Build [$number])".tr()
    @Pure fun toSerializeString() = "$text (Build $number)"
}

interface HasGameInfoSerializationVersion {
    val version: CompatibilityVersion
}

/**
 * Compare two version strings numerically, ignoring non-digit separators.
 *
 * Handles the release tag formats used by both the CN fork (e.g. `4.21.6.5`) and upstream
 * (`4.21.7` / `4.21.7-patch2` / optional `v` prefix). Missing trailing segments count as 0,
 * so `4.21.6` < `4.21.6.5` and `4.21.7-patch2` compares as `4.21.7.2`.
 *
 * @return &gt; 0 if [a] is newer than [b], 0 if equal, &lt; 0 if older
 */
fun compareVersionStrings(a: String, b: String): Int {
    val aNumbers = versionNumberSegments(a)
    val bNumbers = versionNumberSegments(b)
    val maxSegments = maxOf(aNumbers.size, bNumbers.size)
    for (i in 0 until maxSegments) {
        val comparison = aNumbers.getOrElse(i) { 0 }.compareTo(bNumbers.getOrElse(i) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

private fun versionNumberSegments(version: String): List<Int> {
    val segments = ArrayList<Int>()
    var index = 0
    while (index < version.length) {
        val char = version[index]
        if (char in '0'..'9') {
            var end = index
            while (end < version.length && version[end] in '0'..'9') end++
            segments.add(version.substring(index, end).toInt())
            index = end
        } else {
            index++
        }
    }
    return segments
}

data class CompatibilityVersion(
    /** Contains the current serialization version of [GameInfo], i.e. when this number is not equal to [CURRENT_COMPATIBILITY_NUMBER], it means
     * this instance has been loaded from a save file json that was made with another version of the game. */
    val number: Int,
    /** This is the version that saved the game, not the one that did the "new game". */
    val createdWith: Version
) : IsPartOfGameInfoSerialization, Comparable<CompatibilityVersion> {
    @Suppress("unused") // used by json serialization
    internal constructor() : this(-1, Version())

    @Pure
    override operator fun compareTo(other: CompatibilityVersion) = number.compareTo(other.number)

    companion object {
        /** The current compatibility version of [GameInfo]. This number is incremented whenever changes are made to the save file structure that guarantee that
         * previous versions of the game will not be able to load or play a game normally. */
        const val CURRENT_COMPATIBILITY_NUMBER = 5

        val CURRENT_COMPATIBILITY_VERSION = CompatibilityVersion(CURRENT_COMPATIBILITY_NUMBER, UncivGame.Companion.VERSION)

        /** This is the version just before this field was introduced, i.e. all saves without any version will be from this version */
        val FIRST_WITHOUT = CompatibilityVersion(1, Version("4.1.14", 731))
    }
}

/** Class to use when parsing a saved game json if you only want the serialization [version]. */
class GameInfoSerializationVersion : HasGameInfoSerializationVersion {
    override var version = CompatibilityVersion.FIRST_WITHOUT
}
