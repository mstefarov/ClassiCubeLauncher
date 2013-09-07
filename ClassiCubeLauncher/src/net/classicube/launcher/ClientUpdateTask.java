package net.classicube.launcher;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Level;
import javax.swing.SwingWorker;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

public class ClientUpdateTask extends SwingWorker<Boolean, Boolean> {

    MessageDigest digest;
    private static final String ClientHashUrl = "client.jar.md5",
            LauncherHashUrl = "ClassiCubeLauncher.jar.md5",
            BaseUrl = "http://www.classicube.net/static/client/";

    private ClientUpdateTask() {
    }
    private static ClientUpdateTask instance = new ClientUpdateTask();

    public static ClientUpdateTask getInstance() {
        return instance;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        digest = MessageDigest.getInstance("MD5");

        LogUtil.getLogger().log(Level.FINE, "ClientUpdateTask.doInBackground");

        // step 1: build up file list
        LogUtil.getLogger().log(Level.INFO, "Checking for updates.");
        List<FileToDownload> files = findFilesToDownload();

        if (files.isEmpty()) {
            LogUtil.getLogger().log(Level.INFO, "No updates needed.");
        } else {
            LogUtil.getLogger().log(Level.INFO, "Downloading updates.");
            for (FileToDownload file : files) {
                try {
                    // step 2: download
                    File downloadedFile = downloadFile(file);

                    // step 3: unpack
                    File processedFile = processDownload(downloadedFile, file.localName);

                    // step 4: deploy
                    PathUtil.replaceFile(processedFile, file.localName);

                } catch (IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE, "Error while downloading update.", ex);
                }
            }
            LogUtil.getLogger().log(Level.INFO, "Updates applied.");
        }

        return true;
    }

    private List<FileToDownload> findFilesToDownload() {
        List<FileToDownload> files = new ArrayList<>();

        File clientDir = PathUtil.getClientDir();
        File launcherDir = PathUtil.getLauncherDir();

        if (checkForLauncherUpdate()) {
            files.add(new FileToDownload(
                    BaseUrl + "ClassiCubeLauncher.jar",
                    new File(launcherDir, "ClassiCubeLauncher.jar.new")));
        }
        if (checkForClientUpdate()) {
            files.add(new FileToDownload(
                    BaseUrl + "client.jar",
                    new File(clientDir, "client.jar")));
        }
        if (checkForLibraries()) {
            files.add(new FileToDownload(
                    BaseUrl + "lwjgl.jar.pack.lzma",
                    new File(clientDir, "libs/lwjgl.jar")));
            files.add(new FileToDownload(
                    BaseUrl + "lwjgl_util.jar.pack.lzma",
                    new File(clientDir, "libs/lwjgl_util.jar")));
            files.add(pickNativeDownload());
        }
        return files;
    }

    private boolean checkForLauncherUpdate() {
        File launcherFile = new File(PathUtil.getLauncherDir(), "ClassiCubeLauncher.jar");
        return checkUpdateByHash(launcherFile, LauncherHashUrl);
    }

    private boolean checkForClientUpdate() {
        File clientFile = new File(PathUtil.getClientDir(), "client.jar");
        return checkUpdateByHash(clientFile, ClientHashUrl);
    }

    private boolean checkUpdateByHash(File localFile, String hashUrl) {
        String name = localFile.getName();
        if (!localFile.exists()) {
            LogUtil.getLogger().log(Level.FINE, "{0}: No local copy, will download.", name);
            // if local file does not exist, always update/download
            return true;

        } else {
            // else check if remote hash is different from local hash
            final String remoteString = HttpUtil.downloadString(hashUrl);
            final String remoteHash = remoteString.substring(0, 32);
            if (remoteHash == null) {
                LogUtil.getLogger().log(Level.FINE, "{0}: Error downloading remote hash, aborting.", name);
                return false; // remote server is down, dont try to update
            } else {
                try {
                    final String localHashString;
                    localHashString = computeLocalHash(localFile);
                    return !localHashString.equalsIgnoreCase(remoteHash);
                } catch (IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE, name + ": Error computing local hash, aborting.", ex);
                    return false;
                }
            }
        }
    }

    private String computeLocalHash(File clientJar)
            throws FileNotFoundException, IOException {
        if (clientJar == null) {
            throw new NullPointerException("clientJar");
        }
        try (FileInputStream is = new FileInputStream(clientJar)) {
            final DigestInputStream dis = new DigestInputStream(is, digest);
            while (dis.read(ioBuffer) != -1) {
                // DigestInputStream is doing its job, we just need to read through it.
            }
        }
        final byte[] localHashBytes = digest.digest();
        return new BigInteger(1, localHashBytes).toString(16);
    }

    private File downloadFile(FileToDownload file)
            throws MalformedURLException, FileNotFoundException, IOException {
        File tempFile = File.createTempFile(file.localName.getName(), ".downloaded");
        final URL website = new URL(file.remoteUrl);
        final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // todo: progress updates
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return tempFile;
    }

    private File processDownload(File tempFile, File destinationFile)
            throws FileNotFoundException, IOException {
        LogUtil.getLogger().log(Level.FINE, "processDownload({0})", destinationFile.getName());
        String targetName = destinationFile.getName().toLowerCase();

        // decompress(LZMA), if needed
        if (targetName.endsWith(".lzma")) {
            File newFile = File.createTempFile(targetName, ".tmp");
            decompressLZMA(tempFile, newFile);
            tempFile = newFile;
        }

        // unpack (Pack200), if needed
        if (targetName.contains(".pack.")) {
            File newFile = File.createTempFile(targetName, ".tmp");
            unpack200(tempFile, newFile);
            tempFile = newFile;
        }
        return tempFile;
    }

    private void decompressLZMA(File compressedInput, File decompressedOutput)
            throws FileNotFoundException, IOException {
        try (FileInputStream fileIn = new FileInputStream(compressedInput)) {
            try (BufferedInputStream bufferedIn = new BufferedInputStream(fileIn)) {
                final LzmaInputStream compressedIn = new LzmaInputStream(bufferedIn, new Decoder());
                try (FileOutputStream fileOut = new FileOutputStream(decompressedOutput)) {
                    int len;
                    while ((len = compressedIn.read(ioBuffer)) > 0) {
                        fileOut.write(ioBuffer, 0, len);
                    }
                }
            }
        }
    }
    private final byte[] ioBuffer = new byte[65536];

    private void unpack200(File compressedInput, File decompressedOutput)
            throws FileNotFoundException, IOException {
        try (FileOutputStream fostream = new FileOutputStream(decompressedOutput)) {
            try (JarOutputStream jostream = new JarOutputStream(fostream)) {
                final Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(compressedInput, jostream);
            }
        }
    }

    private FileToDownload pickNativeDownload() {
        String osName;
        switch (OperatingSystem.detect()) {
            case Windows:
                osName = "windows";
                break;
            case MacOS:
                osName = "macosx";
                break;
            case Nix:
                osName = "linux";
                break;
            case Solaris:
                osName = "solaris";
                break;
            default:
                throw new IllegalArgumentException();
        }
        String remoteName = BaseUrl + osName + "_natives.jar.lzma";
        File localPath = new File("natives/" + osName + "_natives.jar");
        return new FileToDownload(remoteName, localPath);
    }

    private boolean checkForLibraries() {
        File libDir = new File(PathUtil.getClientDir(), "libs");
        FileToDownload nativeLib = pickNativeDownload();
        File mainLib = new File(libDir, "lwjgl.jar");
        File mainUtilLib = new File(libDir, "lwjgl_util.jar");
        return !libDir.exists()
                || !nativeLib.localName.exists()
                || !mainLib.exists()
                || !mainUtilLib.exists();
    }

    static class FileToDownload {

        public String remoteUrl;
        public File localName;

        public FileToDownload(String remoteName, File localName) {
            this.remoteUrl = remoteName;
            this.localName = localName;
        }
    }
}