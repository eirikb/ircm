package ircm;

import java.net.Socket;

public class Switchboard extends Thread {
    private Msn msn;
    private Debug debug;
    private Socket socket;
    private int cmdnr;
    private String cal;
    private String owner;
    private boolean join;
    private final String[] CMDS = {"MSG", "NAK", "JOI", "USR", "BYE", "IRO"};

    public Switchboard(Msn msn, Debug debug) {
        this.msn = msn;
        this.debug = debug;
        cmdnr = 0;
    }

    public Switchboard(Msn msn, String address, Debug debug) {
        this.msn = msn;
        this.debug = debug;
        join = false;
        cmdnr = 0;
        try {
            socket = new Socket(address, 1863);
            start();
        } catch (Exception e) {
            debug.debug("" + e, 4);
        }
    }

    public void connect(String address) {
        try {
            socket = new Socket(address, 1863);
            start();
        } catch (Exception e) {
            debug.debug(e);
        }
    }

    public void setOwener(String owner) {
        this.owner = owner;
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
                        if (!cmd.equals("MSG")) {
                            in = in.substring(in.indexOf(" ") + 1);
                        }
                        read(cmd, in);
                    }
                    in = "";
                }
            }
        } catch (Exception e) {
            debug.debug(e);
        }
        debug.debug("Switchboard DOWN!", 4);
        msn.remSwitchboard(owner);
    }

    public void read(String cmd, String in) throws Exception {
        int nr = -1;
        for (int i = 0; i < CMDS.length; i++) {
            if (CMDS[i].equals(cmd)) {
                nr = i;
                break;
            }
        }

        switch (nr) {
            case 0:
                //MSG
                String u = in.substring(0, in.indexOf(" "));
                int l = Integer.parseInt(in.substring(in.lastIndexOf(" ") + 1, in.length()));
                byte[] b = new byte[l + 1];
                try {
                    socket.getInputStream().read(b);
                } catch (Exception e) {
                }

                if (b[b.length - 1] != 10) {
                    int header = 0;
                    int i = 0;
                    while (header < 4) {
                        if (b[i] == 10 || b[i] == 13) {
                            header++;
                        } else {
                            header = 0;
                        }
                        i++;
                    }
                    in = new String(b, i, b.length - i);
                    msn.chat(u.substring(0, u.indexOf("@")) + "!" + u, in);
                }

                break;

            case 1:
                //NAK
                msn.chat(owner, "  **ERRR!** The message was not sent!");
                System.out.println("message not sendt, blah!");
                break;

            case 2:
                //JOI
                join = true;
                if (cal != null) {
                    msn.join(owner);
                    cal = null;
                }

                break;

            case 3:
                //USR
                send("CAL", cal);
                break;

            case 4:
                //BYE
                try {
                    socket.close();
                } catch (Exception e) {
                }
                break;
            case 5:
                //IRO
                join = true;
                msn.join(owner);
                break;
        }
    }

    public void ans(String auth) throws Exception {
        send("ANS", msn.getUser().getEmail() + " " + auth);
    }

    public void usr(String auth) throws Exception {
        send("USR", msn.getUser().getEmail() + " " + auth);
    }

    public void send(String cmd, String send) throws Exception {
            cmdnr++;
            debug.debug(cmd + " " + cmdnr + " " + send, 2);
            socket.getOutputStream().write((cmd + " " + cmdnr + " " + send + "\r\n").getBytes());
   
    }

    public void sendMsg(String in) {
        byte[] all = in.getBytes();
        
        byte[] all2 = new byte[all.length];
        int j = 0;
        for (int i = 0; i < all.length; i++) {
            all2[j++] = all[i]; 
            if (all[i] == -61 && all[i + 1] == -125 && all[i + 2] == -62) {
                i += 2;
            }
        }
        
        all = new byte[j];
        for (int i = 0; i < j; i++) {
            all[i] = all2[i];
        } 
        
        try {
            debug.debug("MSG : " + in, 2);
            cmdnr++;
            String tot = "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "X-MMS-IM-Format: FN=MS%20Sans%20Serif; EF=; CO=0; CS=0; PF=0\r\n\r\n";
            tot = "MSG " + cmdnr + " N " + (tot.length() + all.length) + "\r\n" + tot;

            socket.getOutputStream().write(tot.getBytes());
            socket.getOutputStream().write(all);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCal(String c) {
        cal = c;
    }

    public boolean getJoin() {
        return join;
    }
}
