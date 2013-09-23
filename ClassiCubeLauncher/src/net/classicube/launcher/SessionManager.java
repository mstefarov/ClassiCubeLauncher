package net.classicube.launcher;

// Keeps track of the global GameSession and AccountManager instances.
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class SessionManager {
    private static GameSession activeSession;
    private static AccountManager accountManager;

    public static GameSession selectService(final GameServiceType serviceType) {
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

    public static void clearAllResumeInfo() {
        try {
            Preferences servicesNode = Preferences.userNodeForPackage(SessionManager.class).node("GameServices");
            Preferences ccNode = servicesNode.node("ClassiCubeNetSession");
            ccNode.node(GameSession.RESUME_NODE_NAME).removeNode();
            Preferences mcNode = servicesNode.node("MinecraftNetSession");
            mcNode.node(GameSession.RESUME_NODE_NAME).removeNode();
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error erasing resume info", ex);
        }
    }
    
    public static boolean hasAnyResumeInfo(){
        try {
            Preferences servicesNode = Preferences.userNodeForPackage(SessionManager.class).node("GameServices");
            Preferences ccNode = servicesNode.node("ClassiCubeNetSession");
            if(ccNode.nodeExists(GameSession.RESUME_NODE_NAME)) return true;
            Preferences mcNode = servicesNode.node("MinecraftNetSession");
            if(mcNode.nodeExists(GameSession.RESUME_NODE_NAME)) return true;
            return false;
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error checking resume info", ex);
            return false;
        }
    }
}
