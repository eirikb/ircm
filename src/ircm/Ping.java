package ircm;

public class Ping extends Thread {

    private IrcM ircm;

    public Ping(IrcM ircm) {
        this.ircm = ircm;
        start();
    }

    public void run() {
        while (true) {
            try {
                ircm.ping();
                Thread.sleep(450000);
            } catch (Exception e) {
            }
        }
    }
}
