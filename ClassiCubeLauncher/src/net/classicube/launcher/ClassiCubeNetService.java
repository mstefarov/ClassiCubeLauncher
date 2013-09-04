package net.classicube.launcher;

import java.net.URI;

public class ClassiCubeNetService extends GameService {

    public ClassiCubeNetService(UserAccount acct) {
        super("ClassiCubeNetService", acct);
    }

    @Override
    public SignInResult signIn(boolean remember) throws SignInException {
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
    public String getSkinUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI getSiteUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
