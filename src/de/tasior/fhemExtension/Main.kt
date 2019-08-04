package de.tasior.fhemExtension

import de.tasior.fhemExtension.externalDevices.ExternalDevice
import de.tasior.fhemExtension.fhem.FHEM

fun main(args: Array<String>) {
    FHEM.setConnectionData(FHEM_HOST, FHEM_PORT)
    FHEM.startConnecting()

    ExternalDevice() // This initializes the external device which connects itself to FHEM
}