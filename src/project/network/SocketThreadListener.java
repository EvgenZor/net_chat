package project.network;

import java.net.Socket;

public interface SocketThreadListener {

    void onStartSocketThread(SocketThread thread, Socket socket);
    void onStopSocketThread(SocketThread thread, Socket socket);

    void onSocketIsReady(SocketThread thread, Socket socket);
    void onReceivedString(SocketThread thread, Socket socket, String value);
}
