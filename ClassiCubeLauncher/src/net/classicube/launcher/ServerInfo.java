package net.classicube.launcher;

import java.net.InetAddress;

class ServerInfo {

    public ServerInfo(String name, String pass, InetAddress address, int port) {
        this.name = name;
        this.pass = pass;
        this.address = address;
        this.port = port;
    }
    public String name;
    public String pass;
    public InetAddress address;
    public int port;
}
