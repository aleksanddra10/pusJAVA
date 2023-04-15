package pus2022.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.JSONParser;


/**
 *
 * @author jaroc
 */
public class TcpServer {
    
    public static int tcpPort;    
    private final ServerSocket serverSocket;
    public static HashSet<ClientThread> clientsPool = new HashSet<>();    
    
    public TcpServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        Server.logger.log(Level.INFO, "Created TcpServer instance on port {0}", Integer.toString(port));
    }
    
    public void run() {
        for(;;) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientThread clientThread = new ClientThread(clientSocket);
                clientsPool.add(clientThread);
                new Thread(clientThread).start();
            } catch(IOException ex) {
                Server.logger.log(Level.WARNING, "Error ({0}) during accepting client", ex.getMessage());
            }
        }
    }
}