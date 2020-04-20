package com.romoalamn.scrabble.server

import java.io.IOException

class DisconnectionException(name : String) : IOException("Client $name has disconnected")