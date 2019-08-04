package de.tasior.fhemExtension.externalDevices

import de.tasior.fhemExtension.EXTERNAL_DEVICE_DUMMY_NAME
import de.tasior.fhemExtension.fhem.FhemExternalDevice
import de.tasior.fhemExtension.fhem.FhemMessage

class ExternalDevice : FhemExternalDevice(EXTERNAL_DEVICE_DUMMY_NAME){


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