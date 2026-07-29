package com.fuerz4.assistant.domain.model

/** Mirrors NanoServer's `Device.getType()`, which is derived from the UUID prefix, not stored separately. */
enum class DeviceType(val prefix: String) {
    ENERGY("WFEM"),
    ENVIRONMENT("WFTM");

    val wireValue: String
        get() = when (this) {
            ENERGY -> "energy"
            ENVIRONMENT -> "environment"
        }

    companion object {
        fun fromWireValue(value: String?): DeviceType? = when (value) {
            "energy" -> ENERGY
            "environment" -> ENVIRONMENT
            else -> null
        }

        fun fromUuid(uuid: String): DeviceType? = entries.firstOrNull { uuid.startsWith(it.prefix) }
    }
}
