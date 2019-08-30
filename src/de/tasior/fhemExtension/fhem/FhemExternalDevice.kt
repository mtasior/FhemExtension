package de.tasior.fhemExtension.fhem

import de.tasior.fhemExtension.fhem.FhemConnectionHandler.Jsonlist2Result
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("LeakingThis") // Can be omitted by connecting to FHEM last
abstract class FhemExternalDevice(val deviceName: String, DELAY: Long = -1, INTERVAL: Long = -1)
    : FhemMessageListener {
    open val devicesToListen = listOf<String>()

    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    protected val ON = "on"
    protected val OFF = "off"
    protected val STATE = "state"

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Cancelling the scheduled Executor for device $deviceName")
            scheduledExecutor.shutdown()
        })

        if (DELAY > -1 && INTERVAL > -1) {
            startTimedExecution(DELAY, INTERVAL)
        }

        // it leaks "this" in the constructor which can lead to null pointers after startup. This is why it is advisable
        // that the initialization sequence is
        //    FHEM.setConnectionData(FHEM_HOST, FHEM_PORT)
        //
        //    FhemExternalDeviceA()
        //    FhemExternalDeviceB()
        //    FhemExternalDeviceC()
        //
        //    FHEM.startConnecting()
        //
        // It is implemented like this to avoid the additional init step in each derived Device
        FHEM.addMessageListener(this)
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

    open fun messageReceived(message: FhemMessage) {}

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

    open fun runPeriodically() {}
}