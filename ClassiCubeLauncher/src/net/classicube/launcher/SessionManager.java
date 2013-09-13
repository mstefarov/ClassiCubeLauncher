package net.classicube.launcher;

// Keeps track of the global GameSession and AccountManager instances.
final class SessionManager {
    private static GameSession activeSession;
    private static AccountManager accountManager;

    public static GameSession selectService(GameServiceType serviceType) {
        accountManager = new AccountManager(serviceType.name());
        accountManager.load();
        Prefs.setSelectedGameService(serviceType);
        if (serviceType == GameServiceType.ClassiCubeNetService) {
            activeSession = new ClassiCubeNetSession();
        } else {
            activeSession = new MinecraftNetSession();
        }
        return activeSession;
    }

    public static GameSession getSession() {
        return activeSession;
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }
}
