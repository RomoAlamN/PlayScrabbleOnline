package com.romoalamn.scrabble.ui;

import com.romoalamn.scrabble.ScrabbleBoard;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class GameBoard {
    public JLabel playerNameLabel;
    public JPanel gamePanel;
    public JPanel mainPanel;
    public JPanel handPanel;
    private JLabel player1Label;
    private JLabel player2Label;
    private JLabel player3Label;
    private JLabel player4Label;
    private JLabel player5Label;

    public JLabel[] getPlayerList(){
        return new JLabel[]{player1Label, player2Label, player3Label, player4Label, player5Label};
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        gamePanel = new JPanel();
        gamePanel.setMinimumSize(new Dimension(500,500));
        gamePanel.setLayout(new BorderLayout());
        handPanel = new JPanel();
        handPanel.setLayout(new BorderLayout());
    }
}
