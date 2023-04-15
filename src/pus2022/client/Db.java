package pus2022.client;

import java.sql.*;

public class Db {
    public Connection connection = null;
    public Db(String dbUrl) throws SQLException, ClassNotFoundException {
        connection = DriverManager.getConnection(dbUrl);
        Statement st = connection.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS lastseen (id INTEGER PRIMARY KEY, lastread INTEGER)");
        PreparedStatement st2 = connection.prepareStatement("INSERT OR IGNORE INTO lastseen (id,lastread) VALUES (1,?)");
        st2.setLong(1, 0L);
        st2.execute();
    }
}
