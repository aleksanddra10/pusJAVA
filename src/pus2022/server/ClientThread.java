package pus2022.server;

import java.io.*;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.swing.*;

import static pus2022.server.Server.*;
import static pus2022.server.TcpServer.clientsPool;

/**
 *
 * @author jaroc
 */
public class ClientThread implements Runnable {

    private enum Status{
        LOGGED_ACTIVE,
        LOGGED_INACTIVE,
        NOT_LOGGED
    }
    private final int INACTIVE_TIME = 10; // 10 secs for testing
    private Status status = null;
    private long lastSeen;
    private final Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private UUID uuid;
    private String login = null;
    private String email = null;

    private void setStatus(Status status){
        this.status = status;
    }

    public Status getStatus(){
        return status;
    }

    @SuppressWarnings("unchecked")
    private String getJsonError(String message) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "ERROR");
        jsonObject.put("message", message);
        return jsonObject.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private void doConversation() {
        uuid = UUID.randomUUID();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "WELCOME");
        jsonObject.put("from", socket.getRemoteSocketAddress().toString());
        jsonObject.put("timestamp", new Date().getTime());
        jsonObject.put("uuid", uuid.toString());
        jsonObject.put("message", "You are connected to the server " + VERSION);
        output.println(jsonObject.toJSONString());
        for(;;) {
            try {
                String line = input.readLine();
                if(line == null) break;
                try {
                    jsonObject = (JSONObject) jsonParser.parse(line);
                } catch (ParseException ex) {
                    logger.warning("Error parsing line: " + line);
                    output.println(getJsonError("What??"));
                    continue;
                }
                
                String type = (String) jsonObject.get("type");
                if(type == null) type = "null";
                logger.fine("Request: " + jsonObject);
                switch (type) {
                    case "LOGIN" -> {
                        String gLogin = (String) jsonObject.get("login");
                        String gPassword = (String) jsonObject.get("password");
                        User user = new User(gLogin, gPassword);
                        if (user.getId() < 0) {
                            logger.log(Level.WARNING, "Failed attempt to log in as " + gLogin + " from " + socket.getRemoteSocketAddress());
                            output.println(getJsonError("Login failed"));
                        } else {
                            login = user.getLogin();
                            email = user.getEmail();

                            logger.log(Level.INFO, "User " + login + " logged in from " + socket.getRemoteSocketAddress());

                            jsonObject.clear();
                            jsonObject.put("type", "PROFILE");
                            jsonObject.put("login", login);
                            jsonObject.put("email", email);
                            output.println(jsonObject.toJSONString());

                            setStatus(Status.LOGGED_ACTIVE);
                            lastSeen = System.currentTimeMillis();
                            sendNewUserList(null);
                        }
                    }
                    case "REGISTER" -> {
                        String newLogin = (String) jsonObject.get("login");
                        String newPassword = (String) jsonObject.get("password");
                        String newEmail = (String) jsonObject.get("email");
                        User newUser = new User(newLogin, newPassword, newEmail);
                        if (newUser.getId() < 0) {
                            logger.log(Level.WARNING, "Failed attempt to log in as " + newLogin + " from " + socket.getRemoteSocketAddress());
                            output.println(getJsonError("Login failed"));
                        } else {
                            login = newUser.getLogin();
                            email = newUser.getEmail();

                            logger.log(Level.INFO, "User " + login + " logged in from " + socket.getRemoteSocketAddress());

                            jsonObject.clear();
                            jsonObject.put("type", "PROFILE");
                            jsonObject.put("login", login);
                            jsonObject.put("email", email);
                            output.println(jsonObject.toJSONString());
                            setStatus(Status.LOGGED_ACTIVE);
                            lastSeen = System.currentTimeMillis();
                            sendNewUserList(null);
                        }
                    }
                    case "LOGOUT" -> {
                        login = null;
                        email = null;
                        sendNewUserList(null);
                    }
                    case "UNICAST" -> {
                        if (login == null) {
                            output.println(getJsonError("Not logged in"));
                            break;
                        }
                        String to = (String) jsonObject.get("to");
                        String message = (String) jsonObject.get("message");
                        if (to == null) {
                            output.println(getJsonError("No destination for the unicast"));
                            break;
                        }
                        User sender = new User(login);
                        User receiver = new User(to);
                        if (receiver.getId() <= 0) {
                            output.println(getJsonError("No such receiver"));
                            break;
                        }
                        new Message(sender, receiver, message);
                        setMessages(Message.getMessagesCount());
                        jsonObject.replace("type", "MESSAGE");
                        jsonObject.remove("to");
                        jsonObject.put("from", login);
                        String routedString = jsonObject.toJSONString();
                        for (ClientThread clientThread : TcpServer.clientsPool) {
                            if (clientThread != this && clientThread.login != null && clientThread.login.equals(to)) {
                                clientThread.output.println(routedString);
                                break;
                            }
                        }
                    }
                    case "USERLIST" -> sendNewUserList(output);
                    case "USERSTATUS" -> getStatuses();
                    case "UPDATESTATUS" -> {
                        status = Status.LOGGED_ACTIVE;
                        lastSeen = System.currentTimeMillis();
                    }
                    case "FILTER_MESSAGES" -> {
                        long timestamp = (long)jsonObject.get("lastread");
                        sendUnseenMessages(timestamp);
                    }
                    default -> output.println(getJsonError("Unknown request type: " + type));
                }
            } catch(IOException ex) {
                break;
            }
        }
    }

    private void sendUnseenMessages(long timestamp){
        JSONObject jsonObject = new JSONObject();
        String stringToPrint;
        int receiver = User.getUserId(this.login);
        int sender = -1;
        String message = "";
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT sender,message FROM messages WHERE timestamp > ? AND receiver=?");
            st.setLong(1, timestamp);
            st.setInt(2,receiver);
            ResultSet rs = st.executeQuery();
            while(rs.next()) {
                jsonObject.clear();
                jsonObject.put("type", "MESSAGE");
                sender = rs.getInt(1);
                message = rs.getString(2);
                User us = new User(sender);
                jsonObject.put("from", us.getLogin());
                jsonObject.put("message", message);
                stringToPrint = jsonObject.toJSONString();
                output.println(stringToPrint);

            }
        } catch(SQLException ex) {
            logger.log(Level.WARNING, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void sendNewUserList(PrintWriter output) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "USERLIST");
        JSONArray userList = new JSONArray();
        for(ClientThread clientThread: TcpServer.clientsPool) {
            if(clientThread.login != null) {
                JSONObject loggedUser = new JSONObject();
                loggedUser.put("login", clientThread.login);
                loggedUser.put("email", clientThread.email);
                loggedUser.put("uuid", clientThread.uuid.toString());
                userList.add(loggedUser);
            }
        }
        jsonObject.put("users", userList);
        if(output == null) {
            for (ClientThread clientThread : TcpServer.clientsPool) {
                clientThread.output.println(jsonObject.toJSONString());
            }
        } else {
            output.println(jsonObject.toJSONString());
        }
    }
    @SuppressWarnings("unchecked")
    private void getStatuses(){
        boolean logged = false;
        String stringToPrint;
        for (int id: User.getUsersIds()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "USERSTATUS");
            User newuser = new User(id);
            String userLogin = newuser.getLogin();
            for (ClientThread clientThread: TcpServer.clientsPool) {
                if (userLogin.equals(clientThread.login)) {
                    long currentTime = System.currentTimeMillis();
                    if(TimeUnit.MILLISECONDS.toSeconds(currentTime - clientThread.lastSeen) > INACTIVE_TIME) clientThread.setStatus(Status.LOGGED_INACTIVE);

                    jsonObject.put("status", userLogin + ": " + clientThread.getStatus().toString());
                    stringToPrint = jsonObject.toJSONString();
                    output.println(stringToPrint);
                    logged = true;
                    break;
                }
                logged = false;
            }
            if(!logged){
                jsonObject.put("status", userLogin + ": " + Status.NOT_LOGGED.toString());
                stringToPrint = jsonObject.toJSONString();
                output.println(stringToPrint);
            }
        }
    }

    public ClientThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        clientsPool.add(this);
        Server.setThreads(clientsPool.size());
        try {
            logger.log(Level.INFO, "Client connected: " + socket.getRemoteSocketAddress());
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            doConversation();
            logger.log(Level.INFO, "Client disconnected: " + socket.getRemoteSocketAddress());
            socket.close();
            if(login != null) {
                login = null;
                sendNewUserList(null);
            }
        } catch(IOException ex) {
            logger.log(Level.WARNING, "Error during handling a client socket: " + ex.getMessage());
        }
        clientsPool.remove(this);
        Server.setThreads(clientsPool.size());
    }
}
