package com.romoalamn.scrabble.ui

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.SwingUtilities

class MainMenu {
    private val info : ServerInfo = ServerInfo("localhost", 0,false)
    fun startPlay(callback : (ServerInfo)->Unit) {
        SwingUtilities.invokeLater {
            val frame = JFrame()
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame.setSize(200, 400)
            frame.setLocationRelativeTo(null)
            val prompt = StartGamePrompt()
            val join = JoinGame()
            val host = HostGame()

            setupJoinListeners(join, prompt, frame,callback)
            setupHostListeners(host, prompt, frame,callback)

            frame.add(prompt.mainPanel)
            frame.isVisible = true


            prompt.exitButton.addActionListener {
                frame.isVisible = false
                frame.dispose()
            }
            prompt.hostGameButton.addActionListener {
                // createHost()
                // startGame
                frame.isVisible = false
                frame.contentPane.removeAll()
                frame.add(host.panel1)
                frame.setSize(400, 300)
                frame.isVisible = true
            }
            prompt.joinGameButton.addActionListener {
                // join Game Dialog (Insert Ip and port)
                frame.isVisible = false
                frame.contentPane.removeAll()
                frame.add(join.mainPanel)
                frame.pack()
                frame.isVisible = true
            }
        }
    }
    private fun setupJoinListeners(
        join: JoinGame,
        prompt: StartGamePrompt,
        frame: JFrame,
        cb: (ServerInfo) -> Unit
    ){
        join.joinButton.addActionListener {
            val addr = join.addressTextField.text
            val port = join.portTextField.text.toInt()
            info.host = addr
            info.port = port
            info.hostingGame = false
            frame.isVisible = false
            frame.dispose()
            cb(info)
            //startGame()
        }
        join.cancelButton.addActionListener {
            frame.isVisible = false
            frame.contentPane.removeAll()
            frame.setSize(200,400)
            frame.add(prompt.mainPanel)
            frame.isVisible = true
        }
    }
    private fun setupHostListeners(
        hostGame: HostGame,
        prompt: StartGamePrompt,
        frame: JFrame,
        cb: (ServerInfo) -> Unit
    ){
        hostGame.cancelButton.addActionListener {
            frame.isVisible = false
            frame.contentPane.removeAll()
            frame.setSize(200,400)
            frame.add(prompt.mainPanel)
            frame.isVisible = true
        }
        hostGame.createButton.addActionListener {
            val addr = "127.0.0.1"
            val port = hostGame.portField.text.toInt()
            // remote in this context means:
            // will we listen to events on this end?
            // not whether the host is on another computer
            info.host = addr
            info.port = port
            info.hostingGame = true

            frame.isVisible = false
            frame.dispose()
            cb(info)
        }
    }
}
private fun JButton.removeAllActionListeners(){
    for(al in this.actionListeners){
        this.removeActionListener(al)
    }
}

data class ServerInfo(var host : String, var port : Int, var hostingGame : Boolean)