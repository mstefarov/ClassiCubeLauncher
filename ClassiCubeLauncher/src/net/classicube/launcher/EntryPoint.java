package net.classicube.launcher;

import java.util.logging.Level;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;

public class EntryPoint {
    // This is also called by ClassiCubeSelfUpdater

    public static void main(String[] args) {
        // initialize shared code
        LogUtil.Init();
        GameSession.Init();
        SessionManager.Init();
        
        // begin the update process
        ClientUpdateTask.getInstance().execute();

        // set look-and-feel to Numbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException |
                InstantiationException |
                IllegalAccessException |
                UnsupportedLookAndFeelException ex) {
            LogUtil.getLogger().log(Level.WARNING, "Error configuring GUI style", ex);
        }

        // display the form
        ShowSignInScreen();
    }

    // Shows sign-in screen (hides server-list screen, if visible)
    public static void ShowSignInScreen() {
        if (serverListScreen != null) {
            serverListScreen.setVisible(false);
        }
        if (signInScreen == null) {
            signInScreen = new SignInScreen();
        }
        signInScreen.setVisible(true);
    }

    // Shows server-list screen (hides sign-in screen, if visible)
    public static void ShowServerListScreen() {
        if (signInScreen != null) {
            signInScreen.setVisible(false);
        }
        if (serverListScreen == null) {
            serverListScreen = new ServerListScreen();
        }
        serverListScreen.setVisible(true);
    }
    private static SignInScreen signInScreen;
    private static ServerListScreen serverListScreen;
}
