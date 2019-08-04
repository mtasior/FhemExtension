package de.tasior.fhemExtension.fhem

import de.tasior.fhemExtension.fhem.FhemConnectionHandler.Jsonlist2Result
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class FhemExternalDevice(val deviceName: String, DELAY: Long = -1, INTERVAL: Long = -1)
    : FhemMessageListener {
    open val devicesToListen = listOf<String>()

    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    protected val ON = "on"
    protected val OFF = "off"

    init {
        FHEM.addMessageListener(this)

        Runtime.getRuntime().addShutdownHook(Thread {
            println("Cancelling the scheduled Executor for device $deviceName")
            scheduledExecutor.shutdown()
        })

        if (DELAY > -1 && INTERVAL > -1) {
            startTimedExecution(DELAY, INTERVAL)
        }
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
     * Key and value must not contain Sonderzeichen and Umlaute
     */
    fun setReading(reading: String, value: String, currentValue: String? = null) {
        if (currentValue != value) FHEM.setReading(deviceName, reading, value)
    }

    fun sendPushMessage(message: String) {
        FHEM.sendPushMessage(message)
    }

    open fun messageReceived(message: FhemMessage){}

    private fun startTimedExecution(delaySeconds: Long, intervalSeconds: Long) {
        scheduledExecutor.scheduleAtFixedRate({
            executeScheduled()
        }, delaySeconds, intervalSeconds, TimeUnit.SECONDS)
    }

    private fun executeScheduled() {
        Thread(Runnable {
            runPeriodically()
        }).start()
    }

    open fun runPeriodically(){}
}