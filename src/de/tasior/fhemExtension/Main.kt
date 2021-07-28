package de.tasior.fhemExtension

import de.tasior.fhemExtension.externalDevices.ExternalDevice
import de.tasior.fhemExtension.fhem.FHEM

fun main() {
    FHEM.setConnectionData(FHEM_HOST, FHEM_PORT)

    ExternalDevice().apply { registerWithFhem(FHEM) } // This initializes and connects the external device

    FHEM.connect()
}