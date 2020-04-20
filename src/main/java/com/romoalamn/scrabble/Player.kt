package com.romoalamn.scrabble

import com.romoalamn.scrabble.server.Client
import com.romoalamn.scrabble.server.Identity

class Player (val id : Identity){
    val username = "Player " + (id.uid +1 )
    val hand : Hand = Hand()
}