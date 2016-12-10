package project.chat.server.core;

import project.network.ServerSocketThread;
import project.network.ServerSocketThreadListener;
import project.network.SocketThread;
import project.network.SocketThreadListener;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    public static final String CMD_SYNC_USERS = "/sync_all_users";
    public static final String CMD_DELIMITER = ";";
    public static final String CMD_EXIT = "/exit";
    public static final String CMD_AUTH = "/auth";
    public static final String CMD_NICK = "/nick";
    private ServerSocketThread serverSocketThread;
    private final Vector<ChatSocketThread> clients = new Vector<>();
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss: ");

    // Запуск потока чат сервера с подключением к базе пользователей---------------------------------------------------
    public void start(int port){
        if(serverSocketThread != null && serverSocketThread.isAlive()){
            System.out.println("Сервер уже запущен.");
            return;
        }
        serverSocketThread = new ServerSocketThread("ServerSocketThread", this, port, 10000);
        SQLClient.connect();
    }
    // Остановка потока чат сервера с отключением базы-----------------------------------------------------------------
    public void stop(){
        if(serverSocketThread == null || !serverSocketThread.isAlive()){
            System.out.println("Сервер уже остановлен.");
            return;
        }
        serverSocketThread.interrupt();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).close();
        }
        SQLClient.disconnect();
    }
// ----------------------Лог-----------------------------------------------------------------------------------------
    private synchronized void putLog(Thread thread, String msg){
        System.out.println(thread.getName() + ": " + msg);
    }

    //Обработчик событий ServerSocketThread------------------------------------------------------------------------------------
    @Override //При старте потока сервера-------------------------------------------------------------------------
    public void onStartServerThread(ServerSocketThread thread) {
        putLog(thread, "started.");
    }

    @Override //При остановке потока сервера-------------------------------------------------------------------------
    public void onStopServerThread(ServerSocketThread thread) {
        putLog(thread, "stopped.");
    }

    @Override // При создании сокета------------------------------------------------------------------------------
    public void onCreateServerSocket(ServerSocketThread thread, ServerSocket serverSocket) {
        putLog(thread, "onCreateServerSocket " + serverSocket);
    }

    @Override // По готовности сокета----------------------------------------------------------------------------
    public void onAcceptedSocket(ServerSocketThread thread, Socket socket) {
        putLog(thread, "Client connected " + socket);
        String threadName = "SocketThread: " + socket.getInetAddress() + ":" + socket.getPort();
        new ChatSocketThread(threadName, this, socket);
    }

    @Override
    public void onTimeOutSocket(ServerSocketThread thread, ServerSocket serverSocket) {
//        putLog(thread, "onTimeOutSocket.");
    }

    //События сокета-----------------------------------------------------------------------------
    @Override // При создании потока сокета------------------------------------------------------------
    public synchronized void onStartSocketThread(SocketThread thread, Socket socket) {
        putLog(thread, "started " + socket);
    }

    @Override // При остановке потока сокета------------------------------------------------------------
    public synchronized void onStopSocketThread(SocketThread thread, Socket socket) {
        putLog(thread, "stopped " + socket);
        ChatSocketThread client = (ChatSocketThread) thread;
        if(!clients.remove(client))
            throw new RuntimeException("Не удалось удалсть поток: " + thread);
        if(client.isAuthorized()){
            if(client.isReconnected()) {
                sendBroadcastMsg(client.getNick() + ": reconnected.", true);
            } else {
                sendBroadcastMsg(client.getNick() + ": disconnected.", true);
            }
            sendBroadcastMsg(getAllUsersMsg(), false);
        }
    }

    @Override // По готовности сокета-------------------------------------------------------------------------
    public synchronized void onSocketIsReady(SocketThread thread, Socket socket) {
        putLog(thread, "Socket is ready " + socket);
        clients.add((ChatSocketThread) thread);
    }

    @Override // При получении новой строки ------------------------------------------------------------------
    public synchronized void onReceivedString(SocketThread thread, Socket socket, String value) {
        ChatSocketThread chatSocketThread = (ChatSocketThread) thread;
        if(!chatSocketThread.isAuthorized()){
            handleNonAuthorizeMsg(chatSocketThread, value);
            return;
        }
        if (handleMessage((ChatSocketThread) thread, value)) return;
        sendBroadcastMsg(chatSocketThread.getNick() + ": " + value, true);
    }
// Обработка авторизации пользователя------------------------------------------------------
    private void handleNonAuthorizeMsg(ChatSocketThread thread, String value) {
        String[] arr = value.split(ChatServer.CMD_DELIMITER);
        if (arr.length != 3 || !arr[0].equals(CMD_AUTH)) {
            thread.sendMsg("Authorization error.");
            thread.close();
        } else {
            String nick = SQLClient.getNick(arr[1], arr[2]);
            if (nick == null) {
                thread.sendMsg("Authorization error.");
                thread.close();
            } else {
                ChatSocketThread client = findClientByNick(nick);
                if(client != null){
                    client.sendMsg("Повторная авторизация.");
                    client.setReconnected(true);
                    client.close();
                    thread.setNick(nick);
                    thread.setAuthorized(true);
                } else {
                    thread.setNick(nick);
                    thread.setAuthorized(true);
                    sendBroadcastMsg(nick + " connected.", true);
                }
                thread.sendMsg(CMD_NICK + CMD_DELIMITER + nick);
                sendBroadcastMsg(getAllUsersMsg(), false);
            }
        }
    }

    private ChatSocketThread findClientByNick(String nick){ // Поиск пользователя по нику------------------
        for (int i = 0; i < clients.size(); i++) {
            ChatSocketThread client = clients.get(i);
            if(!client.isAuthorized()) continue;
            if(nick.equals(client.getNick())) return client;
        }
        return null;
    }

    private String getAllUsersMsg(){
        StringBuilder sb = new StringBuilder(CMD_SYNC_USERS);
        final int cnt = clients.size();
        for (int i = 0; i < cnt; i++) {
            ChatSocketThread client = clients.get(i);
            if(!client.isAuthorized()) continue;
            sb.append(CMD_DELIMITER).append(client.getNick());
        }
        return sb.toString();
    }

    private void sendBroadcastMsg(String msg, boolean addTime){
        if(addTime){
            msg = dateFormat.format(System.currentTimeMillis()) + msg;
        }
        for (int i = 0; i < clients.size(); i++) {
            ChatSocketThread client = clients.get(i);
            if(client.isAuthorized()) client.sendMsg(msg);
        }
    }

    private boolean handleMessage(ChatSocketThread thread, String value) {
        if(value.length() < 2 || value.charAt(0) != '/') return false;
        String[] arr = value.split(ChatServer.CMD_DELIMITER);
        switch (arr[0]) {
            case CMD_EXIT:
                thread.close();
                return true;
            default:
                return false;
        }
    }
}
