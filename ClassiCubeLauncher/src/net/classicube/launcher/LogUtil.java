package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import javax.swing.JOptionPane;

public class LogUtil {

    private static final Logger logger = Logger.getLogger(LogUtil.class.getName());

    public static void Init() {
        logger.setLevel(Level.FINE);

        File userDir = findUserDir();
        File launcherDir = new File(userDir, LauncherDirName);
        File logFile = new File(launcherDir, "launcher.log");

        try {
            FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException | SecurityException ex) {
            ShowError("Could not open log file: " + ex, "Fatal error");
            System.exit(2);
        }
    }

    public static void Log(Level level, String message) {
        logger.log(level, message);
    }

    public static void Log(Level level, String message, Throwable ex) {
        logger.log(level, message, ex);
    }

    public static void ShowInfo(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "Info: " + title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void ShowWarning(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "Warning: " + title, JOptionPane.WARNING_MESSAGE);
    }

    public static void ShowError(String message, String title) {
        JOptionPane.showMessageDialog(null, message, "ERROR: " + title, JOptionPane.ERROR_MESSAGE);
    }

    // Find OS-specific application data dir
    static File findUserDir() {
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
    static final String MacSuffix = "/Library/Application Support";
    static final String LauncherDirName = "net.classicube.launcher";
}
