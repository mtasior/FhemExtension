package de.tasior.fhemExtension.fhem

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

/**
 * This client connects to a FHEMWEB instance with longpoll attribute set to fhemBidirectionalConnection
 */
class FhemWebsocketClient : WebSocketClient {
    private var delegate: FhemWebsocketClientDelegate? = null
    private val listeners = ArrayList<FhemMessageListener>()

    constructor(serverURI: URI) : super(serverURI) {}

    override fun onOpen(handshakedata: ServerHandshake) {
        delegate!!.socketIsOpened()
    }

    override fun onMessage(message: String) {
        val message = FhemMessage.deserialize(message)
        listeners.forEach { listener ->
            message.forEach { message ->
                listener.onMessage(message)
            }
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        if (delegate != null) {
            delegate!!.socketIsClosed(code, reason, remote)
        }
    }

    override fun onError(ex: Exception) {
        if (ex.message != null && ex.message!!.contains("Connection refused")) {
            println("Connection refused, waiting for 60s to try again...")
            try {
                Thread.sleep(60000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return
        }

        ex.printStackTrace()
        // if the error is fatal then onClose will be called additionally
    }

    fun setDelegate(delegate: FhemWebsocketClientDelegate) {
        this.delegate = delegate
    }

    fun setListeners(listeners: ArrayList<FhemMessageListener>) {
        this.listeners.addAll(listeners)
    }

    fun addListener(listener: FhemMessageListener) {
        listeners.add(listener)
    }

}