package com.romoalamn.scrabble

import com.romoalamn.scrabble.math.Vector
import com.romoalamn.scrabble.math.rangeTo
import com.romoalamn.scrabble.parser.ScrabbleParser
import com.romoalamn.scrabble.serial.BoardLocationSerial
import com.romoalamn.scrabble.serial.BoardSerial
import com.romoalamn.scrabble.serial.SpotSerial
import com.romoalamn.scrabble.server.Server
import com.romoalamn.scrabble.server.Client
import java.io.InputStreamReader
import kotlin.math.ceil
import kotlin.random.Random

class Board(val width: Int, val height: Int) {
    val pieces = array2d(width, height, nullPiece)
    val spots = array2d(width, height, Spot(SpotSerial("", "", BoardLocationSerial.everywhere, "#null#")))

    fun placePiece(x: Int, y: Int, piece: Piece) {
        pieces[x][y] = piece
    }

    fun setSpot(x: Int, y: Int, sp: Spot) {
        spots[x][y] = sp
    }

    fun getPiece(x: Int, y: Int): Piece {
        return pieces[x][y]
    }

    fun getPiece(v: Vector): Piece {
        return getPiece(v.x, v.y)
    }

    fun getSpot(x: Int, y: Int): Spot {
        return spots[x][y]
    }

    fun getSpot(v: Vector): Spot {
        return getSpot(v.x, v.y)
    }


}

inline fun <reified T : Any> mirror90(arr: Array<Array<T>>, ignoreCond: (T) -> Boolean = { false }) {
    // top left quad is filled in, everything else is ignored (read: undefined)
    val sizeX = ceil(arr.size / 2.0).toInt()
    val sizeY = ceil(arr[0].size / 2.0).toInt()
    for (i in 0..sizeX) {
        for (j in 0..sizeY) {
            // mirror ltr
            val ref = arr[i][j]

            arr[arr.size - 1 - i][j] = ref
            // mirror lt t br
            arr[arr.size - 1 - i][arr[i].size - 1 - j] = ref
            // mirror ttb
            arr[i][arr[i].size - 1 - j] = ref
        }
    }
}

fun mirrorEdges() {

}

fun getBoardFromSerial(serial: BoardSerial): Board {
    val b = Board(serial.size.x, serial.size.y)
    val newArrays = ArrayList<Array<Array<Spot>>>()
    for (loc in serial.locations) {
        val sp = Spot(loc)
        val newArray = array2d(serial.size.x, serial.size.y, b.spots[0][0])
        when (loc.location.type) {
            "random" -> {
                for (v in Vector(0, 0)..Vector(b.width, b.height)) {
                    if (Random.nextBoolean()) {
                        newArray[v.x][v.y] = sp
                    }
                }
            }
            "quad" -> {
                val locat = loc.location.location
                for (locationJson in locat) {
                    newArray[locationJson.asJsonObject["x"].asInt][locationJson.asJsonObject["y"].asInt] = sp
                }
                mirror90(newArray)
            }
            // don't cut yourself with that
            "edge" -> {
                val locat = loc.location.location
                for (locationJson in locat) {
                    val dist = locationJson.asInt
                    newArray[dist][0] = sp
                    newArray[0][dist] = sp
                    newArray[b.width - 1 - dist][b.height - 1] = sp
                    newArray[b.width - 1][b.height -1 - dist] = sp
                }
            }
            "diag" -> {
                val locat = loc.location.location
                for (locationJson in locat) {
                    val dist = locationJson.asInt
                    newArray[dist][dist] = sp
                    newArray[dist][b.height - dist - 1] = sp
                    newArray[b.width - 1 - dist][b.height - 1 - dist] = sp
                    newArray[b.width - 1 - dist][dist] = sp
                }
            }
        }
        newArrays.add(newArray)
    }
    val ign = b.getSpot(0,0)
    for (v in Vector(0, 0) .. Vector(b.width -1, b.height-1)) {
        for (arr in newArrays) {
            if(b.getSpot(v.x, v.y ) == ign) {
                b.setSpot(v.x, v.y, arr[v.x][v.y])
            }
        }
    }
    return b
}

fun fromEffectString(effect: String): (Score) -> Unit {
    val parser = ScrabbleParser('#', effect)
    return { score ->
        parser.setAssociation("word", score.wordScore)
        parser.setAssociation("letter", score.letterScore)
        parser.parse()
        score.wordScore = parser.getAssociation("word")
        score.letterScore = parser.getAssociation("letter")
    }
}

class Spot(spotSerial: SpotSerial) {
    val name = spotSerial.name
    val decorationLocation = spotSerial.decoration
    val effect = fromEffectString(spotSerial.effect)
    override fun hashCode(): Int {
        var result = name.hashCode()
        result =  decorationLocation.hashCode() + 31 * result
        result = effect.hashCode() + 31 * result
        return result
    }

    override fun toString(): String {
        return "Spot{$name}"
    }
}

class Score(w: Word, piece: Piece) {
    var wordScore = w.score
    var letterScore = piece.value
}

class Word(val pieces: Array<Piece>) {
    val score: Int get() = pieces.sumBy { it.value }
}

inline fun <reified T : Any> array2d(w: Int, h: Int, a: (Int, Int) -> T): Array<Array<T>> {
    return Array(w) { x -> Array(h) { y -> a(x, y) } }
}

inline fun <reified T : Any> array2d(w: Int, h: Int, a: T): Array<Array<T>> {
    return Array(w) { Array(h) { a } }
}

inline fun <reified T : Any> array2d(w: Int, h: Int, a: (Int) -> T): Array<Array<T>> {
    var i = 0;
    return Array(w) { Array(h) { a(i++) } }
}


class Hand {
    val pieces = Array(7) { nullPiece }
    val limit get() = 7
    /**
     * Placeholder for later, when you can have skins probably
     */
    val decoration get()=""
}

val nullPiece = Piece("_", -1)

class Pouch {
    private val pieces = ArrayList<Piece>()
    private val deadPieces = ArrayList<Piece>()

    init {
        val piecesStream = Pouch::class.java.classLoader.getResourceAsStream("pieces.json")!!
        val allPieces = Server.gson.fromJson(InputStreamReader(piecesStream), Pieces::class.java)
        for (piece in allPieces.pieces) {
            for (no in 0 until piece.quantity) {
                pieces.add(Piece(piece.letter, piece.value))
            }
        }
    }

    fun getPiece(): Piece {
        if (pieces.size == 0) return nullPiece
        val index = Random.nextInt(pieces.lastIndex)
        val piece = pieces.removeAt(index)
        deadPieces.add(piece)
        return piece
    }

}

data class Piece(val letter: String, val value: Int)

class Pieces {
    val pieces = ArrayList<PieceDefinition>()
}

class PieceDefinition {
    val letter = " "
    val quantity = 0
    val value = 0
}