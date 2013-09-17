package net.classicube.selfupdater;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharedUpdaterCode {

    public static final String BASE_URL = "http://static.classicube.net/client/",
            LZMA_JAR_NAME = "lzma.jar",
            LAUNCHER_DIR_NAME = ".net.classicube.launcher",
            MAC_PATH_SUFFIX = "/Library/Application Support",
            LAUNCHER_NEW_JAR_NAME = "launcher.jar.new";
    private static Constructor constructor;
    private static File launcherPath,
            appDataPath;

    public static File getLauncherDir() {
        if (launcherPath == null) {
            final File userDir = getAppDataDir();
            launcherPath = new File(userDir, LAUNCHER_DIR_NAME);
            if (launcherPath.exists()) {
                launcherPath.mkdir();
            }
        }
        return launcherPath;
    }

    public static File getAppDataDir() {
        if (appDataPath == null) {
            final String home = System.getProperty("user.home", ".");
            final OperatingSystem os = OperatingSystem.detect();

            switch (os) {
                case WINDOWS:
                    final String appData = System.getenv("APPDATA");
                    if (appData != null) {
                        appDataPath = new File(appData);
                    } else {
                        appDataPath = new File(home);
                    }
                    break;

                case MACOS:
                    appDataPath = new File(home, MAC_PATH_SUFFIX);
                    break;

                default:
                    appDataPath = new File(home);
            }
        }
        return appDataPath;
    }

    public static File processDownload(final Logger logger, final File downloadedFile, final String remoteUrl, final String namePart)
            throws FileNotFoundException, IOException {
        if (downloadedFile == null) {
            throw new NullPointerException("downloadedFile");
        }
        if (remoteUrl == null) {
            throw new NullPointerException("remoteUrl");
        }
        if (namePart == null) {
            throw new NullPointerException("namePart");
        }
        String remoteUrlLower = remoteUrl.toLowerCase();
        logger.log(Level.FINE, "processDownload({0})", namePart);

        if (remoteUrlLower.endsWith(".pack.lzma")) {
            // decompress (LZMA) and then unpack (Pack200)
            final File newFile1 = File.createTempFile(namePart, ".decompressed.tmp");
            decompressLzma(logger, downloadedFile, newFile1);
            downloadedFile.delete();
            final File newFile2 = File.createTempFile(namePart, ".unpacked.tmp");
            unpack200(newFile1, newFile2);
            newFile1.delete();
            return newFile2;

        } else if (remoteUrlLower.endsWith(".lzma")) {
            // decompress (LZMA)
            final File newFile = File.createTempFile(namePart, ".decompressed.tmp");
            decompressLzma(logger, downloadedFile, newFile);
            downloadedFile.delete();
            return newFile;

        } else if (remoteUrlLower.endsWith(".pack")) {
            // unpack (Pack200)
            final File newFile = File.createTempFile(namePart, ".unpacked.tmp");
            unpack200(downloadedFile, newFile);
            downloadedFile.delete();
            return newFile;

        } else {
            return downloadedFile;
        }
    }

    private static InputStream makeLzmaInputStream(final Logger logger, InputStream str) {
        try {
            if (constructor == null) {
                File jarFile = new File(getLauncherDir(), LZMA_JAR_NAME);
                URL[] jarUrl = new URL[]{jarFile.toURI().toURL()};
                URLClassLoader jarLoader = new URLClassLoader(jarUrl, SharedUpdaterCode.class.getClassLoader());
                Class lzmaClass = Class.forName("LZMA.LzmaInputStream", true, jarLoader);
                constructor = lzmaClass.getDeclaredConstructor(InputStream.class);
            }
            return (InputStream) constructor.newInstance(str);
        } catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException |
                SecurityException | InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException ex) {
            logger.log(Level.SEVERE, "Error creating LzmaInputStream", ex);
            return null;
        }
    }

    private static void decompressLzma(final Logger logger, final File compressedInput, final File decompressedOutput)
            throws FileNotFoundException, IOException {
        if (compressedInput == null) {
            throw new NullPointerException("compressedInput");
        }
        if (decompressedOutput == null) {
            throw new NullPointerException("decompressedOutput");
        }
        try (final FileInputStream fileIn = new FileInputStream(compressedInput)) {
            try (final BufferedInputStream bufferedIn = new BufferedInputStream(fileIn)) {
                try (final InputStream compressedIn = SharedUpdaterCode.makeLzmaInputStream(logger, bufferedIn)) {
                    try (final FileOutputStream fileOut = new FileOutputStream(decompressedOutput)) {
                        int len;
                        final byte[] ioBuffer = new byte[64 * 1024];
                        while ((len = compressedIn.read(ioBuffer)) > 0) {
                            fileOut.write(ioBuffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    private static void unpack200(final File compressedInput, final File decompressedOutput)
            throws FileNotFoundException, IOException {
        if (compressedInput == null) {
            throw new NullPointerException("compressedInput");
        }
        if (decompressedOutput == null) {
            throw new NullPointerException("decompressedOutput");
        }
        try (final FileOutputStream fostream = new FileOutputStream(decompressedOutput)) {
            try (final JarOutputStream jostream = new JarOutputStream(fostream)) {
                final Pack200.Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(compressedInput, jostream);
            }
        }
    }
}
