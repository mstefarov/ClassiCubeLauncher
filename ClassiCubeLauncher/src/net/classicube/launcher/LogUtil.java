package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import javax.swing.JOptionPane;

// Global logging class (to make life easier)
class LogUtil {

    private static final String MacSuffix = "/Library/Application Support";
    private static final String LauncherDirName = "net.classicube.launcher";
    private static final String LogFileName = "launcher.log";
    private static final Logger logger = Logger.getLogger(LogUtil.class.getName());

    // Sets up logging to file (%AppData%/net.classicube.launcher/launcher.log)
    public static void Init() {
        logger.setLevel(Level.FINE);

        File logFile = new File(getLauncherDir(), LogFileName);

        try {
            FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException | SecurityException ex) {
            ShowError("Could not open log file: " + ex, "Fatal error");
            System.exit(2);
        }
    }

    public static File getLauncherDir() {
        if (launcherDir == null) {
            File userDir = findUserDir();
            launcherDir = new File(userDir, LauncherDirName);
            if (launcherDir.exists()) {
                launcherDir.mkdir();
            }
        }
        return launcherDir;
    }

    // Records a log message with given severity level and message
    public static void Log(Level level, String message) {
        logger.log(level, message);
    }

    // Records a log message with given severity level, message, and exception details
    public static void Log(Level level, String message, Throwable ex) {
        logger.log(level, message, ex);
    }

    // Shows an informative modal dialog box
    public static void ShowInfo(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "Info: " + title, JOptionPane.INFORMATION_MESSAGE);
    }

    // Shows a warning modal dialog box
    public static void ShowWarning(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "Warning: " + title, JOptionPane.WARNING_MESSAGE);
    }

    // Shows an alarming modal dialog box
    public static void ShowError(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "ERROR: " + title, JOptionPane.ERROR_MESSAGE);
    }

    // Kills the process after showing and loging a message
    public static void Die(String message) {
        Log(Level.SEVERE, message);
        ShowError(message, "Fatal error");
        System.exit(2);
    }

    // Kills the process after showing and loging a message, and logging the error
    public static void Die(String message, Throwable ex) {
        Log(Level.SEVERE, message, ex);
        ShowError(message, "Fatal error");
        System.exit(2);
    }

    // Find OS-specific application data dir (reused from ClassiCubeSelfUpdater)
    private static File findUserDir() {
        String os = System.getProperty("os.name");
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
