package de.tasior.fhemExtension.fhem

/**
 * A representative of a dummys reading that is updated when changed in FHEM
 * setting a new value updates the FHEM version of it.
 */
@Suppress("UNCHECKED_CAST")
class LiveReading<T>(private val device: String, private val reading: String, initialValue: T) {

    private var _value = initialValue

    /**
     * Writing the new value triggers the setting to FHEM if it is not identical to the old one
     */
    var value: T
        set(newValue) {
            if (_value == newValue) return
            _value = newValue
            FHEM.setReading(device, reading, newValue.toString())
        }
        get() = _value


    private val observers: MutableList<(newValue: T?) -> Unit> = mutableListOf()

    init {
        FHEM.addMessageListener(object : FhemMessageListener {
            override fun onMessage(message: FhemMessage) {
                if (message.moduleName == device) println(message)
                if (message.moduleName == device && message.attribute == reading && message.value != _value.toString()) {
                    applyNewValue(message.value)
                    notifyObservers()
                }
            }
        })

        val currentVal = FHEM.getJsonlist2DataForDevice(device)?.getValueOfReadingAsString(reading)

        if (currentVal != null) {
            applyNewValue(currentVal)
        } else {
            FHEM.setReading(device, reading, initialValue.toString())
        }
    }

    /**
     * get the new value on change
     */
    fun observe(observer: (newValue: T?) -> Unit) {
        observers.add(observer)
    }

    private fun notifyObservers() {
        for (o in observers) {
            o(_value)
        }
    }

    fun webCmd(cmd: String) {
        FHEM.sendCommandToFhem("set $device $cmd")
    }

    private fun applyNewValue(newValue: String) {
        _value = when (_value) {
            is String? -> newValue as T
            is Int? -> newValue.toIntOrNull() as T
            is Float? -> newValue.toFloatOrNull() as T
            is Double? -> newValue.toDoubleOrNull() as T
            is Boolean? -> newValue.toBoolean() as T
            else -> throw Exception("The value is of an unused type")
        }
    }
}