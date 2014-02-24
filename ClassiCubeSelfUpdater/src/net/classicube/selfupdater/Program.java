package net.classicube.selfupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JOptionPane;

public class Program {

    private static final Logger logger = Logger.getLogger(Program.class.getName());
    private static final String LAUNCHER_ENTRY_CLASS = "net.classicube.launcher.EntryPoint";
    private static final String LAUNCHER_JAR_NAME = "launcher.jar";
    private static final String LAUNCHER_ENTRY_METHOD = "main";
    private static final String BUG_REPORT_URL = "http://is.gd/CCL_bugs";
    private static File launcherDir, launcherJar;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        try {
            launcherDir = SharedUpdaterCode.getLauncherDir();
        } catch (IOException ex) {
            fatalError("Error finding launcher's directory.", ex);
        }
        launcherJar = new File(launcherDir, LAUNCHER_JAR_NAME);
        final File newLauncherJar = new File(launcherDir, SharedUpdaterCode.LAUNCHER_NEW_JAR_NAME);

        initLogging();

        while (true) {
            try {
                if (newLauncherJar.exists()) {
                    replaceFile(newLauncherJar, launcherJar);
                } else if (!launcherJar.exists()) {
                    ProgressIndicator progressWindow = new ProgressIndicator();
                    progressWindow.setVisible(true);
                    downloadLauncher();
                    progressWindow.dispose();
                }
                SharedUpdaterCode.testLzma(logger);
                startLauncher(launcherJar);
                return;
            } catch (final Exception ex) {
                logger.log(Level.SEVERE, "Failed to start launcher", ex);
                final String message = String.format(
                        "<html>Could not start the ClassiCube launcher:"
                        + "<blockquote><i>%s</i></blockquote>"
                        + "If clicking [Retry] does not help, please report this problem at %s",
                        new Object[]{exceptionToString(ex), BUG_REPORT_URL});
                Object[] options = {"Abort", "Retry"};
                int chosenOption = JOptionPane.showOptionDialog(null, message, "ClassiCube Launcher Error",
                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null,
                        options, options[1]);
                if (chosenOption != 1) {
                    // Abort
                    System.exit(1);
                } else {
                    // Retry: delete launcher files and re-download
                    deleteLauncherFiles();
                }
            }
        }
    }

    private static void initLogging() {
        logger.setLevel(Level.ALL);
        final File logFile = new File(launcherDir, "selfupdater.log");
        try {
            final FileHandler handler = new FileHandler(logFile.getAbsolutePath());
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (final IOException | SecurityException ex) {
            fatalError("Could not create log file:", ex);
        }
    }

    private static void downloadLauncher() throws IOException {
        final File lzmaJar = new File(launcherDir, SharedUpdaterCode.LZMA_JAR_NAME);
        if (!lzmaJar.exists()) {
            final File lzmaTempFile = downloadFile("lzma.jar");
            replaceFile(lzmaTempFile, lzmaJar);
        }
        final File launcherTempFile = downloadFile("launcher.jar.pack.lzma");
        try {
            final File processedLauncherFile = SharedUpdaterCode.processDownload(
                    logger, launcherTempFile, "launcher.jar.pack.lzma", "launcher.jar");
            replaceFile(processedLauncherFile, launcherJar);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error unpacking launcher.jar", ex);
            throw new IOException("Error unpacking launcher.jar", ex);
        }
    }

    private static void startLauncher(final File launcherJar)
            throws Exception {
        final Class<?> lpClass = loadLauncher(launcherJar);
        final Method entryPoint = lpClass.getMethod(LAUNCHER_ENTRY_METHOD, String[].class);
        entryPoint.invoke(null, new String[0]);
    }

    // Load the entry point from launcher's jar
    private static Class<?> loadLauncher(final File launcherJar)
            throws IOException, ClassNotFoundException {
        final URL[] urls = {new URL("jar:file:" + launcherJar + "!/")};
        final URLClassLoader loader = URLClassLoader.newInstance(urls);
        return loader.loadClass(LAUNCHER_ENTRY_CLASS);
    }

    private static File downloadFile(final String remoteName) throws IOException {
        try {
            final File tempFile = File.createTempFile(remoteName, ".downloaded");
            final URL website = new URL(SharedUpdaterCode.BASE_URL + remoteName);
            final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            try (final FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            return tempFile;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error downloading launcher component " + remoteName, ex);
            throw new IOException("Error downloading launcher component " + remoteName, ex);
        }
    }

    // Replace contents of destFile with sourceFile
    private static void replaceFile(final File sourceFile, final File destFile) throws IOException {
        try {
            destFile.createNewFile();

            try (final FileChannel source = new FileInputStream(sourceFile).getChannel()) {
                try (final FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                    destination.transferFrom(source, 0, source.size());
                }
            }

            sourceFile.delete();
        } catch (final IOException ex) {
            logger.log(Level.SEVERE, "Error deploying launcher component: " + destFile.getName(), ex);
            throw new IOException("Error deploying launcher component: " + destFile.getName(), ex);
        }
    }

    // Attempts to delete all files (except log files) inside launcher's directory.
    // Exits program via fatalError(...) on error.
    private static void deleteLauncherFiles() {
        try {
            final File[] files = launcherDir.listFiles();
            if (files == null) {  // null if security restricted
                throw new IOException("Failed to list contents of " + launcherDir);
            }
            for (final File file : files) {
                if (!file.isDirectory() && !file.getName().toLowerCase().endsWith(".log")) {
                    if (!file.delete()) {
                        logger.log(Level.WARNING, "Unable to delete {0}", file.getName());
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error deleting launcher files.", ex);
            fatalError("Unable to recover from an earlier error:", ex);
        }
    }

    private static void fatalError(String message, Throwable ex) {
        String htmlMessage;
        if (ex != null) {
            htmlMessage = String.format(
                    "<html>%s<blockquote><i>%s</i></blockquote>If this problem persists, contact us at %s",
                    new Object[]{message, exceptionToString(ex), BUG_REPORT_URL});
        } else {
            htmlMessage = String.format(
                    "<html>%s<br>If this problem persists, contact us at <u>%s</u>",
                    new Object[]{message, BUG_REPORT_URL});
        }
        JOptionPane.showMessageDialog(null,
                htmlMessage, "ClassiCube Launcher Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static String exceptionToString(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        do {
            if (sb.length() > 0) {
                sb.append("<br>caused by ");
            }
            StackTraceElement frame = ex.getStackTrace()[0];
            sb.append(ex)
                    .append("<br>&nbsp;&nbsp;&nbsp;&nbsp;at ")
                    .append(frame.getClassName()).append('.').append(frame.getMethodName());
            ex = ex.getCause();
        } while (ex != null);
        return sb.toString();
    }
}
