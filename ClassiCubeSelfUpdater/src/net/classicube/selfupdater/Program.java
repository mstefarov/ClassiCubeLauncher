package net.classicube.selfupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Program {

    static final String MacSuffix = "/Library/Application Support";
    static final String LauncherDirName = "net.classicube.launcher";
    static final String LauncherJarName = "ClassiCubeLauncher.jar";
    static final String UpdatedLauncherJarName = "ClassiCubeLauncher.new.jar";
    static final String LauncherEntryClass = "net.classicube.launcher.EntryPoint";
    static final String LauncherEntryMethod = "Run";

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

        // Hand control over to the launcher
        try {
            Class<?> lpClass = loadLauncher(launcherJar);
            Method entryPoint = lpClass.getMethod(LauncherEntryMethod);
            entryPoint.invoke(null);
        } catch (IOException | NoSuchMethodException | ClassNotFoundException |
                IllegalAccessException | InvocationTargetException ex) {
            System.err.println("ClassiCubeLauncher: Error initializing: " + ex);
        }

        System.in.read();
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

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
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
}