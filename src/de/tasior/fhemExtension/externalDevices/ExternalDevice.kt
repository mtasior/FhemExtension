package de.tasior.fhemExtension.externalDevices

import de.tasior.fhemExtension.EXTERNAL_DEVICE_DUMMY_NAME
import de.tasior.fhemExtension.fhem.FhemExternalDevice
import de.tasior.fhemExtension.fhem.FhemMessage
import de.tasior.fhemExtension.fhem.LiveReading

class ExternalDevice : FhemExternalDevice(EXTERNAL_DEVICE_DUMMY_NAME){

    /**
     * This reading is created and prefilled with the initial value
     */
    private val someReading = LiveReading(EXTERNAL_DEVICE_DUMMY_NAME, "synchronizedReading", "OFF")

    init {
        /**
         * get all changes that are done in FHEM, no matter who did it
         */
        someReading.observe {
            println("OMG, someone changed the reading to $it")
            // write the new value directly to sync it to FHEM
            someReading.value = "newValue"
        }
    }


    /** =============================
     * Below are all useful callbacks
     *  ============================= */

    /**
    * All devices for which the [messageReceived] shall be called
     */
    override val devicesToListen: List<String>
        get() = listOf()

    /**
     * A message from one of the devices in [devicesToListen]
     */
    override fun messageReceived(message: FhemMessage) {
        super.messageReceived(message)
    }

    /**
     * A periodically running function, delay and period defined in the constructor
     */
    override fun runPeriodically() {
        super.runPeriodically()
    }


}