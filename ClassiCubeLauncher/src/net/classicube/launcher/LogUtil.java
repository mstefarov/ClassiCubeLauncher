package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import javax.swing.JOptionPane;

// Global logging class (to make life easier)
class LogUtil {

    private static final Logger logger = Logger.getLogger(LogUtil.class.getName());

    // Sets up logging to file (%AppData%/net.classicube.launcher/launcher.log)
    public static void Init() {
        logger.setLevel(Level.ALL);

        final File logFile = PathUtil.getLogFile();

        try {
            final FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException | SecurityException ex) {
            showError("Could not open log file: " + ex, "Fatal error");
            System.exit(2);
        }
    }

    public static Logger getLogger() {
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
}
