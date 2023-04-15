package pus2022.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import static pus2022.server.Server.logger;

public class Message {
    private int id;
    private long timestamp;
    private User sender;
    private User receiver;
    private String message;

    /**
     * Create new message, id==-1 means that it was not created
     *
     * @param sender
     * @param receiver
     * @param message
     */
    public Message(User sender, User receiver, String message) {
        this.id = -1;
        this.timestamp = System.currentTimeMillis();
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        try {
            PreparedStatement st = Db.connection.prepareStatement("INSERT INTO messages (timestamp,sender,receiver,message) VALUES (?,?,?,?)");
            st.setLong(1, timestamp);
            st.setInt(2, sender.getId());
            st.setInt(3, receiver.getId());
            st.setString(4, message);
            st.execute();
            ResultSet rs = st.getGeneratedKeys();
            this.id = rs.getInt(1);
        } catch(SQLException ex) {
            logger.log(Level.SEVERE, "Cannot create new Message (" + ex.getMessage() + ")");
        }
    }

    public static int getMessagesCount() {
        int result = -1;
        try {
            Statement st = Db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM messages");
            rs.next();
            result = rs.getInt(1);
        } catch(SQLException ex) {}
        return result;
    }

    @Override
    public String toString() {
        return "(" + new Date(timestamp) + ") " + sender + " -> " + receiver + ": " + message;
    }
}