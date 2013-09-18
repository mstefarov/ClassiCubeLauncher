package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JOptionPane;

// Global logging class (to make life easier)
final class LogUtil {

    private static final Logger logger = Logger.getLogger(LogUtil.class.getName());

    // Sets up logging to file (%AppData%/net.classicube.launcher/launcher.log)
    public static void Init() {
        logger.setLevel(Level.ALL);

        final File logFile = PathUtil.getLogFile();

        try {
            final FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (final IOException | SecurityException ex) {
            ErrorScreen.show(null, "Error creating log file", ex.getMessage(), ex);
            System.exit(2);
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    // Shows an informative modal dialog box
    public static void showInfo(final String message, final String title) {
        JOptionPane.showMessageDialog(null, message, "Info: " + title, JOptionPane.INFORMATION_MESSAGE);
    }
}
