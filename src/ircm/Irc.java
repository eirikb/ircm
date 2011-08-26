package ircm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Irc extends Thread {

    private Debug debug;
    private Socket socket;
    private String nick;
    private String user;
    private String host;
    private Msn msn;
    private IrcM ircm;
    private boolean normalChat;
    private boolean channelChat;
    private boolean ping;
    private boolean voice;
    private String topic;
    private String[] lastMsgs;
    private String away;
    private String replyMsgUser;
    private String forwardMsgUser;
    private final String MAINHOST = "ircm.tihlde.org";
    private final String IPPORT = "[158.38.166.207/6667]";
    private final String ROOT = "IrcM";
    private final String MAIN = "#ircm";

    public enum Cmd {

        NICK, USER, PRIVMSG, MODE, PING, PONG, QUIT, WHO, NOVALUE;

        public static Cmd getCmd(String str) {
            try {
                return valueOf(str);
            } catch (Exception e) {
                return NOVALUE;
            }
        }
    }

    public enum Cmd2 {

        MSN, MSNOUT, HELP, DEBUG, BUG, BUGS, INFO, TOSERVER, ALLUSERS, NORMALCHAT,
        CHANNELCHAT, NAMES, LOL, R, H, AWAY, STATUS, UPDATETOPIC, VOICE, NOVALUE;

        public static Cmd2 getCmd(String str) {
            try {
                return valueOf(str);
            } catch (Exception e) {
                return NOVALUE;
            }
        }
    }

    public Irc(IrcM ircm, Socket newSocket, String topic) {
        debug = new Debug(this, true);
        this.ircm = ircm;
        this.topic = topic;
        socket = newSocket;
        normalChat = false;
        channelChat = true;
        voice = false;
        ping = true;
        lastMsgs = new String[20];
        topic = "Welcome to IrcM!";
        start();
    }

    public void run() {
        try {
            send(":" + MAINHOST + " NOTICE AUTH :IrcM initialized, please go on");
            int read = 0;
            String in = "";
            while ((read = socket.getInputStream().read()) >= 0) {
                if (read == 10) {
                    in = in.trim();
                    String cmd = "";
                    int i = in.indexOf(" ");
                    if (i >= 0) {
                        cmd = in.substring(0, i);
                        in = in.substring(i + 1);
                    } else {
                        cmd = in;
                        in = "";
                    }
                    try {
                        read(cmd.toLowerCase(), in);
                    } catch (Exception e2) {
                        debug.debug(e2);
                    }
                    in = "";
                } else {
                    in += (char) read;
                }
            }
        } catch (Exception e) {
            debug.debug(e);
        }
        System.out.println("irc died");
        ircm.remIrc(this);
        if (msn != null) {
            try {
                msn.send("OUT");
            } catch (Exception e) {
            }
        }
    }

    public void read(String cmd, String in) throws Exception {
        switch (Cmd.getCmd(cmd.toUpperCase())) {
            case NICK:
                nick = in;
                host = socket.getInetAddress().getHostName();
                break;

            case USER:
                user = in.substring(0, in.indexOf(" "));
                sendWelcome();
                break;

            case PRIVMSG:
                String too = in.substring(0, in.indexOf(" "));
                in = in.substring(in.indexOf(":") + 1);
                if (in.indexOf(":") >= 0 && in.indexOf(" ") >= 0 && in.indexOf(":") < in.indexOf(" ")) {
                    forwardMsgUser = in.substring(0, in.indexOf(":"));
                    msn.sendMsg(forwardMsgUser, in.substring(in.indexOf(":") + 2));
                    updateTopic();
                } else {
                    if (too.equalsIgnoreCase(MAIN) || too.equalsIgnoreCase(ROOT)) {
                        if (in.charAt(0) != '.') {
                            if (channelChat && msn != null) {
                                msn.sendMsg(forwardMsgUser, in);
                            } else if (msn == null) {
                                privmsg("Not loged in to msn. Type .help for help. (The period is important)");
                            }
                        } else {
                            int i = in.indexOf(" ");
                            if (i > 0) {
                                cmd = in.substring(0, i);
                                in = in.substring(i + 1);
                            } else {
                                cmd = in;
                                in = "";
                            }
                            command(cmd.substring(1), in);
                        }

                    } else if (msn != null) {
                        forwardMsgUser = too;
                        msn.sendMsg(too, in);
                    }
                }
                break;



            case MODE:
                int i = in.indexOf(" ");
                too = in;
                if (i > 0) {
                    too = in.substring(0, i);
                    in = in.substring(i + 1);
                    if (too.equals(nick)) {
                        send(221, "+i");
                    } else if (too.equals(MAIN) && in.equals("b")) {
                        send(368, MAIN + " :No bans possible");
                    }
                }
                if (in.equals(MAIN)) {
                    send(324, "+t");
                }
                break;

            case WHO:
                send(352, MAIN + " " + user + " " + host + " " + MAINHOST + " " + nick + " H :0  " + user);
                send(315, MAIN + " :End of /WHO list.");
                break;

            case PING:
                send(":" + MAINHOST + " PONG " + MAINHOST + " :" + in);
                break;

            case QUIT:
                socket.close();
                break;

            case PONG:
                ping = true;
                break;

            case NOVALUE:
                debug.debug("Ircserver don't know command: " + cmd + ". Probably not important for IrcM. Full message: " + cmd + " " + in, 4);
                break;
        }
    }

    public void command(String cmd, String in) throws Exception {
        switch (Cmd2.getCmd(cmd.toUpperCase())) {
            case MSN:
                if (in.indexOf(" ") > 0) {
                    String email = in.substring(0, in.indexOf(" "));
                    in = in.substring(in.indexOf(" ") + 1);
                    String status = "Available";
                    if (in.indexOf(" ") > 0) {
                        status = in.substring(in.indexOf(" ") + 1);
                        in = in.substring(0, in.indexOf(" "));
                    }
                    msn = new Msn(this, email, in, status, debug);
                } else {
                    msn = new Msn(this, "oloorin@hotmail.com", "nigger", "Away", debug);
                }
                break;

            case MSNOUT:
                if (msn != null) {
                    socket.close();
                    msn = null;
                }
                break;

            case HELP:
                in = in.replaceAll("../", "");
                File file = new File("txt/help/" + in.replaceAll(" ", "/"));
                if (!file.isDirectory()) {
                    privmsgFile(file.getAbsolutePath());
                } else {
                    privmsgFile(file.getAbsolutePath() + "/index");
                }
                String[] files = file.list();
                if (files.length > 1) {
                    privmsg("");
                    privmsg(" - More info -");
                }
                for (String s : files) {
                    if (!s.equals("index")) {
                        privmsg((char) 3 + "14" + s);
                    }
                }
                break;

            case DEBUG:
                privmsg("Debugging is now set to: " + debug.alterDebug());
                break;

            case BUG:
                if (in.length() > 0) {
                    privmsg("Reporting bug...");
                    new PostMail("From: " + nick + "\r\n\r\n" + in);
                    writeFile("txt/bugs", time() + " " + in + "\n", true);
                    privmsg("Bug reported!");
                    ircm.sendAll("Bugreport reported! Use .bugs to view");
                } else {
                    privmsg("Please type in some text, to report a bug!");
                }
                break;

            case INFO:
                sendInfo();
                break;

            case TOSERVER:
                ircm.sendAll(this, in);
                break;

            case ALLUSERS:
                String[] all = ircm.allUsers();
                for (String s : all) {
                    privmsg(s);
                }
                break;

            case NORMALCHAT:
                privmsg("Normal chat is now set to: " + (normalChat = !normalChat));
                break;

            case CHANNELCHAT:
                privmsg("Channel chat is now set to: " + (channelChat = !channelChat));
                break;

            case VOICE:
                privmsg("Voice is now set to: " + (voice = !voice));
                break;

            case NAMES:
                User[] allUsers = sort(msn.getUsers(), in);
                String[][] names = createPrintableNames(allUsers);
                for (int i = 0; i < names.length; i++) {
                    String s = "";
                    for (int j = 0; j < names[i].length; j++) {
                        s += names[i][j];
                    }
                    privmsg(s);
                }
                break;

            case LOL:
                privmsgFile("txt/lol");
                for (int i = 0; i < 50; i++) {
                    privmsg((char) 3 + "" + i + " ( " + i + " ) Dette er en test!");
                }
                break;

            case R:
                privmsg("Message sent to " + replyMsgUser + "...");
                msn.sendMsg(replyMsgUser, in);
                break;

            case H:
                privmsg("Last messages: ");
                for (int i = lastMsgs.length - 1; i >= 0; i--) {
                    if (lastMsgs[i] != null) {
                        privmsg(lastMsgs[i]);
                    }
                }
                break;

            case AWAY:
                if (in.length() == 0) {
                    away = null;
                    privmsg("Awayessage removed.");
                } else {
                    away = in;
                    privmsg("Awaymessage set to: " + in);
                }
                break;

            case STATUS:
                String s = msn.changeStatus(in);
                if (s != null) {
                    privmsg("Unkown status, avialable statuses: " + s);
                } else {
                    privmsg("Status changed.");
                }
                break;

            case BUGS:
                privmsg("Bugs:");
                privmsgFile("txt/bugs");
                break;

            case UPDATETOPIC:
                if (in.length() == 0) {
                    ircm.setTopic(null);
                } else {
                    ircm.setTopic(in);
                }
                break;

            case NOVALUE:
                privmsg("Unknown command. Try .help");
                break;
        }
    }

    public void send(String send) throws Exception {
        socket.getOutputStream().write((send + "\r\n").getBytes());
    }

    public void send(int cmd, String s) throws Exception {
        String cmd1 = cmd + "";
        if (cmd < 100) {
            cmd1 = "0" + cmd1;
        }
        if (cmd < 10) {
            cmd1 = "0" + cmd1;
        }
        socket.getOutputStream().write((":" + MAINHOST + " " + cmd1 + " " + nick + " " + s + "\r\n").getBytes());
    }

    public void sendWelcome() throws Exception {
        send(1, ":Welcome to IrcM, " + nick);
        send(2, ":Your host is " + MAINHOST + " " + IPPORT + " running version 1");
        send(3, ":IrcM <http://ircm.tihlde.org/>");
        send(4, MAINHOST + " 1.0.3 ais ntov");
        //send(5, ":PREFIX=(ov)@+ CHANTYPES=#& CHANMODES=,,,nt NICKLEN=23 NETWORK=BitlBee CASEMAPPING=rfc1459 MAXTARGETS=1 WATCH=128 :are supported by this server");
        send(375, ":- " + MAINHOST + " Message Of The Day - ");
        send(372, ":- Welcome to IrcM at " + MAINHOST + "!");
        sendAbout();
        send(376, ":End of /MOTD command.");
        send(221, "+s");
        joinMain();
    }

    public void joinMain() throws Exception {
        send(":" + nick + "!" + user + "@" + host + " JOIN :" + MAIN);
        send(332, MAIN + " :Welcome to IrcM!");
        send(353, "= " + MAIN + " :@" + nick + " @" + ROOT);
        send(366, MAIN + " :End of /NAMES list.");
        updateTopic();
        privmsg("Welcome! Main channel: " + MAIN + " Main user: " + ROOT);
        privmsgFile("txt/welcome");
        sendInfo();
    }

    public void sendAbout() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("txt/motd"));
        String s = "";
        while ((s = reader.readLine()) != null) {
            send(372, ":- " + s);
        }
        reader = null;
    }

    public void privmsgFile(String fName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fName));
        String s = "";
        while ((s = reader.readLine()) != null) {
            s = s.replaceAll("<b>", (char) 3 + "14");
            s = s.replaceAll("</b>", (char) 3 + "");
            privmsg(s);
        }
        reader.close();
    }

    public void privmsg(String from, String msg) throws Exception {

        String fromNick = from.substring(0, from.indexOf("!"));
        BufferedReader reader = new BufferedReader(new StringReader(msg));
        String s = "";
        while ((s = reader.readLine()) != null) {
            for (int i = lastMsgs.length - 2; i >= 0; i--) {
                lastMsgs[i + 1] = lastMsgs[i];
            }
            lastMsgs[0] = time() + " " + fromNick + ": " + s;
            if (normalChat) {
                send(":" + from + " PRIVMSG " + nick + " :" + s);
            }
            if (channelChat) {
                send(":" + from + " PRIVMSG " + MAIN + " :" + s);
            }


            if (away != null) {
                msn.sendMsg(fromNick, "Awaymessage: " + away);
                privmsg("Awaymessage sendt to " + fromNick + ": " + away);
            }
        }
    }

    public void privmsg(String msg) throws Exception {
        send(":" + ROOT + "!" + ROOT + "@" + MAINHOST + " PRIVMSG " + MAIN + " :" + msg);
    }

    public void setMsn(Msn msn) {
        this.msn = msn;
    }

    public String getMAIN() {
        return MAIN;
    }

    public String getNick() {
        return nick;
    }

    public void sendInfo() throws Exception {
        String[] all = ircm.getInfo();
        privmsg("Total connections: " + all[0]);
        privmsg("Total online: " + all[1]);
        privmsg("Uptime: " + all[2]);
        privmsg("Debug = " + debug.debug() + ". Normal chat = " + normalChat + ". Channel chat = " + channelChat + ". Voice = " + voice + ".");
    }

    public String readFile(String fName) throws Exception {
        FileInputStream fIn = new FileInputStream(fName);
        byte[] b = new byte[fIn.available()];
        fIn.read(b);
        fIn.close();
        return new String(b);
    }

    public void writeFile(String fName, String text, boolean append) throws Exception {
        FileOutputStream fOut = new FileOutputStream(fName, append);
        fOut.write(text.getBytes());
        fOut.close();
    }

    public String getUser() {
        if (msn != null) {
            return nick + " " + msn.getUser().getEmail();
        }
        return nick;
    }

    public String time() {
        return new SimpleDateFormat("[dd.MM.yy hh:mm]").format(Calendar.getInstance().getTime());
    }

    public String date() {
        return new SimpleDateFormat("dd.MM.yy").format(Calendar.getInstance().getTime());
    }

    public void setPing(boolean b) {
        ping = b;
    }

    public boolean getPing() {
        return ping;
    }

    public void ping() throws Exception {
        send("PING :" + MAINHOST);
    }

    public User[] sort(User[] users, String criteria) {
        User[] all = new User[users.length];
        User[] available = new User[users.length];
        User[] rest = new User[users.length];

        int a = 0;
        int r = 0;
        for (User u : users) {
            if (u.getStatus() == msn.getStatus("Available")) {
                available[a++] = u;
            } else {
                rest[r++] = u;
            }
        }

        available = sort(available);
        rest = sort(rest);

        for (int i = 0; i < a; i++) {
            all[i] = available[i];
        }
        for (int i = 0; i < r; i++) {
            all[a + i] = rest[i];
        }

        return all;
    }

    public User[] sort(User[] users) {
        User[] sortedUsers = new User[users.length];
        for (int i = 0; i < sortedUsers.length; i++) {
            sortedUsers[i] = users[i];
            for (int j = i; j < sortedUsers.length; j++) {
                if (users[j] != null && users[j].getNick().compareTo(sortedUsers[i].getNick()) < 0) {
                    User temp = sortedUsers[i];
                    sortedUsers[i] = users[j];
                    users[j] = temp;

                }
            }
        }
        return sortedUsers;
    }

    public void updateTopic() throws Exception {
        String topic2 = "";
        if (forwardMsgUser != null) {
            topic2 += "(" + forwardMsgUser + ") ";
        }
        if (topic != null) {
            topic2 += topic;
        }

        send(332, MAIN + " :" + topic2);
    }

    public void setTopc(String topic) {
        this.topic = topic;
        try {
            updateTopic();
        } catch (Exception e) {
        }
    }

    public void voice(User user) throws Exception {
        if (voice) {
            send(":" + ROOT + "!" + ROOT + "@" + MAINHOST + " MODE " + MAIN + " +v " + user.getNick());
        }
    }

    public void deVoice(User user) throws Exception {
        if (voice) {
            send(":" + ROOT + "!" + ROOT + "@" + MAINHOST + " MODE " + MAIN + " -v " + user.getNick());
        }
    }

    private String[][] createPrintableNames(User[] users) {
        String[][] all = new String[users.length + 1][5];
        all[0][0] = (char) 3 + "14Nick";
        all[0][1] = "Email";
        all[0][2] = "Status";
        all[0][3] = "Alias";
        all[0][4] = "Name";
        for (int i = 0; i < users.length; i++) {
            all[i + 1][0] = users[i].getNick();
            all[i + 1][1] = users[i].getEmail();
            all[i + 1][2] = msn.getStatus(users[i].getStatus());
            all[i + 1][3] = users[i].getAlias();
            all[i + 1][4] = users[i].getName();
        }
        
        for (int j = 0; j < 4; j++) {
            int l = 0;
            for (int i = 0; i < all.length; i++) {
                l = all[i][j].length() > l ? all[i][j].length() : l;
            }
            l += 4;
            for (int i = 0; i < all.length; i++) {
                while (all[i][j].length() < l) {
                    all[i][j] += " ";
                }
            }
        }
        
        all[0][0] += "   ";
        
        return all;
    }
}
