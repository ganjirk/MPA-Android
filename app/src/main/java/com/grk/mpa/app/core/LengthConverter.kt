package com.grk.mpa.app.core

/**
 * Length units supported by the converter. [meters] is the size of one unit in metres.
 * Ported from the legacy Django LengthConversion app (which used rounded factors and
 * global state); this version uses exact factors and has no shared state.
 */
enum class LengthUnit(val label: String, val meters: Double) {
    MILLIMETRE("Millimetre", 0.001),
    CENTIMETRE("Centimetre", 0.01),
    METRE("Metre", 1.0),
    KILOMETRE("Kilometre", 1000.0),
    INCH("Inch", 0.0254),
    MILE("Mile", 1609.344)
}

object LengthConverter {
    fun convert(value: Double, from: LengthUnit, to: LengthUnit): Double =
        value * from.meters / to.meters

    /** Converts [value] into every supported unit. */
    fun convertAll(value: Double, from: LengthUnit): Map<LengthUnit, Double> =
        LengthUnit.entries.associateWith { convert(value, from, it) }
}
