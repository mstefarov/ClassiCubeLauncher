package net.classicube.launcher;

abstract class GameService {
    UserAccount account;
    protected GameService(UserAccount account){
        this.account = account;
    }
    
    public abstract void signIn();
    public abstract String getSkinUrl();
    public abstract ServerInfo[] getServerList();
    public abstract String getServerPass();
}
