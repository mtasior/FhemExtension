package de.tasior.fhemExtension.fhem

import de.tasior.fhemExtension.fhem.FhemConnectionHandler.Jsonlist2Result
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class FhemExternalDevice(val deviceName: String, DELAY: Long = -1, INTERVAL: Long = -1)
    : FhemMessageListener {
    open val devicesToListen = listOf<String>()

    private var fhemInstance: FhemConnectionHandler? = null

    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    protected val ON = "on"
    protected val OFF = "off"
    protected val STATE = "state"

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            log("Cancelling the scheduled Executor for device $deviceName")
            scheduledExecutor.shutdown()
            fhemInstance?.let {
                log("deregistering from FHEM")
                deregisterFromFhem()
            }
        })

        if (getCurrentDeviceState() == null) {
            log("Not present in FHEM, creating new dummy")
            FHEM.sendCommandToFhem("define $deviceName dummy")
        }

        if (DELAY > -1 && INTERVAL > -1) {
            startTimedExecution(DELAY, INTERVAL)
        }
    }

    /**
     * Register this virtual device with a given FHEM instance
     */
    fun registerWithFhem(instance: FhemConnectionHandler) {
        fhemInstance = instance
        instance.addMessageListener(this)
    }

    /**
     * Deregisters this virtual device from a FHEM instance if present
     */
    fun deregisterFromFhem(){
        fhemInstance?.removeMessageListener(this)
    }

    override fun onMessage(message: FhemMessage) {
        if (devicesToListen.contains(message.moduleName)) {
            messageReceived(message)
        }
    }

    fun getCurrentDeviceState(): Jsonlist2Result? {
        return FHEM.getJsonlist2DataForDevice(deviceName)
    }

    fun setObjectAsReadings(value: Any?, currentState: Jsonlist2Result? = null) {
        if (value == null) {
            println("object is null, cannot set to FHEM")
            return
        }
        FHEM.setAllFieldsOfClassAsReadingToFhemInstance(value, deviceName, currentState)
    }

    /**
     * Returns the value of a reading for another device
     */
    fun getReadingOfDeviceAsString(device: String, reading: String): String? {
        return FHEM.getJsonlist2DataForDevice(device)?.getValueOfReadingAsString(reading)
    }

    /**
     * Key and value must not contain Sonderzeichen and Umlaute
     */
    fun setReading(reading: String, value: String, currentValue: String? = null) {
        if (currentValue != value) FHEM.setReading(deviceName, reading, value)
    }

    fun sendPushMessage(message: String) {
        FHEM.sendPushMessage(message)
    }

    fun log(message: String) {
        println("${deviceName.uppercase()}: $message")
    }

    open fun messageReceived(message: FhemMessage) {}

    private fun startTimedExecution(delaySeconds: Long, intervalSeconds: Long) {
        scheduledExecutor.scheduleAtFixedRate({
            try {
                runPeriodically()
            } catch (e: Exception) {
                println("$deviceName: Exception in periodical request: ${e.localizedMessage}")
            }
        }, delaySeconds, intervalSeconds, TimeUnit.SECONDS)
    }

    open fun runPeriodically() {}
}