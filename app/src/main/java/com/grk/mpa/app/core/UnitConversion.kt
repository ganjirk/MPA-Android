package com.grk.mpa.app.core

/**
 * One unit inside a [UnitCategory]. Every unit knows how to go to and from the category's base unit,
 * so any unit converts to any other through it.
 */
class ConversionUnit private constructor(
    val label: String,
    private val toBaseFunction: (Double) -> Double,
    private val fromBaseFunction: (Double) -> Double,
    /** HTML min attribute equivalent from the website, when one is defined for this unit. */
    val minimum: Double? = null
) {
    fun toBase(value: Double): Double = toBaseFunction(value)
    fun fromBase(value: Double): Double = fromBaseFunction(value)

    override fun toString(): String = label

    companion object {
        /** A unit that is [factor] base units, such as 1 foot = 0.3048 metres. */
        fun linear(label: String, factor: Double, minimum: Double? = null) =
            ConversionUnit(label, { it * factor }, { it / factor }, minimum)

        /** A unit that needs its own formulas, such as Fahrenheit. */
        fun custom(
            label: String,
            toBase: (Double) -> Double,
            fromBase: (Double) -> Double,
            minimum: Double? = null
        ) = ConversionUnit(label, toBase, fromBase, minimum)
    }
}

class UnitCategory(
    val id: String,
    val label: String,
    val units: List<ConversionUnit>,
    /** True when negative values make sense, so the screen can offer a keyboard with a minus sign. */
    val allowsNegative: Boolean = false,
    val note: String? = null
)

/**
 * The same unit groups, units and factors as the MPA website's Unit Conversion page, in the
 * same order. Minimums mirror the website's input min attributes.
 */
object UnitCategories {
    private fun linear(label: String, factor: Double, minimum: Double? = null) =
        ConversionUnit.linear(label, factor, minimum)

    val length = UnitCategory(
        id = "length", label = "Length", allowsNegative = true,
        units = listOf(
            linear("Feet", 0.3048), linear("Meters", 1.0), linear("Inches", 0.0254),
            linear("cm", 0.01), linear("Yards", 0.9144), linear("Kilometers", 1000.0),
            linear("Miles", 1609.344)
        )
    )

    val time = UnitCategory(
        id = "time", label = "Time",
        units = listOf(
            linear("Seconds", 1.0, 0.0), linear("Minutes", 60.0, 0.0),
            linear("Hours", 3600.0, 0.0), linear("Days", 86_400.0, 0.0),
            linear("Weeks", 604_800.0, 0.0)
        )
    )

    val weight = UnitCategory(
        id = "weight", label = "Weight", allowsNegative = true,
        units = listOf(
            linear("Pounds", 0.45359237), linear("Kilograms", 1.0),
            linear("Ounces", 0.028349523125), linear("Grams", 0.001),
            linear("Stones", 6.35029318)
        )
    )

    val temperature = UnitCategory(
        id = "temperature", label = "Temperature", allowsNegative = true,
        units = listOf(
            ConversionUnit.custom("Fahrenheit", { (it - 32) / 1.8 }, { it * 1.8 + 32 }, -459.67),
            ConversionUnit.custom("Celsius", { it }, { it }, -273.15),
            ConversionUnit.custom("Kelvin", { it - 273.15 }, { it + 273.15 }, 0.0)
        )
    )

    val volume = UnitCategory(
        id = "volume", label = "Volume", allowsNegative = true,
        units = listOf(
            linear("Litres", 1.0), linear("Millilitres", 0.001),
            linear("Gallons (US)", 3.785411784), linear("Cups (US)", 0.2365882365),
            linear("Fl oz (US)", 0.0295735295625)
        )
    )

    val speed = UnitCategory(
        id = "speed", label = "Speed", allowsNegative = true,
        units = listOf(
            linear("MPH", 1.609344), linear("KPH", 1.0),
            linear("Knots", 1.852), linear("Mach", 1225.044)
        )
    )

    val area = UnitCategory(
        id = "area", label = "Area",
        units = listOf(
            linear("Sq metres", 1.0, 0.0), linear("Sq feet", 0.09290304, 0.0),
            linear("Acres", 4046.8564224, 0.0), linear("Hectares", 10_000.0, 0.0),
            linear("Sq miles", 2_589_988.110336, 0.0)
        )
    )

    val data = UnitCategory(
        id = "data", label = "Data",
        units = listOf(
            linear("Bytes", 1.0, 0.0), linear("KB", 1e3, 0.0),
            linear("MB", 1e6, 0.0), linear("GB", 1e9, 0.0), linear("TB", 1e12, 0.0)
        ),
        note = "1 KB = 1,000 bytes, as on phone and disk labels."
    )

    val all: List<UnitCategory> = listOf(length, time, weight, temperature, volume, speed, area, data)
}

object UnitConverter {
    fun convert(value: Double, from: ConversionUnit, to: ConversionUnit): Double =
        to.fromBase(from.toBase(value))
}
