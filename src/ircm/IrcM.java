package ircm;

import java.io.File;
import java.net.*;
import java.util.ArrayList;

public class IrcM {

    private int online;
    private int totalConnect;
    private long startTime;
    private ArrayList<Irc> ircs;       
    private String topic;

    public static void main(String[] args) {
        new IrcM();
    }

    public IrcM() {
        if (!new File("users").isDirectory()) {
            new File("users").mkdir();
        }
        online = 0;
        totalConnect = 0;
        startTime = System.currentTimeMillis();
        ircs = new ArrayList<Irc>();
        topic = "Welcome to IrcM!";
        try {
            System.out.println("  -- Ircm --\n");
            ServerSocket server = new ServerSocket(6667);
            new Ping(this);
            while (true) {
                Socket socket = server.accept();
                online++;
                totalConnect++;
                ircs.add(new Irc(this, socket, topic));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void remIrc(Irc irc) {
        ircs.remove(irc);
        online--;
    }

    public String[] getInfo() {
        String[] all = new String[3];
        all[0] = "" + totalConnect;
        all[1] = "" + online;

        long time = (System.currentTimeMillis() - startTime) / 1000;

        int days = (int) (time / 86400);
        time -= (days * 86400);
        int hours = (int) (time / 3600);
        time -= (hours * 3600);
        int minutes = (int) (time / 60);
        time -= (minutes * 60);

        all[2] = days + ":" + hours + ":" + minutes + ":" + time;
        return all;
    }

    public void sendAll(Irc irc, String msg) throws Exception {
        for (Irc i : ircs) {
            i.privmsg("Message from " + irc.getNick() + " to all: " + msg);
        }
    }

    public void sendAll(String msg) throws Exception {
        for (Irc i : ircs) {
            i.privmsg(msg);
        }
    }

    public String[] allUsers() {
        String[] all = new String[ircs.size()];
        for (int i = 0; i < ircs.size(); i++) {
            all[i] = ircs.get(i).getUser();
        }
        return all;
    }

    public void ping() throws Exception {
        for (Irc i : ircs) {
            if (!i.getPing()) {
                remIrc(i);
            }
            i.setPing(false);
            i.ping();
        }
    }

    public void setTopic(String topic) {
        this.topic = topic; 
        for (Irc i : ircs) {
            i.setTopc(topic);
        }
    }
}
