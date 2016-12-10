package project.chat.server.core;

import project.network.SocketThread;
import project.network.SocketThreadListener;

import java.net.Socket;

class ChatSocketThread extends SocketThread {

    private String nick;
    private boolean authorized;
    private boolean reconnected;

    ChatSocketThread(String name, SocketThreadListener eventListener, Socket socket) {
        super(name, eventListener, socket);
    }

    boolean isAuthorized() {
        return authorized;
    }

    void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    String getNick() {
        return nick;
    }

    void setNick(String nick) {
        this.nick = nick;
    }

    void setReconnected(boolean reconnected){
        this.reconnected = reconnected;
    }

    boolean isReconnected(){
        return reconnected;
    }
}
