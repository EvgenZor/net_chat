package project.chat.server.gui;

import project.chat.server.core.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// Пользовательский интерфейс чат сервера---------------------------------------------------------------
public class ServerGUI extends JFrame implements ActionListener {

    private final ChatServer chatServer = new ChatServer();
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");

    public static void main(String[] args) {
        new ServerGUI();
    }

    private ServerGUI(){
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(1550, 550, 200, 100);
        setResizable(false);
        setLayout(new GridLayout(1, 2));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        btnStart.addActionListener(this);
        btnStop.addActionListener(this);
        add(btnStart);
        add(btnStop);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if(source == btnStart){
            chatServer.start(8189);
        } else if(source == btnStop){
            chatServer.stop();
        }
    }
}
