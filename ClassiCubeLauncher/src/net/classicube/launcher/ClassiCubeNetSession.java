package net.classicube.launcher;

import java.net.URI;

class ClassiCubeNetSession extends GameSession {

    public ClassiCubeNetSession(UserAccount acct) {
        super("ClassiCubeNetService", acct);
    }

    @Override
    public SignInTask signInAsync(boolean remember) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GetServerListTask getServerListAsync() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GetServerDetailsTask getServerDetailsAsync(ServerInfo server) {
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
