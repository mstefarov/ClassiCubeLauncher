package net.classicube.launcher;

// Keeps track of the global GameSession and AccountManager instances.
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class SessionManager {
    private static GameSession activeSession;
    private static AccountManager accountManager;

    public static GameSession selectService(final GameServiceType serviceType) {
        accountManager = new AccountManager(serviceType);
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

    public static void clearAllResumeInfo() {
        try {
            Preferences servicesNode = Preferences.userNodeForPackage(SessionManager.class);
            Preferences ccNode = servicesNode.node(GameServiceType.ClassiCubeNetService.name());
            ccNode.node(GameSession.RESUME_NODE_NAME).removeNode();
            Preferences mcNode = servicesNode.node(GameServiceType.MinecraftNetService.name());
            mcNode.node(GameSession.RESUME_NODE_NAME).removeNode();
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error erasing resume info", ex);
        }
    }
    
    public static boolean hasAnyResumeInfo(){
        try {
            Preferences servicesNode = Preferences.userNodeForPackage(SessionManager.class);
            Preferences ccNode = servicesNode.node(GameServiceType.ClassiCubeNetService.name());
            if(ccNode.nodeExists(GameSession.RESUME_NODE_NAME)) return true;
            Preferences mcNode = servicesNode.node(GameServiceType.MinecraftNetService.name());
            if(mcNode.nodeExists(GameSession.RESUME_NODE_NAME)) return true;
            return false;
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error checking resume info", ex);
            return false;
        }
    }
}
