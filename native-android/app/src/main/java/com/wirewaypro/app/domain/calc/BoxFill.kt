package com.wirewaypro.app.domain.calc

/**
 * Outlet / device box fill — NEC 314.16.
 *
 * Volume required is the sum of counted volumes per Table 314.16(B), each priced at
 * the free-space allowance for the relevant conductor size:
 *  • Each current-carrying conductor entering & terminating (or passing through): 1×.
 *  • A conductor that originates and terminates inside the box (pigtail): not counted.
 *  • All equipment grounding conductors together: 1× the largest EGC.
 *  • One or more clamps (internal cable clamps) collectively: 1× the largest conductor.
 *  • Each support fitting (fixture stud, hickey): 1× each.
 *  • Each yoke/strap of a device (receptacle, switch): 2× the largest conductor
 *    connected to that device.
 *
 * Box is adequate when its marked volume ≥ required volume. Deterministic, offline,
 * educational.
 */
object BoxFill {

    /** Conductor sizes eligible for box-fill counting, with Table 314.16(B) volume (in³). */
    enum class BoxWire(val label: String, val volumeSqIn: Double) {
        AWG18("18 AWG", 1.50),
        AWG16("16 AWG", 1.75),
        AWG14("14 AWG", 2.00),
        AWG12("12 AWG", 2.25),
        AWG10("10 AWG", 2.50),
        AWG8("8 AWG", 3.00),
        AWG6("6 AWG", 5.00),
    }

    /**
     * @param conductors count of current-carrying/through conductors of [conductorSize].
     *   (Use the largest size present for a mixed box, per the conservative reading;
     *   callers that track mixed sizes can sum multiple evaluations.)
     * @param devices number of yokes/straps (each device counts as two conductors).
     * @param hasClamps true if the box has one or more internal cable clamps.
     * @param supportFittings number of fixture studs/hickeys.
     * @param groundingConductors number of equipment grounding conductors present
     *   (all count as a single largest-EGC allowance; 0 = none).
     */
    data class Input(
        val conductorSize: BoxWire,
        val conductors: Int,
        val devices: Int = 0,
        val hasClamps: Boolean = false,
        val supportFittings: Int = 0,
        val groundingConductors: Int = 0,
    )

    data class Result(
        val conductorEquivalents: Double, // total "conductor count" charged
        val requiredVolumeSqIn: Double,
        val boxVolumeSqIn: Double,
        val withinLimit: Boolean,
        val spareVolumeSqIn: Double,
    )

    /**
     * Required volume and pass/fail against a box of [boxVolumeSqIn] marked volume.
     * All allowances are priced at [Input.conductorSize] — the largest conductor in the
     * box — which is the conservative, code-correct basis for clamps, devices, and EGCs.
     */
    fun evaluate(input: Input, boxVolumeSqIn: Double): Result {
        val unit = input.conductorSize.volumeSqIn

        var equivalents = input.conductors.toDouble()
        // Each device yoke = 2 conductors.
        equivalents += input.devices * 2
        // All clamps together = 1 conductor.
        if (input.hasClamps) equivalents += 1
        // All grounding conductors together = 1 conductor.
        if (input.groundingConductors > 0) equivalents += 1
        // Each support fitting = 1 conductor.
        equivalents += input.supportFittings

        val required = equivalents * unit
        return Result(
            conductorEquivalents = equivalents,
            requiredVolumeSqIn = required,
            boxVolumeSqIn = boxVolumeSqIn,
            withinLimit = required <= boxVolumeSqIn + 1e-9,
            spareVolumeSqIn = boxVolumeSqIn - required,
        )
    }
}
