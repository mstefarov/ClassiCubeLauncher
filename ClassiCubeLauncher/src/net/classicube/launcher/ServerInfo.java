package net.classicube.launcher;

import java.net.InetAddress;

// Stores all metadata about a game server
class ServerInfo {
    // Basic info
    public String name;
    public String hash;
    
    // Info from the server list
    public int players;
    public int maxPlayers;
    public String flag;
    public int uptime;
    
    // Info from the play page
    public String pass;
    public InetAddress address;
    public int port;
}
