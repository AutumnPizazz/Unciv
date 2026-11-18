package com.unciv.ui.popups.options

import com.badlogic.gdx.utils.Array
import com.unciv.models.UncivSound
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.format
import com.unciv.utils.toGdxArray
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.reflect.KMutableProperty0


internal interface MultiplayerSelectBoxHelpers {
    /** Wraps a [value] with a translated display [label] for use in a [SelectBox] */
    abstract class SelectItem<T>(val label: String, val value: T) {
        override fun toString(): String = label.tr()
        override fun equals(other: Any?): Boolean = other is SelectItem<*> && value == other.value
        override fun hashCode(): Int = value.hashCode()
    }

    /** Holds an UncivSound with a prettified UI label for use in a SelectBox */
    class UncivSoundLabeled(label: String, value: UncivSound) : SelectItem<UncivSound>(label, value) {
        constructor(entry: Map.Entry<String, String>) : this(entry.value, UncivSound(entry.key))
    }

    /** Translate a reference for an UncivSound (stored in settings) -> UncivSoundLabeled field that can be used as reference for addSelectBox */
    class UncivSoundProxy(private val property: KMutableProperty0<UncivSound>) {
        var value: UncivSoundLabeled
            get() = UncivSoundLabeled("", property.get()) // can do without pretty label as it's used only for selection
            set(value) {
                property.set(value.value)
            }
    }

    class DurationItem(label: String, value: Duration) : SelectItem<Duration>(label, value) {
        constructor(duration: Duration) : this(duration.format(), duration)
    }

    fun createRefreshOptions(unit: ChronoUnit, vararg options: Long) = options.map { DurationItem(Duration.of(it, unit)) }

    /** A helper to more easily initialize and connect a standard SelectBox for Duration properties
     *  @param property The setting property to edit - used in [value]
     *  @param options Items to offer. Use [createRefreshOptions] to translate them.
     *  @property value Use `this::value` as property parameter for `addSelectBox`
     */
    class RefreshSelectOptions(
        private val property: KMutableProperty0<Duration>,
        options: Iterable<DurationItem>
    ) {
        val items: Array<DurationItem> = options.toGdxArray()

        var value: DurationItem
            get() = DurationItem(property.get()) // can do without pretty label as it's used only for selection
            set(value) {
                property.set(value.value)
            }

    }
}
