package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import javax.swing.JOptionPane;

// Global logging class (to make life easier)
class LogUtil {

    private static final String MacSuffix = "/Library/Application Support",
            LauncherDirName = "net.classicube.launcher",
            LogFileName = "launcher.log";
    private static final Logger logger = Logger.getLogger(LogUtil.class.getName());

    // Sets up logging to file (%AppData%/net.classicube.launcher/launcher.log)
    public static void Init() {
        logger.setLevel(Level.FINE);

        final File logFile = new File(getLauncherDir(), LogFileName);

        try {
            final FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException | SecurityException ex) {
            showError("Could not open log file: " + ex, "Fatal error");
            System.exit(2);
        }
    }

    public static File getLauncherDir() {
        if (launcherDir == null) {
            final File userDir = findUserDir();
            launcherDir = new File(userDir, LauncherDirName);
            if (launcherDir.exists()) {
                launcherDir.mkdir();
            }
        }
        return launcherDir;
    }

    public static Logger getLogger(){
        return logger;
    }

    // Shows an informative modal dialog box
    public static void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "Info: " + title, JOptionPane.INFORMATION_MESSAGE);
    }

    // Shows a warning modal dialog box
    public static void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "Warning: " + title, JOptionPane.WARNING_MESSAGE);
    }

    // Shows an alarming modal dialog box
    public static void showError(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "ERROR: " + title, JOptionPane.ERROR_MESSAGE);
    }

    // Kills the process after showing and loging a message
    public static void die(String message) {
        getLogger().log(Level.SEVERE, message);
        showError(message, "Fatal error");
        System.exit(2);
    }

    // Kills the process after showing and loging a message, and logging the error
    public static void die(String message, Throwable ex) {
        getLogger().log(Level.SEVERE, message, ex);
        showError(message, "Fatal error");
        System.exit(2);
    }

    // Find OS-specific application data dir (reused from ClassiCubeSelfUpdater)
    private static File findUserDir() {
        final String os = System.getProperty("os.name");
        String path;
        if (os.contains("Windows")) {
            path = System.getenv("AppData");
        } else {
            path = System.getProperty("user.home");
            if (os.contains("MacOS")) {
                path += MacSuffix;
            }
        }
        return new File(path);
    }
    private static File launcherDir;
}
