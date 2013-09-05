package net.classicube.launcher;

import java.util.prefs.Preferences;

class SessionManager {

    private static GameServiceType activeServiceType;
    private static GameSession activeSession;
    private static AccountManager accountManager;
    public static final String SelectedServiceKeyName = "SelectedGameService";

    public static void selectService(GameServiceType serviceType) {
        activeServiceType = serviceType;
        accountManager = new AccountManager(serviceType.name());
        prefs.put(SelectedServiceKeyName, serviceType.name());
    }

    public static GameServiceType getServiceType() {
        return activeServiceType;
    }

    public static GameSession getSession() {
        return activeSession;
    }

    public static GameSession createSession(UserAccount newService) {
        if (activeServiceType == GameServiceType.ClassiCubeNetService) {
            activeSession = new ClassiCubeNetSession(newService);
        } else {
            activeSession = new MinecraftNetSession(newService);
        }
        return activeSession;
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }

    public static void Init() {
        // preferred service
        prefs = Preferences.userNodeForPackage(EntryPoint.class);
        String serviceName = prefs.get(SelectedServiceKeyName,
                GameServiceType.ClassiCubeNetService.name());
        activeServiceType = GameServiceType.valueOf(serviceName);
    }
    static Preferences prefs;
}
