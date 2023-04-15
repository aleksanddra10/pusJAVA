package pus2022.client;

import java.util.Date;

public class User {
    private final String login;
    private final String email;
    public User(String login, String email) {
        this.login = login;
        this.email = email;
    }
    public String toString() {
        return login + " <" + email + ">";
    }
}
