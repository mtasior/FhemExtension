package de.tasior.fhemExtension.util

import com.google.gson.Gson

/**
 * Created by michaeltasior on 04.01.18.
 *
 * To be used with Gson-decoded classes to have a proper textual output
 */
abstract class Dumpable {
    override fun toString(): String {
        return Gson().toJson(this)

    }
}
