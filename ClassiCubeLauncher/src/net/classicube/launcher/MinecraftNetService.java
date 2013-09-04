package net.classicube.launcher;

import java.net.URI;
import java.util.prefs.Preferences;

public class MinecraftNetService extends GameService {

    public MinecraftNetService(UserAccount acct) {
        super(acct);
    }

    @Override
    public SignInResult signIn() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ServerInfo[] getServerList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getServerPass(ServerInfo server) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void storeSession(Preferences pref) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void loadSession(Preferences pref) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSkinUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public URI getSiteUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
