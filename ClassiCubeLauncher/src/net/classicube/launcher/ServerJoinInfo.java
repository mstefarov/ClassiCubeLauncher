package net.classicube.launcher;

import java.net.InetAddress;

public class ServerJoinInfo {

    public String playerName;
    public String hash;
    public InetAddress address;
    public int port;
    public String pass;
    public boolean override;
    public boolean signInNeeded;
    public boolean passNeeded;

    @Override
    public String toString() {
        return String.format("mc://%s:%s/%s/%s",
                address.getHostAddress(), port, playerName, pass != null ? pass : "");
    }
}
