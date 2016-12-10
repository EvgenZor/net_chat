package project.chat.server.core;

import java.sql.*;

class SQLClient {

    private static Connection connection;
    private static Statement statement;

    // Установка соединения с SQL базой-----------------------------------------------------------------
    static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat_db.sqlite");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
    // Разрыв соединения с SQL базой-----------------------------------------------------------------
    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static String getNick(String login, String pass){
        try {
            String request = "SELECT nickname FROM users WHERE login='" +
                    login + "' AND password='" + pass + "'";
            ResultSet resultSet = statement.executeQuery(request);
            if(resultSet.next()){
                String result = resultSet.getString(1);
                resultSet.close();
                return result;
            } else {
                resultSet.close();
                return null;
            }
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}
