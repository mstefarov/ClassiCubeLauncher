package net.classicube.launcher;

import java.util.prefs.Preferences;

abstract class GameService {

    UserAccount account;

    protected GameService(UserAccount account) {
        this.account = account;
    }

    // Tries to start a play session
    public abstract SignInResult signIn();

    // Fetches the server list
    public abstract ServerInfo[] getServerList();

    // Gets mppass for given server
    public abstract String getServerPass(ServerInfo server);

    // Stores current session
    public abstract void storeSession(Preferences pref);

    // Loads a previously-saved session
    public abstract void loadSession(Preferences pref);

    // Gets base skin URL (to pass to the client)
    public abstract String getSkinUrl();
}