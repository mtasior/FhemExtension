package de.tasior.fhemExtension.fhem

import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by michaeltasior on 21.12.17.
 */
class FhemMessage {
    var timestamp: Date = Date()
    var moduleType: String = ""
    var moduleName: String = ""
    var attribute: String = ""
    var value: String = ""

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun deserialize(message: String): Set<FhemMessage> {
            var message = message
            val list = mutableSetOf<FhemMessage>()

            //there can be more than one message in a frame
            message = message.replace("\n", "")
                    .replace("\\u003cbr\\u003e", "\n")
                    .replace("<br>", "\n")

            val messages = message.split("\n")
            for (m in messages) {
                val fhemMessage = FhemMessage()
                val messageArray = m.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (messageArray.isEmpty()) continue

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                fhemMessage.timestamp = sdf.parse(messageArray[0] + " " + messageArray[1])
                fhemMessage.moduleType = messageArray[2]
                fhemMessage.moduleName = messageArray[3]
                fhemMessage.attribute = messageArray[4]

                if (fhemMessage.attribute.endsWith(":")) {
                    fhemMessage.attribute = fhemMessage.attribute.substring(0, fhemMessage.attribute.length - 1)
                }

                if (messageArray.size < 6) {
                    fhemMessage.value = fhemMessage.attribute
                    fhemMessage.attribute = "state"
                    list.add(fhemMessage)
                    continue
                }

                var value = messageArray[5]

                if (messageArray.size > 6) {
                    for (messagePart in Arrays.copyOfRange(messageArray, 6, messageArray.size)) {
                        value += " $messagePart"
                    }
                }

                fhemMessage.value = value

                list.add(fhemMessage)
            }

            return list
        }
    }
}
