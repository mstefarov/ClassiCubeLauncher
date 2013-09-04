package net.classicube.selfupdater;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.*;

public class Program {

    static final String MacSuffix = "/Library/Application Support";
    static final String LauncherDirName = "net.classicube.launcher";
    static final String LauncherJarName = "ClassiCubeLauncher.jar";
    static final String UpdatedLauncherJarName = "ClassiCubeLauncher.new.jar";
    static final String LauncherEntryClass = "net.classicube.launcher.EntryPoint";
    static final String LauncherEntryMethod = "main";
    static final String LauncherDownload = "http://www.classicube.net/static/launcher/ClassiCubeLauncher.jar";

    public static void main(String[] args) throws IOException {
        // Find launcher jars
        File userDir = findUserDir();
        File launcherDir = new File(userDir, LauncherDirName);
        File launcherJar = new File(launcherDir, LauncherJarName);
        File updatedJarFile = new File(launcherDir, UpdatedLauncherJarName);

        // Update launcher.jar, if needed
        if (updatedJarFile.exists()) {
            try {
                replaceFile(updatedJarFile, launcherJar);
            } catch (IOException ex) {
                System.err.println("ClassiCubeLauncher: Error updating: " + ex);
            }
        }

        if (!launcherJar.exists()) {
            downloadLauncherJar(launcherJar);
        }

        // Hand control over to the launcher
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

    // Replace contents of destFile with sourceFile
    static void replaceFile(File sourceFile, File destFile)
            throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel()) {
            try (FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
        }

        sourceFile.delete();
    }

    // Load the entry point from
    static Class<?> loadLauncher(File launcherJar)
            throws IOException, ClassNotFoundException {
        URL[] urls = {new URL("jar:file:" + launcherJar + "!/")};
        URLClassLoader loader = URLClassLoader.newInstance(urls);
        return loader.loadClass(LauncherEntryClass);
    }

    private static void downloadLauncherJar(File launcherJar) throws MalformedURLException, IOException {
        URL website = new URL(LauncherDownload);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        try (FileOutputStream fos = new FileOutputStream(launcherJar)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
}