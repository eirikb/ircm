package ircm;

import java.util.ArrayList;

public class User {

    private String email;
    private String nick;
    private String host;
    private String name;
    private int status;
    private ArrayList<String> msgs;
    private String alias;

    public User(String email, int status, String name) {
        this.email = email;
        msgs = new ArrayList<String>();
        if (email.indexOf("@") >= 0) {
            nick = email.substring(0, email.indexOf("@"));
            host = email.substring(email.indexOf("@") + 1);
        } else {
            nick = email;
            host = "";
        }
        alias = nick;
        this.status = status;
        this.name = name;
    }

    public String getNick() {
        return nick;
    }

    public String getHost() {
        return host;
    }

    public int getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addMsg(String s) {
        msgs.add(s);
    }

    public String fetch() {
        if (msgs.size() == 0) {
            return null;
        }
        String s = msgs.get(0);
        msgs.remove(0);
        return s;
    }

    public String getMsg() {
        if (msgs.size() == 0) {
            return null;
        }
        return msgs.get(0);
    }

    public void remMsg() {
        if (msgs.size() > 0) {
            msgs.remove(0);
        }
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
