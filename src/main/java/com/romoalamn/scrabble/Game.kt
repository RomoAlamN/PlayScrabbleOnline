package com.romoalamn.scrabble

import com.romoalamn.scrabble.math.Vector
import com.romoalamn.scrabble.math.rangeTo
import com.romoalamn.scrabble.serial.BoardSerial
import com.romoalamn.scrabble.server.*
import com.romoalamn.scrabble.ui.GameBoard
import com.romoalamn.scrabble.ui.MainMenu
import com.romoalamn.scrabble.ui.ServerInfo
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ConnectException
import javax.swing.*
import javax.swing.plaf.synth.SynthLookAndFeel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.exitProcess

fun main() {
    try {
        val laf = SynthLookAndFeel()
        laf.load(Game::class.java.getResourceAsStream("laf.xml"), Game::class.java)
        UIManager.setLookAndFeel(laf)
    } catch (except: Exception) {
        return
    }
    startGame()
}

@Throws(ConnectException::class)
fun startGame() {
    val menu = MainMenu()
    menu.startPlay { info ->
        val game: Game
        try {
            game = Game(info)
        } catch (ex: ConnectException) {
            startGame()
            return@startPlay
        }
        game.initFrame()
    }
}

fun getResource(path: String): InputStream {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(path)!!
}

class Game(info: ServerInfo) {

    val server = Server(info)

    // the local player (always exists)
    private var player: Player = Player(Identity("null", 0, -1))
    private val client: Client
    private val clientBoard: Board

    private val peers = ArrayList<Player>();

    init {
        // start server only does anything if hostingGame = true
        server.startServer()
        client = Client(info)
        val serialJson = getResource("board.json")
        val serialBoard = Server.gson.fromJson(InputStreamReader(serialJson), BoardSerial::class.java)
        clientBoard = getBoardFromSerial(serialBoard)

        client.addMessageClient(object : IMessageClient<Client> {
            override fun handleSignal(container: Client, signal: Signal) {
                // Server is asking for introduction
                if (signal.name == Signal.requestIntroductionSignal) {
                    val uid = signal.data.toInt()
                    client.introduce(Identity("ScrabblePlayer", 0, uid))
                    client.giveSignal(IntroductionResponse(client.identity))
                    player = Player(client.identity)
                } else if (signal.name == Signal.connectClientSignal) {
                    val newPeer = Player(Server.gson.fromJson(signal.data, Identity::class.java))
                    if (player.id.uid == newPeer.id.uid) {
                        return
                    }
                    peers.add(newPeer)
                    println("Added Peer.")
                } else if (signal.name == Signal.bootSignal) {
                    container.stop()
                    if (info.hostingGame) return
                    JOptionPane.showMessageDialog(frame, "Server has disconnected you. Reason: ${signal.data}")
                }
            }
        })

    }

    var frame: JFrame? = null
    fun initFrame() {
        SwingUtilities.invokeLater {
            frame = JFrame("Scrabble Online")
            val gameBoard = GameBoard()
            frame!!.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame!!.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(p0: WindowEvent?) {
                    server.stop()
                    client.stop()
                    exitProcess(0)
                }
            })
            frame!!.add(gameBoard.mainPanel)
            gameBoard.gamePanel.add(ScrabbleBoard(clientBoard))
            gameBoard.handPanel.add(HandDisplay(player.hand))
            frame!!.pack()
            frame?.minimumSize = Dimension(500, 800)
            frame!!.isVisible = true
        }
    }
}

class ScrabbleBoard(val board: Board) : JPanel() {
    init{
        minimumSize = Dimension(500,500)
    }
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g !is Graphics2D) return
        val size = min(bounds.width, bounds.height)
        val startX = (width - size) / 2
        val startY = (height - size) / 2

        val origin = Vector(0,0)
        val bSize = Vector(board.width - 1, board.height-1)

        g.color= Color(200,195, 210)
        g.fillRect(startX, startY, size, size)

        val advanceX = size.toDouble() / board.width
        val advanceY = size.toDouble() / board.height
        val background = this.background!!
        g.color = background
        for(i in 0 .. board.width){
            g.drawLine((startX + i * advanceX).toInt(), startY, (startX + i * advanceX).roundToInt(), startY + size)
        }
        for(i in 0 .. board.height){
            g.drawLine(startX, (startY + i * advanceY).roundToInt(), startX + size, (startY+i*advanceY).roundToInt())
        }

        for(v in origin .. bSize){
            val spot = board.getSpot(v)
            val piece = board.getPiece(v)
            // fallback in case no decoration is defined
            if(spot.decorationLocation != "") {
                val parts = spot.name.split(" ")
                for ((i, p) in parts.withIndex()) {
                    g.drawString(p,( v.x * advanceX + startX + 3).roundToInt(), (v.y * advanceY + startY + (i + 1) * 14).roundToInt())
                }
            }
        }

    }
}

class HandDisplay(val hand: Hand) : JPanel() {
    val maxBounds = Dimension(350 * 3, 50 * 3)
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g as Graphics2D
        val h = min(min(bounds.getHeight(), bounds.getWidth()/hand.limit), maxBounds.getHeight())
        val w = min(min(bounds.getHeight() * hand.limit, bounds.getWidth()),maxBounds.getWidth())
        val a = bounds.getWidth()
        // center horizontally
        val start = bounds.getWidth() / 2 - w / 2
        g.color = Color(190,139, 75)
        g.fillRect(start.toInt(), 0, w.toInt(), h.toInt())

    }
}