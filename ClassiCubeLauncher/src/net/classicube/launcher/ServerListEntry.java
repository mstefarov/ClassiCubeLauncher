package net.classicube.launcher;

import java.util.Locale;

// Stores all metadata about a game server
public final class ServerListEntry {
    // Basic info
    public String name;
    public String hash;
    
    // Info from the server list
    public int players;
    public int maxPlayers;
    public String flag;
    public int uptime;

    public static String formatUptime(final int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 60 * 60) {
            return (seconds / 60) + "m";
        } else if (seconds < 60 * 60 * 24) {
            return (seconds / (60 * 60)) + "h";
        } else {
            return (seconds / (60 * 60 * 24)) + "d";
        }
    }

    public static String toCountryName(final String countryCode) {
        if (countryCode == null) {
            throw new NullPointerException("s");
        }
        final Locale l = new Locale("EN", countryCode);
        return l.getDisplayCountry();
    }
}
