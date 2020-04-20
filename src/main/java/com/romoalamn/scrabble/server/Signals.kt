package com.romoalamn.scrabble.server


open class Signal(val name: String, val data: String) {
    override fun toString(): String {
        return Server.gson.toJson(this)
    }

    companion object {
        const val connectClientSignal = "connect-client" // sent on connection
        const val requestIntroductionSignal = "introduction-request" // sent to request an introduction to the server
        const val introductionResponseSignal = "instroduction-response" // sent to introduce yourself
        const val clientDisconnectSignal = "disconnect-client" // sent before disconnection, to avoid an exception
        const val peerDisconnect = "disconnect-peer" // sent when a peer disconnects the server
        const val bootSignal = "boot-client"
        const val error = "error-occurred"

        // Play things, peer took their turn, bagStatus changed, game over, new game, etc.
    }
}

open class JsonSignal<T : Any>(name: String, data: T) : Signal(name, Server.gson.toJson(data))

open class ConnectSignal(identity: Identity) : JsonSignal<Identity>(connectClientSignal, identity)
open class IntroductionSignal(reason: String) : Signal(requestIntroductionSignal, reason)
open class IntroductionResponse(intro: Identity) : JsonSignal<Identity>(introductionResponseSignal, intro)
open class DisconnectSignal(reason: String) : Signal(clientDisconnectSignal, reason)
open class PeerDisconnect(uid : Int) : Signal(peerDisconnect, uid.toString())
open class BootSignal(reason : String) : Signal(bootSignal, reason)

open class ErrorSignal(descriptor: String) : Signal(error, descriptor)
open class FatalErrorSignal(error:String) : DisconnectSignal("Fatal error: $error")