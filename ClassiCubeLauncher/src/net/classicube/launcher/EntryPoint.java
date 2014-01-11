package net.classicube.launcher;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import net.classicube.launcher.gui.DebugWindow;
import net.classicube.launcher.gui.ErrorScreen;
import net.classicube.launcher.gui.SignInScreen;

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
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel() {
                @Override
                public UIDefaults getDefaults() {
                    // Customize the colors to match ClassiCube.net style
                    final Color ccLight = new Color(153, 128, 173);
                    final Color ccBorder = new Color(97, 81, 110);
                    final UIDefaults ret = super.getDefaults();
                    final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 13);
                    ret.put("Button.font", font);
                    ret.put("ToggleButton.font", font);
                    ret.put("Button.textForeground", Color.WHITE);
                    ret.put("ToggleButton.textForeground", Color.WHITE);
                    ret.put("nimbusBase", ccLight);
                    ret.put("nimbusBlueGrey", ccLight);
                    ret.put("control", ccLight);
                    ret.put("nimbusFocus", ccBorder);
                    ret.put("nimbusBorder", ccBorder);
                    ret.put("nimbusSelectionBackground", ccBorder);
                    ret.put("Table.background", Color.WHITE);
                    ret.put("Table.background", Color.WHITE);
                    ret.put("nimbusOrange", new Color(101, 38, 143));
                    return ret;
                }
            });
        } catch (final UnsupportedLookAndFeelException ex) {
            LogUtil.getLogger().log(Level.WARNING, "Error configuring GUI style.", ex);
        }

        if (Prefs.getDebugMode()) {
            DebugWindow.showWindow();
            DebugWindow.setWindowTitle("Launcher Running");
        }

        // display the form
        new SignInScreen().setVisible(true);

        // begin the update process
        ClientUpdateTask.getInstance().execute();
        
        // begin looking up our external IP address
        GetExternalIPTask.getInstance().execute();
    }
}
