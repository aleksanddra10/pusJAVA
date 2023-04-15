package pus2022.client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

import org.json.simple.parser.ParseException;

import static pus2022.server.Server.jsonParser;
import static pus2022.server.Server.logger;

public class ClientUI extends JFrame implements ActionListener, KeyListener, WindowListener, Runnable {

    public static final String VERSION = "DEV-0.0.1";

    private InetAddress addr;
    private int port;
    private String connectTo = null;

    private String uuid;

    //UI
    private final JTextField input;
    private final ArrayList<String> history = new ArrayList<>();
    private int historyPos = 0;
    private final JScrollPane scroller;
    private final JTextArea mainPanel;
    private final JButton buttonOk;
    //

    private PrintWriter out = null;
    private BufferedReader in = null;
    private DatagramSocket datagramSocket;
    private String dbUrl;
    private static Db db = null;
    private String login = null;
    private String currentLogin = null;
    private String currentEmail = null;
    private HashSet<User> currentUserList = new HashSet<>();
    private String password = null;
    private JMenu menuConnection;
    private JLabel loginLabel;

    private ClientUI(String title) {
        super(title);
        ClientUI self = this;
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container interior = getContentPane();
        interior.setLayout(new BorderLayout());

        JMenuBar menu = new JMenuBar();
        JMenu menuFile = new JMenu("File");
        JMenuItem menuFileAbout = new JMenuItem("About");
        menu.add(menuFile);
        menuFile.add(menuFileAbout);
        JMenuItem menuFileExit = new JMenuItem("Exit");
        menuFile.add(menuFileExit);
        menuConnection = new JMenu("Connection");
        JMenuItem menuConnectionLogin = new JMenuItem("Login");
        menuConnection.add(menuConnectionLogin);
        JMenuItem menuConnectionRegister = new JMenuItem("Register");
        menuConnection.add(menuConnectionRegister);
        JMenuItem menuConnectionLogout = new JMenuItem("Logout");
        menuConnection.add(menuConnectionLogout);
        JMenuItem menuConnectionSelect = new JMenuItem("Select user");
        menuConnection.add(menuConnectionSelect);
        JMenuItem menuConnectionAbout = new JMenuItem("About connection");
        menuConnection.add(menuConnectionAbout);
        menu.add(menuConnection);
        interior.add(menu, BorderLayout.NORTH);

        menuFileAbout.addActionListener(self);
        menuFileExit.addActionListener(self);
        menuConnectionLogin.addActionListener(self);
        menuConnectionRegister.addActionListener(self);
        menuConnectionLogout.addActionListener(self);
        menuConnectionSelect.addActionListener(self);
        menuConnectionAbout.addActionListener(self);

        mainPanel = new JTextArea();
        mainPanel.setEditable(false);
        scroller = new JScrollPane(mainPanel);
        interior.add(scroller, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JPanel bottomLeftPanel = new JPanel();
        loginLabel = new JLabel();
        bottomLeftPanel.add(loginLabel);
        bottomPanel.add(bottomLeftPanel, BorderLayout.WEST);
        input = new JTextField();
        bottomPanel.add(input, BorderLayout.CENTER);
        buttonOk = new JButton("OK");
        buttonOk.addActionListener(self);
        input.addKeyListener(self);
        bottomPanel.add(buttonOk, BorderLayout.EAST);
        interior.add(bottomPanel, BorderLayout.SOUTH);
        addWindowListener(self);
        Dimension dim = getToolkit().getScreenSize();
        Rectangle aBounds = getBounds();
        setLocation((dim.width - aBounds.width) / 2, (dim.height - aBounds.height) / 2);
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                if (historyPos > 0) {
                    historyPos--;
                    input.setText(history.get(historyPos));
                }
                break;
            case KeyEvent.VK_DOWN:
                if (historyPos < history.size() - 1) {
                    historyPos++;
                    input.setText(history.get(historyPos));
                } else {
                    historyPos = history.size();
                    input.setText("");
                }
                break;
            case KeyEvent.VK_ENTER:
                buttonOk.doClick();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if(login == null) return;

        JSONObject result = new JSONObject();
        result.put("type", "UPDATESTATUS");
        result.put("login", login);

        out.println(result.toJSONString());

    }
    @Override
    public void windowClosed(WindowEvent e) {}
    @Override
    public void windowClosing(WindowEvent e) {}
    @Override
    public void windowActivated(WindowEvent e) {}
    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {
        input.requestFocus();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String action = ae.getActionCommand();
        switch (action) {
            case "OK" -> {
                String s = input.getText();
                if (s.equals("")) {
                    return;
                }
                try {
                    printlnToPanel("→ " + s);
                    out.println(s);
                    history.add(s);
                    historyPos = history.size();
                } catch (Exception e) {
                }
                input.setText(null);
            }
            case "About" -> infoMessageBox("ClientUI " + VERSION);
            case "Exit" -> System.exit(0);
            case "Login" -> {
                JSONObject loginRequest = loginDialog();
                if (loginRequest != null) {
                    out.println(loginRequest.toJSONString());
                }
            }
            case "Register" -> {
                JSONObject registerRequest = registerDialog();
                if (registerRequest != null) {
                    out.println(registerRequest.toJSONString());
                }
            }
            case "Logout" -> logout();
            case "Select user" -> {
                User interlocutor = selectUserDialog();
                printlnToPanel("→ New interlocutor " + interlocutor);
            }
            case "About connection" -> {
                String aboutStr = "Connection to: " + addr + ":" + port + "\nConnection uuid: " + uuid;
                if (currentLogin != null) {
                    aboutStr += "\nLogin as: " + currentLogin + "<" + currentEmail + ">";
                }
                infoMessageBox(aboutStr);
            }
        }
    }

    private void printlnToPanel(String s) {
        printToPanel(s + "\n");
    }

    private void printToPanel(String s) {
        mainPanel.append(s);
        scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum() + 1);
    }

    private void setLoginLabel() {

        if(currentLogin != null && currentLogin.length() > 0) {
            loginLabel.setText(currentLogin);
            input.setEnabled(true);
            buttonOk.setEnabled(true);
                try {
                    db = new Db(dbUrl + "-" + currentLogin);
                } catch(SQLException | ClassNotFoundException ex) {
                    // użytkownik nie może użyć swojej lokalnej klienckiej bazy danych
                    infoMessageBox(ex.getMessage());
            }
        } else {
            loginLabel.setText("");
            input.setEnabled(false);
            buttonOk.setEnabled(false);
            try{
                if(db != null) db.connection.close();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Closing db failed (" + ex.getMessage() + ")");
            }
            db = null;
        }

    }

    @Override
    public void run() {
        for (;;) {
            String line = null;
            try {
                if (in == null) {
                    connect();
                }
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Connection closed by the server");
                }
                JSONObject jsonObject = (JSONObject) jsonParser.parse(line);
                String type = (String) jsonObject.get("type");
                if(type == null) type = "null";
                switch (type) {
                    case "WELCOME" -> {
                        uuid = (String) jsonObject.get("uuid");
                        printlnToPanel("← " + jsonObject.get("message").toString());
                        JSONObject welcome = new JSONObject();
                        welcome.put("uuid", uuid);
                        String welcomeStr = welcome.toJSONString();
                        DatagramPacket welcomePacket = new DatagramPacket(welcomeStr.getBytes(), welcomeStr.length(), addr, port);
                        datagramSocket.send(welcomePacket);
                    }
                    case "PROFILE" -> {
                        currentLogin = (String) jsonObject.get("login");
                        currentEmail = (String) jsonObject.get("email");
                        setLoginLabel();

                        JSONObject messagesRequest = sendTimeRequestMessages();
                        out.println(messagesRequest.toJSONString());
                        updateLastSeenTime();
                    }
                    case "USERLIST" -> {
                        currentUserList.clear();
                        JSONArray userList = (JSONArray) jsonObject.get("users");
                        for (Object userObj : userList) {
                            if (userObj instanceof JSONObject) {
                                String login = (String) ((JSONObject) userObj).get("login");
                                String email = (String) ((JSONObject) userObj).get("email");
                                User user = new User(login, email);
                                currentUserList.add(user);
                            }
                        }
                    }
                    case "MESSAGE" -> {
                        String sender = jsonObject.get("from").toString();
                        String mess = jsonObject.get("message").toString();
                        if (sender == null) return;
                        printlnToPanel("← MESSAGE FROM " + sender + ": " + mess);
                        updateLastSeenTime();
                    }
                    case "USERSTATUS" -> printlnToPanel(jsonObject.get("status").toString());
                    case "ERROR" -> printlnToPanel("← ERROR: " + jsonObject.get("message").toString());
                    default -> throw new UnknownError("Unknown message type \"" + type + "\"");
                }
            } catch(UnknownError ex0) {
                printlnToPanel("← MSGERROR: " + ex0.getMessage());
            } catch(ParseException ex1) {
                printlnToPanel("← JSONERROR: " + ex1 + " in " + line);
            } catch (IOException ex2) {
                printlnToPanel("← IOERROR: " + ex2.getMessage());
                in = null;
                out = null;
                currentLogin = null;
                currentEmail = null;
                setLoginLabel();
            }
        }
    }

    private static int numOfDots = 1;
    
    private void connect() {
        for(;;) {
            String dots = new String(new char[numOfDots]).replace('\0', '.');
            setTitle("Connecting to " + connectTo + dots);
            menuConnection.setEnabled(false);
            currentLogin = null;
            currentEmail = null;
            setLoginLabel();
            try {
                Socket sock = new Socket(addr.getHostName(), port);
                out = new PrintWriter(sock.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                setTitle("Connected to " + connectTo);
                logout();
                menuConnection.setEnabled(true);
                break;
            } catch(IOException ex) {
                numOfDots++;
                if(numOfDots > 10) numOfDots = 1;            
            }
        }
    }
    private JSONObject sendTimeRequestMessages(){
        long time = 0;
        try{
            Statement st = db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT lastread FROM lastseen");
            if(rs.next()) time = rs.getLong(1);

        } catch(SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        JSONObject result = new JSONObject();
        result.put("type", "FILTER_MESSAGES");
        result.put("lastread", time);
        return result;
    }
    private void updateLastSeenTime(){
        long timestamp = System.currentTimeMillis();
        try{
            PreparedStatement st2 = db.connection.prepareStatement("UPDATE lastseen SET lastread = ? WHERE id=1");
            st2.setLong(1, timestamp);
            st2.execute();
        }
        catch(SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }

    }
    private JSONObject loginDialog() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Login", SwingConstants.RIGHT));
        label.add(new JLabel("Password", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField loginField = new JTextField();
        loginField.setText(login);
        controls.add(loginField);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setText(password);
        controls.add(passwordField);
        panel.add(controls, BorderLayout.CENTER);

        if(JOptionPane.showConfirmDialog(this, panel, "Login", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;

        login = loginField.getText();
        password = new String(passwordField.getPassword());

        JSONObject result = new JSONObject();
        result.put("type", "LOGIN");
        result.put("login", login);
        result.put("password", password);
        return result;
    }
    private JSONObject registerDialog() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Login", SwingConstants.RIGHT));
        label.add(new JLabel("Password", SwingConstants.RIGHT));
        label.add(new JLabel("E-mail", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField loginField = new JTextField();
        controls.add(loginField);
        JPasswordField passwordField = new JPasswordField();
        controls.add(passwordField);
        JTextField emailField = new JTextField();
        controls.add(emailField);
        panel.add(controls, BorderLayout.CENTER);

        if(JOptionPane.showConfirmDialog(this, panel, "Register", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;

        login = loginField.getText();
        password = new String(passwordField.getPassword());
        String email = emailField.getText();

        JSONObject result = new JSONObject();
        result.put("type", "REGISTER");
        result.put("login", login);
        result.put("password", password);
        result.put("email", email);
        return result;
    }

    private void logout() {
        currentLogin = null;
        currentEmail = null;
        setLoginLabel();
        JSONObject result = new JSONObject();
        result.put("type", "LOGOUT");
        out.println(result);
    }

    private User selectUserDialog() {
        if(currentUserList.size() == 0) {
            infoMessageBox("No user to select");
            return null;
        }

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Users", SwingConstants.CENTER));
        panel.add(label, BorderLayout.NORTH);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        User[] userArray = new User[currentUserList.size()];
        currentUserList.toArray(userArray);
        JList<User> userList = new JList<>(userArray);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setSelectedIndex(0);
        controls.add(userList);
        panel.add(controls, BorderLayout.CENTER);

        if(JOptionPane.showConfirmDialog(this, panel, "Select user", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;

        return userList.getSelectedValue();
    }

    private static void infoMessageBox(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void errorMessageBox(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {

        try {
            String propFileName = "clientUI.properties";
            Properties props = new Properties();
            props.load(new FileInputStream(propFileName));
            ClientUI mainWindow = new ClientUI("Communicator client");
            mainWindow.login = props.getProperty("login");
            mainWindow.password = props.getProperty("password");
            mainWindow.addr = InetAddress.getByName(props.getProperty("host"));
            mainWindow.port = Integer.parseInt(props.getProperty("port"));
            Class.forName(props.getProperty("dbDriver"));
            mainWindow.dbUrl = props.getProperty("dbUrl");
            mainWindow.connectTo = mainWindow.addr.getHostAddress() + ":" + mainWindow.port;
            mainWindow.datagramSocket = new DatagramSocket();
            mainWindow.setVisible(true);
            new Thread(mainWindow).start();
        } catch(Exception ex) {
            errorMessageBox("Error: " + ex.getMessage());
            System.exit(1);
        }
    }
}