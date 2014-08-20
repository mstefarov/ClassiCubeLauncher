package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import net.classicube.launcher.gui.ErrorScreen;
import net.classicube.shared.SharedUpdaterCode;

// Global logging class (to make life easier)
public final class LogUtil {

    public static final String VERSION_STRING = "ClassiCube Launcher - b60";
    private static final Logger logger = Logger.getLogger(LogUtil.class.getName());
    private static BroadcastingPrintStream outProxy, errProxy;

    public static boolean addConsoleListener(PrintStream stream) {
        return outProxy.addListener(stream) && errProxy.addListener(stream);
    }

    // Sets up logging to file (%AppData%/net.classicube.launcher/launcher.log)
    public static void init() throws IOException {
        logger.setLevel(Level.ALL);
        outProxy = new BroadcastingPrintStream(System.out);
        errProxy = new BroadcastingPrintStream(System.err);
        System.setOut(outProxy);
        System.setErr(errProxy);

        final File logFile = new File(SharedUpdaterCode.getLauncherDir(), PathUtil.LOG_FILE_NAME);
        final File logOldFile = new File(SharedUpdaterCode.getLauncherDir(), PathUtil.LOG_OLD_FILE_NAME);

        // If a logfile already exists, rename it to "launcher.old.log"
        if (logFile.exists()) {
            if (logOldFile.exists()) {
                logOldFile.delete();
            }
            logFile.renameTo(logOldFile);
        }

        // Set up log file handler for this session
        try {
            final FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (final IOException | SecurityException ex) {
            ErrorScreen.show("Error creating log file", ex.getMessage(), ex);
            System.exit(2);
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    private LogUtil() {
    }

    static class BroadcastingPrintStream extends PrintStream {

        HashSet<PrintStream> listeners = new HashSet<>();

        public BroadcastingPrintStream(OutputStream out) {
            super(out);
        }

        public boolean addListener(PrintStream stream) {
            return listeners.add(stream);
        }

        public boolean removeListener(PrintStream stream) {
            return listeners.remove(stream);
        }

        @Override
        public void write(int b) {
            for (PrintStream listener : listeners) {
                listener.write(b);
            }
            super.write(b);
        }

        @Override
        public void write(byte[] buf) throws IOException {
            for (PrintStream listener : listeners) {
                listener.write(buf);
            }
            super.write(buf);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            for (PrintStream listener : listeners) {
                listener.write(buf, off, len);
            }
            super.write(buf, off, len);
        }

        @Override
        public void flush() {
            for (PrintStream listener : listeners) {
                listener.flush();
            }
            super.flush();
        }

        @Override
        public void close() {
            for (PrintStream listener : listeners) {
                listener.close();
            }
            super.close();
        }
    }
}
