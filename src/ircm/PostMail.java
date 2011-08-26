package ircm;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class PostMail extends Thread {
    private SSLSocket socket;
    private String text;

    public PostMail(String text) {
        this.text = text;
        start();
    }

    public void run() {
        try {
            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket("smtp.gmail.com", 465);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            send("EHLO " + InetAddress.getLocalHost().getHostName());
            while (!reader.readLine().equals("250-AUTH LOGIN PLAIN"));
            reader.readLine();
            send("AUTH LOGIN");
            reader.readLine();
            send("ZWlyaWtiYnJh");
            reader.readLine();
            send("RmFobmc0YnU=");
            reader.readLine();
            send("MAIL FROM:<eirikbbra@gmail.com>");
            reader.readLine();
            send("RCPT TO:<eirikbra@tihlde.org>");
            reader.readLine();
            send("DATA");
            reader.readLine();
            send("From: Eirik B <eirikbbra@gmail.com>");
            send("Subject: IrcM Bugreport");
            send("To: eirikbra@tihlde.org");
            send("");
            send(text);
            send(".");
            reader.readLine();
            send("QUIT");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String s) {
        try {
            socket.getOutputStream().write((s + "\r\n").getBytes());
        } catch (Exception e) {}
    }
}
