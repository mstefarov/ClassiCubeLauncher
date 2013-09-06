package net.classicube.launcher;

import java.net.URI;
import javax.swing.SwingWorker;

class ClassiCubeNetSession extends GameSession {

    public ClassiCubeNetSession(UserAccount acct) {
        super("ClassiCubeNetService", acct);
    }

    @Override
    public SwingWorker<SignInResult, String> signInAsync(boolean remember) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SwingWorker<ServerInfo[], ServerInfo> getServerListAsync() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SwingWorker<Boolean, Boolean> getServerPassAsync(ServerInfo server) {
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
