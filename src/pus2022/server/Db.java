package pus2022.server;

import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import static pus2022.server.Server.logger;

public class Db {
    public static Connection connection = null;
    public Db(String dbDriver, String dbUrl, String adminLogin, String adminPassword, String adminEmail) throws IOException, SQLException, ClassNotFoundException {
        if(connection == null) {
            Class.forName(dbDriver);
            connection = DriverManager.getConnection(dbUrl);
            Statement st = connection.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, email VARCHAR(40), password VARCHAR(40), login VARCHAR(20))");
            int usersCount = User.getUsersCount();
            if(usersCount == 0 && adminLogin != null && adminPassword != null && adminEmail != null) {
                User admin = new User(adminEmail, adminPassword, adminLogin);
                logger.log(Level.INFO, "The first user (" + admin + ") created");
            } else {
                logger.log(Level.INFO, "Users count: " + usersCount);
            }
            st.execute("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER, sender INTEGER, receiver INTEGER, message TEXT)");
        }
    }
}