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
    private static final String LauncherEntryClass = "net.classicube.launcher.EntryPoint";
    private static final String LAUNCHER_JAR_NAME = "launcher.jar";
    private static final String LauncherEntryMethod = "main";
    private static File launcherDir, launcherJar;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        try {
            launcherDir = SharedUpdaterCode.getLauncherDir();
        } catch (IOException ex) {
            final String message = "Error finding launcher's directory. Details:<br>" + ex;
            fatalError(message);
        }
        launcherJar = new File(launcherDir, LAUNCHER_JAR_NAME);
        final File newLauncherJar = new File(launcherDir, SharedUpdaterCode.LAUNCHER_NEW_JAR_NAME);

        initLogging();

        while (true) {
            if (newLauncherJar.exists()) {
                replaceFile(newLauncherJar, launcherJar);
            } else if (!launcherJar.exists()) {
                ProgressIndicator progressWindow = new ProgressIndicator();
                progressWindow.setVisible(true);
                downloadLauncher();
                progressWindow.dispose();
            }
            try {
                startLauncher(launcherJar);
                return;
            } catch (final Exception ex) {
                final String message = "Could not start the ClassiCube launcher: " + ex;
                Object[] options = {"Abort", "Retry"};
                int chosenOption = JOptionPane.showOptionDialog(null, message, "Error",
                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null,
                        options, options[1]);
                if (chosenOption == 0) {
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
            fatalError("Could not create log file.");
        }
    }

    private static void downloadLauncher() {
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
        } catch (Exception ex) {
            final String message = "Error unpacking the launcher. Details:<br>" + ex;
            fatalError(message);
        }
    }

    private static void startLauncher(final File launcherJar)
            throws Exception {
        final Class<?> lpClass = loadLauncher(launcherJar);
        final Method entryPoint = lpClass.getMethod(LauncherEntryMethod, String[].class);
        entryPoint.invoke(null, (Object) new String[0]);
    }

    // Load the entry point from launcher's jar
    private static Class<?> loadLauncher(final File launcherJar)
            throws IOException, ClassNotFoundException {
        final URL[] urls = {new URL("jar:file:" + launcherJar + "!/")};
        final URLClassLoader loader = URLClassLoader.newInstance(urls);
        return loader.loadClass(LauncherEntryClass);
    }

    private static File downloadFile(final String remoteName) {
        try {
            final File tempFile = File.createTempFile(remoteName, ".downloaded");
            final URL website = new URL(SharedUpdaterCode.BASE_URL + remoteName);
            final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            try (final FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            return tempFile;
        } catch (Exception ex) {
            final String message = String.format(
                    "Error downloading launcher component \"%s\". Details:<br>%s",
                    new Object[]{remoteName, ex.getMessage()});
            fatalError(message);
            return null;
        }
    }

    // Replace contents of destFile with sourceFile
    private static void replaceFile(final File sourceFile, final File destFile) {
        try {
            destFile.createNewFile();

            try (final FileChannel source = new FileInputStream(sourceFile).getChannel()) {
                try (final FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                    destination.transferFrom(source, 0, source.size());
                }
            }

            sourceFile.delete();
        } catch (final IOException ex) {
            final String message = String.format(
                    "Error deploying launcher component \"%s\". Details:<br>%s",
                    new Object[]{destFile.getName(), ex});
            fatalError(message);
        }
    }

    private static void fatalError(String message) {
        if (logger != null) {
            logger.log(Level.SEVERE, message);
        }
        JOptionPane.showMessageDialog(null,
                "<html>" + message,
                "ClassiCube launcher error",
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void deleteLauncherFiles() {
        try {
            final File[] files = launcherDir.listFiles();
            if (files == null) {  // null if security restricted
                    throw new IOException("Failed to list contents of " + launcherDir);
            }
            for (final File file : files) {
                if(!file.isDirectory() && !file.getName().toLowerCase().endsWith(".log")){
                    file.delete();
                }
            }
            
        } catch (IOException ex) {
            fatalError("Unable to recover from earlier error. Details:<br>" + ex);
        }
    }
}
