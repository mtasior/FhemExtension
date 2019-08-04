package de.tasior.fhemExtension.fhem

import com.google.gson.Gson
import com.google.gson.JsonObject
import de.tasior.fhemExtension.EXTENSION_VERSION
import de.tasior.fhemExtension.FHEM_EXTENSION_DEVICE
import de.tasior.fhemExtension.util.Dumpable
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.net.*
import java.util.*
import kotlin.reflect.full.memberProperties

/**
 * Created by michaeltasior on 21.12.17.
 *
 *
 * Handles the fhemBidirectionalConnection connections to the given FHEM instance.
 * It receives and dispatches FHEM Events via the FhemMessageListener
 *
 * Use it as a singleton
 */
val FHEM: FhemConnectionHandler
    get() = FhemConnectionHandler.instance


class FhemConnectionHandler private constructor() : FhemWebsocketClientDelegate {
    private var host: String? = null
    private var port = -1
    private val listeners = ArrayList<FhemMessageListener>()
    private var fhemWebsocketClient: FhemWebsocketClient? = null
    private var statusDevice = FHEM_EXTENSION_DEVICE


    companion object {
        val instance = FhemConnectionHandler()
    }

    fun startConnecting() {
        if (host == null || port == -1) {
            println("Please provide URL and Port")
            System.exit(0)
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            println("Closing connection to FHEM")
            fhemWebsocketClient?.close()
            setDebugState("disconnected")
        })

        createAndConnect()
        setAllFieldsOfClassAsReadingToFhemInstance(Version(), statusDevice)
    }

    fun setConnectionData(ip: String, port: Int, device: String? = null) {
        this.host = ip
        this.port = port
        device?.let { this.statusDevice = device }

        println("Connecting to $ip:$port")
    }

    private fun createAndConnect() {
        fhemWebsocketClient = null
        try {
            fhemWebsocketClient = FhemWebsocketClient(URI("ws://" + host + ":" + port +
                    "/fhem?XHR=1&inform=type=raw;withLog=0;filter=.*&timestamp=1513796954504")).apply {
                setListeners(listeners)
                setDelegate(this@FhemConnectionHandler)
                connect()
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

    }

    /**
     * add a listener to all opened fhemBidirectionalConnection connections, persists over fhemBidirectionalConnection connection drops.
     *
     * @param listener
     */
    fun addMessageListener(listener: FhemMessageListener) {
        listeners.add(listener)
        fhemWebsocketClient?.addListener(listener)
    }

    /**
     * gets all fields of a class and sets them as a reading for a given Device
     *
     * @param pValues
     * @param pDevice
     * @param pCurrentState The current state of the device. If this is not null, the value is only written if it is changed
     */
    fun setAllFieldsOfClassAsReadingToFhemInstance(pValues: Any, pDevice: String, pCurrentState: Jsonlist2Result? = null) {
        pValues.javaClass.kotlin.memberProperties.forEach {
            val newValue = it.get(pValues)
            val oldValue = pCurrentState?.getValueOfReadingAsString(it.name)
            if (oldValue == null || oldValue != newValue.toString())
                setReading(pDevice, it.name, "$newValue")
        }
    }

    /**
     * Returns the JsonList2 result for the given device.
     *
     * @param pDevice
     * @return the specified device, null if no device or more than one device
     */
    fun getJsonlist2DataForDevice(pDevice: String): Jsonlist2Result? {
        val rawJson = sendCommandToFhem("jsonlist2 $pDevice")
        val answer = Gson().fromJson(rawJson, Jsonlist2Answer::class.java)
        return if (answer.Results!!.size == 1) {
            answer.Results!![0]
        } else {
            null
        }
    }

    /**
     * set or update the state in the HomeController dummy in the connected FHEM
     *
     * @param value
     */
    fun setDebugState(value: String) {
        sendCommandToFhem("set $statusDevice $value")
    }

    /**
     * set or update a defined reading in the HomeController dummy in the connected FHEM
     *
     * @param key
     * @param value
     */
    fun setDebugReading(key: String, value: String) {
        setReading(statusDevice, key, value)
    }

    /**
     * sends a FHEMWidget2 Push message via msgHandler. This is specific to my FHEM installation
     * @param pMessage
     */
    fun sendPushMessage(pMessage: String) {
        sendCommandToFhem("set pushMessage $pMessage")
        println("Sent \"$pMessage\" via Push")
    }

    /**
     * specialized function to set a reading for a device
     *
     * @param pDevice
     * @param pReadingName
     * @param pReadingValue
     */
    fun setReading(pDevice: String, pReadingName: String, pReadingValue: String) {
        sendCommandToFhem("setreading $pDevice $pReadingName $pReadingValue")
    }

    /**
     * the generic sendCommandToFhem that can set everything. Returns a String response message or null
     *
     * @param pCommand
     * @return String response or null
     */
    fun sendCommandToFhem(pCommand: String): String? {
        if (host == null || port == -1) {
            println("IP and/or Port missing, cannot connect to FHEM")
            return null
        }


        var res: String? = null
        var response: CloseableHttpResponse? = null

        try {
            var urlString = "http://$host:$port/fhem?cmd="
            val query = URLEncoder.encode(pCommand, "UTF-8").replace("+", "%20")
            urlString += "$query&XHR=1"

            val url = URL(urlString)
            //println("Sending Command: \"$urlString\"")

            //execute the request
            val fhemRequest = HttpGet(url.toURI())
            response = HttpClients.createDefault().execute(fhemRequest)
            res = EntityUtils.toString(response.getEntity())
            EntityUtils.consume(response.getEntity())

            //println("Received: " + res!!)

        } catch (ce: ConnectException) {
            println("Connection refused (sendCommandToFhem)")
        } catch (e: Exception) {
            println(e.message)
        } finally {
            try {
                response?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return res
    }

    override fun socketIsOpened() {
        setDebugState("connected")
    }

    override fun socketIsClosed(code: Int, reason: String, remote: Boolean) {
        setDebugState("disconnected")
        //System.out.println("Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
        createAndConnect()
    }

    inner class Jsonlist2Answer : Dumpable() {
        //The variables are coming from FHEM, cannot follow the naming convention without translation
        internal var Arg: String? = null
        internal var Results: Array<Jsonlist2Result>? = null
        internal var totalResultsReturned: Int = 0
    }

    inner class Jsonlist2Result : Dumpable() {
        var Name: String? = null
        var PossibleSets: String? = null
        var PossibleAttrs: String? = null
        var Internals: JsonObject? = null
        var Readings: JsonObject? = null
        var Attributes: JsonObject? = null

        fun getValueOfReadingAsString(reading: String): String? {
            return Readings?.getAsJsonObject(reading)?.getAsJsonPrimitive("Value")?.asString
        }
    }

    inner class Version {
        val version = EXTENSION_VERSION

        init {
            println("FhemConnector Version $version")
        }
    }
}

/**
 * Created by michaeltasior on 21.12.17.
 */
interface FhemWebsocketClientDelegate {
    fun socketIsOpened()
    fun socketIsClosed(code: Int, reason: String, remote: Boolean)
}

/**
 * Created by michaeltasior on 21.12.17.
 */
interface FhemMessageListener {
    fun onMessage(message: FhemMessage)
}