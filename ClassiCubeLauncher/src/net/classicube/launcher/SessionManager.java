package net.classicube.launcher;

final class SessionManager {

    private static ServerJoinInfo joinInfo;
    private static GameServiceType activeServiceType;
    private static GameSession activeSession;
    private static AccountManager accountManager;

    public static void init() {
        activeServiceType = Prefs.getSelectedGameService();
    }

    public static GameSession selectService(GameServiceType serviceType) {
        activeServiceType = serviceType;
        accountManager = new AccountManager(serviceType.name());
        accountManager.load();
        Prefs.setSelectedGameService(serviceType);
        return createNewSession();
    }

    public static GameServiceType getServiceType() {
        return activeServiceType;
    }

    public static GameSession getSession() {
        return activeSession;
    }

    public static GameSession createNewSession() {
        if (activeServiceType == GameServiceType.ClassiCubeNetService) {
            activeSession = new ClassiCubeNetSession();
        } else {
            activeSession = new MinecraftNetSession();
        }
        return activeSession;
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }

    public static void setJoinInfo(ServerJoinInfo server) {
        joinInfo = server;
    }

    public static ServerJoinInfo getJoinInfo() {
        return joinInfo;
    }
}
