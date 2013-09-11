package net.classicube.launcher;

import java.util.prefs.Preferences;

final class SessionManager {

    private static ServerInfo serverDetails;
    private static GameServiceType activeServiceType;
    private static GameSession activeSession;
    private static AccountManager accountManager;

    public static void selectService(GameServiceType serviceType) {
        activeServiceType = serviceType;
        accountManager = new AccountManager(serviceType.name());
        accountManager.Load();
        Prefs.setSelectedGameService(serviceType);
    }

    public static GameServiceType getServiceType() {
        return activeServiceType;
    }

    public static GameSession getSession() {
        return activeSession;
    }

    public static GameSession createSession(UserAccount userAccount) {
        if (userAccount == null) {
            throw new NullPointerException("userAccount");
        }
        if (activeServiceType == GameServiceType.ClassiCubeNetService) {
            activeSession = new ClassiCubeNetSession(userAccount);
        } else {
            activeSession = new MinecraftNetSession(userAccount);
        }
        return activeSession;
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }

    public static void Init() {
        activeServiceType = Prefs.getSelectedGameService();
    }

    public static void setServerInfo(ServerInfo server) {
        serverDetails = server;
    }

    public static ServerInfo getServerInfo() {
        return serverDetails;
    }
}
