package ircm;

import java.io.FileOutputStream;

public class Debug {

    private FileOutputStream fOut;
    private Irc irc;
    private boolean debug;

    public Debug(Irc irc, boolean debug) {
        this.irc = irc;
        this.debug = debug;
        try {
            fOut = new FileOutputStream("Exceptions", true);
        } catch (Exception e) {
        }
    }

    public void debug(String debugMsg, int color) {
        if (debug) {
            send(debugMsg, color);
        }
    }

    public void send(String debugMsg, int color) {
        try {
            irc.send(":" + "debug!debug@ircm.tihlde.org" + " PRIVMSG " + irc.getNick() + " :" + (char) 3 + color + debugMsg);
        } catch (Exception e) {
        }
    }

    public void debug(Exception e) {
        try {
            e.printStackTrace();
            fOut.write((irc.time() + "  " + irc.getNick() + "  " + irc.getUser() + "\n" + e + "\n").getBytes());
            if (debug) {
                irc.send(":" + "debug!debug@ircm.tihlde.org" + " PRIVMSG " + irc.getNick() + " :");
                irc.send(":" + "debug!debug@ircm.tihlde.org" + " PRIVMSG " + irc.getNick() + " :" + (char) 3 + "4" + e);
            }
            for (StackTraceElement s : e.getStackTrace()) {
                fOut.write(("" + s + "\n").getBytes());
                if (debug) {
                    irc.send(":" + "debug!debug@ircm.tihlde.org" + " PRIVMSG " + irc.getNick() + " :" + (char) 3 + "4" + s);
                }
            }
            fOut.write(("\n").getBytes());
            if (debug) {
                irc.send(":" + "debug!debug@ircm.tihlde.org" + " PRIVMSG " + irc.getNick() + " :");
            }
        } catch (Exception e2) {
        }
    }

    public void debug(boolean debug) {
        this.debug = debug;
    }

    public boolean debug() {
        return debug;
    }

    public boolean alterDebug() {
        debug = !debug;
        return debug;
    }
}
