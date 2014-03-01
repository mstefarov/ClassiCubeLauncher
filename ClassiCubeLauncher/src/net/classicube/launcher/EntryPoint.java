package net.classicube.launcher;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import net.classicube.launcher.gui.DebugWindow;
import net.classicube.launcher.gui.ErrorScreen;
import net.classicube.launcher.gui.Resources;
import net.classicube.launcher.gui.SignInScreen;
import net.classicube.shared.SharedUpdaterCode;

// Contains initialization code for the whole launcher
public final class EntryPoint {
    // This is also called by ClassiCubeSelfUpdater

    public static void main(final String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Create launcher's data dir and init logger
        try {
            SharedUpdaterCode.getLauncherDir();
            LogUtil.init();

        } catch (final IOException ex) {
            ErrorScreen.show("Error starting ClassiCube",
                    "Could not create data directory for launcher.", ex);
            System.exit(0);
        }

        // initialize shared code
        GameSession.initCookieHandling();

        // set look-and-feel to Numbus
        Resources.setLookAndFeel();

        if (Prefs.getDebugMode()) {
            DebugWindow.showWindow();
            DebugWindow.setWindowTitle("Launcher Running");
        }

        // display the form
        new SignInScreen().setVisible(true);

        // begin the update process
        UpdateTask.getInstance().execute();

        // begin looking up our external IP address
        GetExternalIPTask.getInstance().execute();
    }
}
