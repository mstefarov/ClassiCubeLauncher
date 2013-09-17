package net.classicube.launcher;

import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel; // TODO investigate javax.swing.plaf.nimbus.NimbusLookAndFeel
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.logging.Level;
import javax.swing.UIDefaults;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;

// Contains initialization code for the whole launcher
public final class EntryPoint {
    // This is also called by ClassiCubeSelfUpdater

    public static void main(final String[] args) {
        // Create launcher's data dir
        final File launcherDataDir = SharedUpdaterCode.getLauncherDir();
        if (!launcherDataDir.exists() && !launcherDataDir.mkdirs()) {
            LogUtil.die("Could not create launcher data dir.", null);
        }

        // initialize shared code
        LogUtil.Init();
        GameSession.init();

        // set look-and-feel to Numbus
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel() {
                @Override
                public UIDefaults getDefaults() {
                    Color ccLight = new Color(153, 128, 173);
                    Color ccBorder = new Color(97, 81, 110);
                    UIDefaults ret = super.getDefaults();
                    Font font = new Font(Font.SANS_SERIF, Font.BOLD, 13);
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
                    ret.put("nimbusOrange", new Color(101,38,143));
                    return ret;
                }
            });
        } catch (final UnsupportedLookAndFeelException ex) {
            LogUtil.getLogger().log(Level.WARNING, "Error configuring GUI style", ex);
        }

        // display the form
        new SignInScreen().setVisible(true);

        // begin the update process
        ClientUpdateTask.getInstance().execute();
    }
}
