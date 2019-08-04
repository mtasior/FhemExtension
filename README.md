# A bidirectional communication framework for FHEM

## External dependencies

This project uses the following external dependencies (IntelliJ integrated maven notation)

**Google Gson** com.google.code.gson:gson:Latest  
**Apache Http Components:** org.apache.httpcomponents:httpclient:Latest  
**Java Websocket:** org.java-websocket:Java-WebSocket:Latest  

To use them in IntelliJ Idea, just copy the coordinates and add them in
_File -> Project Structure -> Libraries -> + -> From Maven_

## Idea
The general concept behind this framework is: create a Websocket, listen to all messages from FHEM and control dummy devices that represent the "External devices" in the Kotlin by setting the appropriate values into the dummy device.

## Usage

Define a FHEMWEB device with attributes  
```
CORS        0
csrfToken   none
longpoll    websocket
```

Define the name of your FHEMWEB device and its port in `Constants.kt` and run `Main.kt`.  
For every external device you may want to integrate into FHEM, create a class that inherits from `FhemExternalDevice`.
Every `FhemExternalDevice` must be provided with a name of a corresponding dummy device in your FHEM instance.  

Now you can start listening to messages from your FHEM by providing a list of device names like so:

```java
override val devicesToListen: List<String>
        get() = listOf(DEVICE_A, DEVICE_B)
```

Every time a message from the given devices is fired, the `messageReceived()` is called. You can then query for attributes and values:

```java
override fun messageReceived(message: FhemMessage) {
        if (message.attribute == A_READING_YOU_ARE_INTERESTED_IN) {
            // do something with message.value
        }
```

If you provide the constructor with a DELAY and an INTERVAL in seconds `class MyDevice : FhemExternalDevice(DEVICE_DUMMY_NAME, 2L, 10L) {` then the `runPeriodically()` is called in the given interval

```java
override fun runPeriodically() {
        //do something with the given interval
    }
```
This is called in a new `Thread` every time it is invoked. If it fails, the subsequent calls will then also run.

You might then want to update data in your dummy to store stuff or trigger functionality implemented in FHEM. This can be done using
```java
fun setObjectAsReadings(value: Any?, currentState: Jsonlist2Result? = null)
```
This reflects through the given object and writes all fields as reading into the given dummy device. This is especially useful if you have some kind of data class for an external interface you want to store in your FHEM instance. If you provide the currentState, it only writes the changed values.  
The current state of the dummy can be called using
```java
fun getCurrentDeviceState(): Jsonlist2Result?
```
on your FhemExternalDevice. From this `JsonList2Result` a desired reading can be received using
```java
result.getValueOfReadingAsString(reading: String): String?)
```

There is also a function very specific to the author's FHEM instance: `fun sendPushMessage(message: String)` which triggers a Push Message using FHEM Widget 2's push functionality. You can replicate this if you desire.  

In case you want to modify FHEM directly from your device, e.g. for changing values in some FHEM device, you can execute FHEM commands directly from within your FhemExternalDevice by calling 

```java
FHEM.sendCommandToFhem("any FHEM command you would write into the textfield")
```

