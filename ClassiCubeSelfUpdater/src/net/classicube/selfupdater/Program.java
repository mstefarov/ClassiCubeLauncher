package net.classicube.selfupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class Program {
    private static final Logger logger = Logger.getLogger(Program.class.getName());
    private static final String LauncherEntryClass = "net.classicube.launcher.EntryPoint";
    private static final String LAUNCHER_JAR_NAME = "launcher.jar";
    private static final String LauncherEntryMethod = "main";

    public static void main(String[] args) {
        File launcherDir = SharedUpdaterCode.getLauncherDir();
        File launcherJar = new File(launcherDir, LAUNCHER_JAR_NAME);
        File launcherNewJar = new File(launcherDir, SharedUpdaterCode.LAUNCHER_NEW_JAR_NAME);

        if (launcherNewJar.exists()) {
            replaceFile(launcherNewJar, launcherJar);
        } else if (!launcherJar.exists()) {
            downloadLauncher();
        }

        startLauncher(launcherJar);
    }

    private static void downloadLauncher() { // TODO: indicate progress
        File launcherDir = SharedUpdaterCode.getLauncherDir();
        File launcherJar = new File(launcherDir, LAUNCHER_JAR_NAME);
        launcherJar.getParentFile().mkdirs();
        File lzmaJar = new File(launcherDir, SharedUpdaterCode.LZMA_JAR_NAME);
        if (!lzmaJar.exists()) {
            File lzmaTempFile = downloadFile("lzma.jar");
            replaceFile(lzmaTempFile, lzmaJar);
        }
        File launcherTempFile = downloadFile("launcher.jar.pack.lzma");
        try {
            File processedLauncherFile = SharedUpdaterCode.processDownload(logger, launcherTempFile, "launcher.jar.pack.lzma", "launcher.jar");
            replaceFile(processedLauncherFile, launcherJar);
        } catch (IOException ex) {
            String message = String.format("Error unpacking the launcher.\nDebug info: %s", ex.getMessage());
            fatalError(message);
        }

    }

    private static void startLauncher(File launcherJar) {
        try {
            Class<?> lpClass = loadLauncher(launcherJar);
            Method entryPoint = lpClass.getMethod(LauncherEntryMethod, String[].class);
            entryPoint.invoke(null, (Object) new String[0]);
        } catch (IOException | NoSuchMethodException | ClassNotFoundException |
                IllegalAccessException | InvocationTargetException ex) {
            System.err.println("ClassiCubeLauncher: Error initializing: " + ex);
            System.exit(1);
        }
    }

    // Load the entry point from launcher's jar
    private static Class<?> loadLauncher(File launcherJar)
            throws IOException, ClassNotFoundException {
        URL[] urls = {new URL("jar:file:" + launcherJar + "!/")};
        URLClassLoader loader = URLClassLoader.newInstance(urls);
        return loader.loadClass(LauncherEntryClass);
    }

    private static File downloadFile(String remoteName) {
        try {
            final File tempFile = File.createTempFile(remoteName, ".downloaded");
            final URL website = new URL(SharedUpdaterCode.BASE_URL + remoteName);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            return tempFile;
        } catch (IOException ex) {
            String message = String.format("Error downloading launcher component \"%s\".\nDebug info: %s",
                    new Object[]{remoteName, ex.getMessage()});
            fatalError(message);
            return null;
        }
    }

    // Replace contents of destFile with sourceFile
    static void replaceFile(File sourceFile, File destFile) {
        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            try (FileChannel source = new FileInputStream(sourceFile).getChannel()) {
                try (FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                    destination.transferFrom(source, 0, source.size());
                }
            }

            sourceFile.delete();
        } catch (IOException ex) {
            String message = String.format("Error deploying launcher component \"%s\".\nDebug info: %s",
                    new Object[]{destFile.getName(), ex.getMessage()});
            fatalError(message);
        }
    }

    static void fatalError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}