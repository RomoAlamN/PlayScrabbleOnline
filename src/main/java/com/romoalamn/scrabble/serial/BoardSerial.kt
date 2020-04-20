package com.romoalamn.scrabble.serial

import com.google.gson.JsonElement
import java.awt.Dimension
import java.awt.Point

data class BoardSerial(val name : String, val mode : String, val size : Point, val locations : Array<SpotSerial>){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoardSerial

        if (name != other.name) return false
        if (mode != other.mode) return false
        if (size != other.size) return false
        if (!locations.contentEquals(other.locations)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + locations.contentHashCode()
        return result
    }

}
data class SpotSerial(val name : String, val decoration : String, val location : BoardLocationSerial, val effect : String)

data class BoardLocationSerial(val type : String, val location: Array<JsonElement>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoardLocationSerial

        if (type != other.type) return false
        if (!location.contentEquals(other.location)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + location.contentHashCode()
        return result
    }
    companion object{
        val everywhere = BoardLocationSerial("all-np", arrayOf<JsonElement>())
    }
}