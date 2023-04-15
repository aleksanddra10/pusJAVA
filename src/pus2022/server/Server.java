package pus2022.server;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author jaroc
 */
public class Server {
    
    public static final String VERSION = "DEV-0.0.1";
    
    public static final Logger logger = Logger.getLogger(TcpServer.class.getName());    
    public static JSONParser jsonParser = new JSONParser();
    public static Db db;
    
    private static JLabel nThreadsLabel, nRegisteredUsers, nMessages;
    private static JTextArea logWindow;
    private static JScrollPane scrollPane;
    
    public static void setThreads(int numThreads) {
        nThreadsLabel.setText("" + numThreads);
    }
    
    public static void setRegisteredUsers(int numRegisteredUsers) {
        nRegisteredUsers.setText("" + numRegisteredUsers);
    }
    
    public static void setMessages(int numMessages) {
        nMessages.setText("" + numMessages);
    }
    
    public static void main(String[] args) {
        
        TcpServer tcpServer = null;
        UdpServer udpServer = null;
        
        try {
            String propFileName = "server.properties";
            Properties props = new Properties();
            props.load(new FileInputStream(propFileName));
            TcpServer.tcpPort = Integer.parseInt(props.getProperty("port"));
            String dbDriver = props.getProperty("dbDriver");
            String dbUrl = props.getProperty("dbUrl");
            if(dbDriver == null || dbUrl == null) throw new SQLException("Driver of Url to database not configured");
            Server.db = new Db(props.getProperty("dbDriver"),
                props.getProperty("dbUrl"),
                props.getProperty("adminLogin"),
                props.getProperty("adminPassword"),
                props.getProperty("adminEmail")
            );
            tcpServer = new TcpServer(TcpServer.tcpPort);
            udpServer = new UdpServer(TcpServer.tcpPort);

        } catch (IOException|ClassNotFoundException|SQLException e) {
            logger.log(Level.SEVERE, "Cannot start: {0}", e.getMessage());
            System.exit(1);
        }

        logger.log(Level.INFO, "Server " + VERSION);

        JFrame mainWindow = new JFrame("Communicator server on port " + TcpServer.tcpPort);
        mainWindow.setSize(800, 500);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container interior = mainWindow.getContentPane();
        interior.setLayout(new BorderLayout());
        Container header = new Panel();
        interior.add(header, BorderLayout.SOUTH);
        header.setLayout(new GridLayout(1, 7));
        header.add(new JLabel("Registered users:", JLabel.RIGHT));
        nRegisteredUsers = new JLabel("0", JLabel.LEFT);
        header.add(nRegisteredUsers);
        header.add(new JLabel("Saved messages:", JLabel.RIGHT));
        nMessages = new JLabel("0", JLabel.LEFT);
        header.add(nMessages);
        header.add(new JLabel("Active threads:", JLabel.RIGHT));
        nThreadsLabel = new JLabel("0", JLabel.LEFT);
        header.add(nThreadsLabel);
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        header.add(exitButton);
        logWindow = new JTextArea();
        logWindow.setEditable(false);
        scrollPane = new JScrollPane(logWindow);
        interior.add(scrollPane, BorderLayout.CENTER);
        Dimension dim = mainWindow.getToolkit().getScreenSize();
        Rectangle abounds = mainWindow.getBounds();
        mainWindow.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
        mainWindow.setVisible(true);
                
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String line = getFormatter().format(record);
                logWindow.append(line);
                scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
            
        };
        
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "%1$tF %1$tT [%2$s] %3$s %n";
            
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(), lr.getMessage());
            }
        });
        
        logger.addHandler(handler);
        logger.log(Level.INFO, "Server " + VERSION + " on port " + TcpServer.tcpPort);
        setRegisteredUsers(User.getUsersCount());
        setMessages(Message.getMessagesCount());

        new Thread(udpServer).start();
        tcpServer.run();
    }
}