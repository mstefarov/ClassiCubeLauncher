package net.classicube.launcher;

import java.io.File;
import java.util.logging.Level;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;

public final class EntryPoint {
    // This is also called by ClassiCubeSelfUpdater

    public static void main(String[] args) {
        // Create launcher's data dir
        final File launcherDataDir = PathUtil.getLauncherDir();
        if (!launcherDataDir.exists() && !launcherDataDir.mkdirs()) {
            LogUtil.die("Could not create launcher data dir.");
        }

        // initialize shared code
        LogUtil.Init();
        GameSession.init();
        SessionManager.init();

        // begin the update process
        //ClientUpdateTask.getInstance().execute(); // TEMP: testing updates

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
        new SignInScreen().setVisible(true);
    }
}
