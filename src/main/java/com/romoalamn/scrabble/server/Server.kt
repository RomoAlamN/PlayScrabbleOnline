package com.romoalamn.scrabble.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.romoalamn.scrabble.ui.ServerInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class Server(private val info: ServerInfo) {
    private var server: ServerSocket? = null
    private val clients = ArrayList<Client>()
    private var running = AtomicBoolean(false)

    private var currentUid = 0

    private val messageClients = ArrayList<IMessageClient<Client>>()

    private val clientThreads = ArrayList<Thread>();
    fun stop() {
        running.set(false)
        synchronized(clients) {
            for (cl in clients) {
                if (cl.local) continue
                cl.giveSignal(BootSignal("Server shutting down."))
            }
            for (th in clientThreads) {
                th.interrupt()
            }
        }
        serverThread?.interrupt()
        server?.close()
    }

    var serverThread: Thread? = null
    fun startServer(): Boolean {
        // no reason to start a remote server
        if (!info.hostingGame) return false
        server = ServerSocket(info.port)
//        server?.soTimeout = 1000
        running.set(true)
        serverThread = Thread {
            while (running.get()) {
                val client: Socket
                try {
                    client = server!!.accept()
                } catch (sockEx: SocketException) {
                    // Clean output, because server is supposed to throw an exception when it is closed.
                    // (It's supposed to throw an exception, so we don't print anything)
                    return@Thread
                }
                val player = Client(client)
                synchronized(clients) {
                    clients.add(player)
                }
                addClient(player)
            }
        }
        serverThread!!.start()

        return true
    }

    private fun addClient(newClient: Client): Boolean {
        try {
            newClient.giveSignal(IntroductionSignal((currentUid++).toString()))
        } catch (disconnect: DisconnectionException) {
            // fastest disconnect ever ?
            handleDisconnect(newClient)
            return false // just in case I need to read the status afterward?
        }
        do {
            val response: Signal
            try {
                response = newClient.awaitInputPacket()
            } catch (disconnect: DisconnectionException) {
                // encountered error while attempting to read the packet
                handleDisconnect(newClient)
                return false
            }
            if (response.name == Signal.introductionResponseSignal) {
                newClient.introduce(gson.fromJson(response.data, Identity::class.java))
//                println("Hell's Bells Batman")
            } else {
                newClient.giveSignal(IntroductionSignal("new-connection"))
            }
        } while (!newClient.introduced)
        synchronized(clients) {
            for (activeClient in clients) {
                if (activeClient == newClient) continue
                activeClient.giveSignal(ConnectSignal(newClient.identity))
                activeClient.giveSignal(ConnectSignal(activeClient.identity))
            }
        }
        clientThreads.add(Thread {
            while (running.get()) {
                val signal = newClient.awaitInputPacket()
                synchronized(messageClients) {
                    for (messageClient in messageClients) {
                        messageClient.handleSignal(newClient, signal)
                    }
                }
            }
        })
        return true
    }

    private fun handleDisconnect(disconnectedClient: Client) {
        synchronized(clients) {
            clients.remove(disconnectedClient)
            for (activeClient in clients) {
                activeClient.giveSignal(PeerDisconnect(disconnectedClient.identity.uid))
            }
        }
    }

    fun addMessageClient(messageClient: IMessageClient<Client>) {
        messageClients.add(messageClient)
    }

    fun removeMessageClient(messageClient: IMessageClient<Client>): Boolean {
        return messageClients.remove(messageClient)
    }

    companion object {
        val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    }

}

class Client(private val client: Socket, val local: Boolean = false) : Comparable<Client> {
    override fun compareTo(other: Client): Int {
        if (identity.uid < other.identity.uid) {
            return -1
        } else if (identity.uid > other.identity.uid) {
            return 1
        }
        return 0
    }

    private val inputStream = BufferedReader(InputStreamReader(client.getInputStream()))
    private val outputStream = PrintStream(client.getOutputStream())

    private val messageClients = ArrayList<IMessageClient<Client>>()

    var introduced = false
    lateinit var identity: Identity

    private val gson = Server.gson

    private val clientThread: Thread

    private val connected = false

    private val running = AtomicBoolean(true)
    fun stop() {
        running.set(false)
        clientThread.interrupt()
    }

    init {
        clientThread = Thread {
            synchronized(messageClients) {
                while (running.get()) {
                    val signal = awaitInputPacket()
                    for (messageClient in messageClients) {
                        messageClient.handleSignal(this@Client, signal)
                    }
                }
            }
        }
        if (local) {
            clientThread.start()
        }
    }

    @Throws(DisconnectionException::class)
    fun giveSignal(signal: Signal) {
        checkConnection()
        try {
            outputStream.println(signal.toString())
            outputStream.flush()
        } catch (except: SocketException) {
            throw DisconnectionException(identity.name)
        }
    }

    fun awaitInputPacket(): Signal {
        checkConnection()
        var ret: Signal? = null
        try {
            ret = gson.fromJson(inputStream.readLine(), Signal::class.java)
        } catch (except: SocketException) {
            throw DisconnectionException(identity.name)
        }
        return ret!!
    }

    private fun checkConnection() {
        if (client.isClosed) {
            throw DisconnectionException(identity.name)
        }
    }

    fun introduce(data: Identity) {
        identity = data
        introduced = true
    }

    constructor(host: String, port: Int, local: Boolean = true) : this(Socket(host, port), local)
    constructor(info: ServerInfo) : this(info.host, info.port, true)

    // receives messages for us
    fun addMessageClient(messageClient: IMessageClient<Client>) {
        messageClients.add(messageClient)
    }

    fun removeMessageClient(messageClient: IMessageClient<Client>): Boolean {
        return messageClients.remove(messageClient)
    }
}

interface IMessageClient<T : Any> {
    fun handleSignal(container: T, signal: Signal)
}