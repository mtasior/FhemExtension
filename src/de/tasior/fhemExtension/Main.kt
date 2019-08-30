package de.tasior.fhemExtension

import de.tasior.fhemExtension.externalDevices.ExternalDevice
import de.tasior.fhemExtension.fhem.FHEM

fun main() {
    FHEM.setConnectionData(FHEM_HOST, FHEM_PORT)

    ExternalDevice() // This initializes the external device which connects itself to FHEM

    // IMPORTANT! do this at the very end of the initialization phase. For more information check [FhemExternalDevice]
    FHEM.startConnecting()
}