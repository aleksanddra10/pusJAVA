package pus2022.server;

import javax.swing.plaf.nimbus.State;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;
import static pus2022.server.Server.logger;

public class User {

    private int id;
    private String email;
    private String login;


    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getLogin() {
        return login;
    }


    /**
     * Create new user, id==-1 means that it was not created
     * 
     * @param email
     * @param password
     * @param login
     */
    public User(String email, String password, String login) {
        this.id = -1;
        this.email = email;
        this.login = login;
        try {
            User uniq = new User(login);
            if(uniq.id > 0) throw new SQLException("User " + login + " already exists");
            PreparedStatement st = Db.connection.prepareStatement("INSERT INTO users (email,password,login) VALUES (?,?,?)");
            st.setString(1, email);
            st.setString(2, password);
            st.setString(3, login);
            st.execute();
            ResultSet rs = st.getGeneratedKeys();
            this.id = rs.getInt(1);
        } catch(SQLException ex) {
            logger.log(Level.SEVERE, "Cannot create new User (" + ex.getMessage() + ")");
        }
    }

    /**
     * Retrieve a user from database using its login
     * 
     * @param login
     */
    public User(String login) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id,email,login FROM users WHERE login=?");
            st.setString(1, login);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = rs.getInt(1);
                this.email = rs.getString(2);
                this.login = rs.getString(3);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
            this.email = null;
            this.login = null;
        }
    }
    public User(int id) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id,email,login FROM users WHERE id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = rs.getInt(1);
                this.email = rs.getString(2);
                this.login = rs.getString(3);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
            this.email = null;
            this.login = null;
        }
    }

    /**
     * Retrieve a user from database using a user/password pair (so validate)
     *
     * @param login
     * @param password
     */
    public User(String login, String password) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id,email,login FROM users WHERE login=? AND password=?");
            st.setString(1, login);
            st.setString(2, password);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = rs.getInt(1);
                this.email = rs.getString(2);
                this.login = rs.getString(3);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
            this.email = null;
            this.login = null;
        }
    }

    /**
     * Count users
     * 
     * @return
     */
    public static int getUsersCount() {
        int result = -1;
        try {
            Statement st = Db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            result = rs.getInt(1);
        } catch(SQLException ex) {}
        return result;
    }

    /**
     * Get all users ids
     * 
     * @return 
     */
    public static int[] getUsersIds() {
        int n = getUsersCount();
        int[] result = new int[n];
        try {
            Statement st = Db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT id FROM users");
            n = 0;
            while(rs.next()) {
                result[n] = rs.getInt(1);
                n++;
            }
        } catch(SQLException ex) {}
        return result;
    }
    public static int getUserId(String login){
        int result = -1;
        try{
            PreparedStatement st = Db.connection.prepareStatement("SELECT id FROM users WHERE login=?");
            st.setString(1, login);
            ResultSet rs = st.executeQuery();
            if(rs.next()) result = rs.getInt(1);
        } catch(SQLException ex) {}
        return result;
    }

    @Override
    public String toString() {
        return login + " <" + email + ">";
    }
}
