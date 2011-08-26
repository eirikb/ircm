package ircm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.*;

public class Msn extends Thread {

    private Debug debug;
    private Socket socket;
    private int cmdnr;
    private User user;
    private String pass;
    private Irc irc;
    private Map<String, User> users;
    private Map<String, Switchboard> sbs;
    private Switchboard tempSb;
    private boolean close;
    private int syn;
    private final String[][] STATUS = {{"NLN", "BSY", "IDL", "BRB", "AWY", "PHN", "LUN"},
        {"Available", "Busy", "Idle", "BRB", "Away", "Phone", "Lunch"}};

    public enum Cmd {

        VER, CVR, XFR, USR, CHL, RNG, ILN, FLN, NLN, MSG, SYN, NOVALUE;

        public static Cmd getCmd(String str) {
            try {
                return valueOf(str);
            } catch (Exception e) {
                return NOVALUE;
            }
        }
    }

    public Msn(Irc irc, String email, String pass, String status, Debug debug) {
        debug.debug("MSN CREATE", 4);
        irc.setMsn(new Msn(irc, new User(email, getStatus(status), "LOL"), pass, debug));
    }

    public Msn(Irc irc, User user, String pass, Debug debug) {
        debug.debug("MSN CREATE", 4);
        try {
            close = true;
            this.irc = irc;
            this.user = user;
            this.debug = debug;
            debug.debug("BLUE -> SENDING", 2);
            debug.debug("GREEN -> RECEIVEING", 3);
            debug.debug("RED -> STATUS", 4);
            debug.debug("", 0);
            irc.privmsg("Logging in...");
            irc.setMsn(new Msn(irc, user, pass.trim(), "messenger.hotmail.com", debug));
        } catch (Exception e) {
            debug.debug(e);
        }
    }

    public Msn(Irc irc, User user, String pass, String host, Debug debug) {
        debug.debug("MSN CREATE", 4);
        try {
            close = false;
            this.irc = irc;
            this.user = user;
            this.pass = pass;
            this.debug = debug;
            cmdnr = 0;
            sbs = new HashMap<String, Switchboard>();
            socket = new Socket(host, 1863);

            send("VER", "MSNP8 CVR0");

            start();

        } catch (Exception e) {
            debug.debug(e);
        }
    }

    public void run() {
        try {
            int read = 0;
            String in = "";
            while ((read = socket.getInputStream().read()) >= 0) {
                in += (char) read;
                if (read == 13) {
                    in = in.trim();
                    debug.debug(in, 3);
                    if (in.indexOf(" ") > 0) {
                        String cmd = in.substring(0, in.indexOf(" "));
                        in = in.substring(in.indexOf(" ") + 1);
                        String cmdint = "";
                        if (in.indexOf(" ") >= 0) {
                            cmdint = in.substring(0, in.indexOf(" "));
                        }
                        in = in.substring(in.indexOf(" ") + 1);
                        try {
                            read(cmd, cmdint, in);
                        } catch (Exception e) {
                            debug.debug(e);
                        }
                    }
                    in = "";
                }
            }
        } catch (Exception e) {
            debug.debug(e);
        }

        debug.debug("MSN QUIT", 4);
        if (close) {
            try {
                saveUsers();
                irc.privmsg("Msn shut down, lol.");
            } catch (Exception e) {
            }
            irc.setMsn(null);
        }
    }

    public void read(String cmd, String cmdint, String in) throws Exception {
        switch (Cmd.getCmd(cmd)) {
            case VER:
                send("CVR", "0x0409 win 4.10 i386 MSNMSGR 5.0.0544 MSMSGS " + user.getEmail());
                break;

            case CVR:
                send("USR", "TWN I " + user.getEmail());
                break;

            case XFR:
                if (in.indexOf("NS ") == 0) {
                    in = in.substring(in.indexOf(" ") + 1, in.indexOf(":"));
                    irc.setMsn(new Msn(irc, user, pass, in, debug));
                } else {
                    String s = cmdint;
                    in = in.substring(in.indexOf(" ") + 1);
                    String host = in.substring(0, in.indexOf(":"));
                    in = in.substring(in.indexOf(" ") + 1);
                    in = in.substring(in.indexOf(" ") + 1);
                    tempSb.connect(host);
                    tempSb.usr(in);
                }
                break;

            case USR:
                if (in.substring(0, in.indexOf(" ")).equals("TWN")) {
                    in = in.substring(in.indexOf(" ") + 1);
                    in = in.substring(in.indexOf(" ") + 1);
                    String key = getKey(in);
                    if (key.length() > 0) {
                        send("USR", "TWN S " + key);
                    } else {
                        irc.privmsg("Login failed! Wrong username / password?");
                        socket.close();
                    }
                } else {
                    irc.privmsg("Login ok!");
                    close = true;
                    users = loadUsers();
                    if (users == null) {
                        users = new HashMap<String, User>();
                    } else {
                        Collection<User> col = users.values();
                        for (User u : col) {
                            irc.send(":" + u.getNick() + "!~" + u.getEmail() + " JOIN :" + irc.getMAIN());
                            if (u.getStatus() == 0) {
                                irc.voice(u);
                            }
                        }
                    }
                    send("SYN", "" + loadSyn());
                    send("CHG", STATUS[0][user.getStatus()] + " 0");
                }
                break;

            case CHL:
                challenge(in);
                break;

            case RNG:
                String name = in.substring(in.lastIndexOf(" "));
                String host = in.substring(0, in.indexOf(":"));
                in = in.substring(in.indexOf(" ") + 1);
                in = in.substring(in.indexOf(" ") + 1);
                String auth = in.substring(0, in.indexOf(" ")) + " " + cmdint;
                in = in.substring(in.indexOf(" ") + 1);
                String userHost = in.substring(in.indexOf("@") + 1);
                in = in.substring(0, in.indexOf("@"));
                if (users.get(in) == null) {
                    users.put(in, new User(in + "@" + userHost, 0, name));
                }
                Switchboard sb = new Switchboard(this, host, debug);
                sb.setOwener(in);
                sbs.put(in, sb);
                sb.ans(auth);
                break;

            case ILN:
                int status = getStatus(in.substring(0, in.indexOf(" ")));
                in = in.substring(in.indexOf(" ") + 1);
                String email = in.substring(0, in.indexOf(" "));
                in = in.substring(in.indexOf(" ") + 1);
                name = in.substring(0, in.indexOf(" "));
                User user = new User(email, status, name);
                if (users.get(user.getNick()) == null) {
                    users.put(user.getNick(), user);
                    irc.send(":" + user.getNick() + "!~" + email + " JOIN :" + irc.getMAIN());
                    if (user.getStatus() == 0) {
                        irc.voice(user);
                    }
                }
                break;
            case FLN:
                user = users.get(in.substring(0, in.indexOf("@")));
                irc.send(":" + user.getNick() + "!~" + user.getEmail() + " QUIT :");
                users.remove(user.getNick());
                break;

            case NLN:
                status = getStatus(cmdint);
                email = in.substring(0, in.indexOf(" "));
                String nick = in.substring(0, in.indexOf("@"));
                in = in.substring(in.indexOf(" ") + 1);
                name = in.substring(0, in.indexOf(" "));
                if (users.get(nick) == null) {
                    users.put(nick, new User(email, status, name));
                    irc.send(":" + nick + "!~" + email + " JOIN :" + irc.getMAIN());
                    if (users.get(nick).getStatus() == 0) {
                        irc.voice((users.get(nick)));
                    }
                } else {
                    user = users.get(nick);
                    int firstStatus = user.getStatus();
                    users.get(nick).setStatus(getStatus(cmdint));
                    users.get(nick).setName(name);
                    if (firstStatus != user.getStatus() && (firstStatus == 0 || user.getStatus() == 0)) {
                        if (user.getStatus() == 0) {
                            irc.voice(user);
                        } else {
                            irc.deVoice(user);
                        }
                    }
                }
                break;

            case MSG:
                byte[] b = new byte[Integer.parseInt(in.substring(in.indexOf(" ") + 1))];
                socket.getInputStream().read(b);
                break;

            case SYN:
                syn = Integer.parseInt(in.substring(0, in.indexOf(" ")));
                break;

            case NOVALUE:
                debug.debug("Msnclient don't know command: " + cmd + ". Probably not important for IrcM. Full message: " + cmd + " " + in, 4);
                break;
        }
    }

    public void challenge(String s) throws Exception {
        s = s + "Q1P7W2E4J9R8U3S5";
        s = MD5(s);
        send("QRY", "msmsgs@msnmsgr.com " + s.length());
        socket.getOutputStream().write(s.getBytes());
    }

    public void send(String cmd, String send) throws Exception {
        cmdnr++;
        debug.debug(cmd + " " + cmdnr + " " + send, 2);
        socket.getOutputStream().write((cmd + " " + cmdnr + " " + send + "\r\n").getBytes());
    }

    public void send(String send) throws Exception {
        debug.debug(send, 2);
        socket.getOutputStream().write((send + "\r\n").getBytes());

    }

    public String getKey(String auth) throws Exception {
        SSLSocket s = (SSLSocket) SSLSocketFactory.getDefault().createSocket("login.passport.com", 443);

        String email = user.getEmail();
        email.replace("@", "%40");
        String toSend = "GET /login2.srf HTTP/1.1\r\n";
        toSend += "Authorization: Passport1.4 OrgVerb=GET,OrgURL=http%3A%2F%2Fmessenger%2Emsn%2Ecom,sign-in=" + email + ",pwd=" + pass;
        toSend += "," + auth + "\r\n";
        toSend += "Host: login.passport.com\r\n\r\n";

        s.getOutputStream().write(toSend.getBytes());

        String in = "";
        while (in.indexOf("\r\n\r\n") < 0) {
            in += (char) s.getInputStream().read();
        }
        s.close();

        if (in.indexOf("HTTP/1.1 200 OK") == 0) {
            in = in.substring(in.indexOf("Authentication-Info:"));
            in = in.substring(in.indexOf("'t=") + 1);
            in = in.substring(0, in.indexOf("'"));
            return in;
        } else {
            return "";
        }
    }

    public void sendMsg(String too, String msg) throws Exception {
        Switchboard sb = sbs.get(too);
        if (sb != null) {
            users.get(too).addMsg(msg);
            if (sb.getJoin()) {
                String s = "";
                while ((s = users.get(too).fetch()) != null) {
                    sb.sendMsg(s);
                }
            }
        } else {
            User u = users.get(too);
            if (u != null) {
                tempSb = new Switchboard(this, debug);
                u.addMsg(msg);
                tempSb.setCal(u.getEmail());
                sbs.put(too, tempSb);
                tempSb.setOwener(too);
                send("XFR", "SB");
            } else {
                irc.privmsg("User not in online-contact-list");
            }
        }
    }

    public String MD5(String pass) throws Exception {
        byte[] defaultBytes = pass.getBytes();
        MessageDigest algorithm = MessageDigest.getInstance("MD5");
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        pass = hexString.toString();
        return pass;
    }

    public int getStatus(String status) {
        for (int i = 0; i < STATUS[0].length; i++) {
            if (status.equals(STATUS[0][i]) || status.equalsIgnoreCase(STATUS[1][i])) {
                return i;
            }
        }
        return -1;
    }

    public User getUser() {
        return user;
    }

    public void chat(String u, String msg) throws Exception {
        irc.privmsg(u, msg);
    }

    public void remSwitchboard(String owner) {
        sbs.remove(owner);
    }

    public void saveUsers() throws Exception {
        FileOutputStream fOut = new FileOutputStream("users/" + user.getEmail() + "/lst");
        for (User u : users.values()) {
            fOut.write((u.getEmail() + " " + u.getStatus() + " " + u.getName() + "\n").getBytes());
        }
        fOut.close();
        fOut = new FileOutputStream("users/" + user.getEmail() + "/syn");
        fOut.write(("" + syn).getBytes());
        fOut.close();
    }

    public HashMap<String, User> loadUsers() throws Exception {
        if (!new File("users/" + user.getEmail()).exists()) {
            new File("users/" + user.getEmail()).mkdir();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("users/" + user.getEmail() + "/lst")));
        HashMap<String, User> all = new HashMap<String, User>();
        String s = "";
        while ((s = reader.readLine()) != null) {
            String email = s.substring(0, s.indexOf(" "));
            s = s.substring(s.indexOf(" ") + 1);
            int status = Integer.parseInt(s.substring(0, s.indexOf(" ")));
            String name = s.substring(s.indexOf(" ") + 1);
            User user = new User(email, status, name);
            all.put(user.getNick(), user);
        }
        reader.close();
        return all;
    }

    public int loadSyn() {
        try {
            FileInputStream fIn = new FileInputStream("users/" + user.getEmail() + "/syn");
            byte[] b = new byte[fIn.available()];
            fIn.read(b);
            fIn.close();
            return Integer.parseInt(new String(b).trim());
        } catch (Exception e) {

        }
        return 0;
    }

    public void join(String owner) {
        String s = "";
        while ((s = users.get(owner).fetch()) != null) {
            sbs.get(owner).sendMsg(s);
        }
    }

    public User[] getUsers() {
        return (User[]) users.values().toArray(new User[users.size()]);
    }

    public String getStatus(int i) {
        return STATUS[1][i];
    }

    public String changeStatus(String in) throws Exception {
        int stat = getStatus(in);
        if (stat >= 0) {
            user.setStatus(stat);
            send("CHG", STATUS[0][stat] + " 0");
        } else {
            String s = "";
            for (int i = 0; i < STATUS[0].length; i++) {
                s += STATUS[1][i] + "  ";
            }
            return s;
        }
        return null;
    }
}
