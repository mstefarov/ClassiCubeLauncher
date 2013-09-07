package net.classicube.launcher;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

public class ClientUpdateTask extends SwingWorker<Boolean, Boolean> {

    private static final String ClientDownloadUrl = "http://www.classicube.net/static/client/client.jar",
            ClientHashUrl = "http://www.classicube.net/static/client/client.jar.md5";

    private ClientUpdateTask() {
    }
    private static ClientUpdateTask instance = new ClientUpdateTask();

    public static ClientUpdateTask getInstance() {
        return instance;
    }
    File targetPath, clientFile;

    @Override
    protected Boolean doInBackground() throws Exception {
        LogUtil.getLogger().log(Level.FINE, "ClientUpdateTask.doInBackground");
        targetPath = PathUtil.getLauncherDir();
        clientFile = PathUtil.getClientJar();

        final boolean needsUpdate = checkForClientUpdate();

        if (needsUpdate) {
            LogUtil.getLogger().log(Level.INFO, "Downloading.");
            getClientUpdate();
            LogUtil.getLogger().log(Level.INFO, "Update applied.");
        } else {
            LogUtil.getLogger().log(Level.INFO, "No update needed.");
        }

        return needsUpdate;
    }

    private boolean checkForClientUpdate()
            throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        boolean needsUpdate;
        if (!clientFile.exists()) {
            LogUtil.getLogger().log(Level.INFO, "No local copy, will download.");
            // if local file does not exist, always update/download
            needsUpdate = true;

        } else {
            LogUtil.getLogger().log(Level.INFO, "Checking for update.");
            // else check if remote hash is different from local hash
            final String remoteString = HttpUtil.downloadString(ClientHashUrl);
            final String remoteHash = remoteString.substring(0, 32);
            if (remoteHash == null) {
                LogUtil.getLogger().log(Level.INFO, "Error downloading remote hash, aborting.");
                needsUpdate = false; // remote server is down, dont try to update
            } else {
                final String localHashString = computeLocalHash(clientFile);
                needsUpdate = !localHashString.equalsIgnoreCase(remoteHash);
            }
        }
        return needsUpdate;
    }

    private void getClientUpdate()
            throws MalformedURLException, FileNotFoundException, IOException {
        // download (or re-download) the client
        final File clientTempFile = new File(targetPath, PathUtil.ClientTempJar);
        downloadClientJar(clientTempFile);
        PathUtil.replaceFile(clientTempFile, clientFile);
    }

    private String computeLocalHash(File clientJar)
            throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        if (clientJar == null) {
            throw new NullPointerException("clientJar");
        }
        final MessageDigest digest = MessageDigest.getInstance("MD5");
        final byte[] buffer = new byte[8192];
        try (FileInputStream is = new FileInputStream(clientJar)) {
            final DigestInputStream dis = new DigestInputStream(is, digest);
            while (dis.read(buffer) != -1) {
                // DigestInputStream is doing its job, we just need to read through it.
            }
        }
        final byte[] localHashBytes = digest.digest();
        return new BigInteger(1, localHashBytes).toString(16);
    }

    private void downloadClientJar(File clientJar)
            throws MalformedURLException, FileNotFoundException, IOException {
        if (clientJar == null) {
            throw new NullPointerException("clientJar");
        }
        clientJar.delete();
        final URL website = new URL(ClientDownloadUrl);
        final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        try (FileOutputStream fos = new FileOutputStream(clientJar)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    private static final String baseDownloadUrl = "http://www.classicube.net/static/client/";
    private static final FileToDownload[] libFileNames = new FileToDownload[]{
        new FileToDownload("lwjgl.jar.pack.lzma", "libs/lwjgl.jar"),
        new FileToDownload("lwjgl_util.jar.pack.lzma", "libs/lwjgl_util.jar")
    };

    private void processDownload(File tempFile, File destinationFile)
            throws FileNotFoundException, IOException {
        LogUtil.getLogger().log(Level.FINE, "unpackLib({0})", destinationFile.getName());

        // decompress(LZMA), if needed
        if (tempFile.getName().toLowerCase().endsWith(".lzma")) {
            final File newFile = PathUtil.removeExtension(tempFile);
            decompressLZMA(tempFile, newFile);
            tempFile.delete();
            tempFile = newFile;
        }

        // unpack (Pack200), if needed
        if (tempFile.getName().toLowerCase().endsWith(".pack")) {
            final File newFile = PathUtil.removeExtension(tempFile);
            unpack200(tempFile, newFile);
            tempFile.delete();
            tempFile = newFile;
        }

        PathUtil.replaceFile(tempFile, destinationFile);
    }

    private void decompressLZMA(File compressedInput, File decompressedOutput)
            throws FileNotFoundException, IOException {
        try (FileInputStream fileIn = new FileInputStream(compressedInput)) {
            try (BufferedInputStream bufferedIn = new BufferedInputStream(fileIn)) {
                final LzmaInputStream compressedIn = new LzmaInputStream(bufferedIn, new Decoder());
                try (FileOutputStream fileOut = new FileOutputStream(decompressedOutput)) {
                    int len;
                    while ((len = compressedIn.read(lzmaBuffer)) > 0) {
                        fileOut.write(lzmaBuffer, 0, len);
                    }
                }
            }
        }
    }
    private final byte[] lzmaBuffer = new byte[65536];

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
        String baseName;
        switch (OperatingSystem.detect()) {
            case Windows:
                baseName = "windows";
                break;
            case MacOS:
                baseName = "macosx";
                break;
            case Nix:
                baseName = "linux";
                break;
            case Solaris:
                baseName = "solaris";
                break;
            default:
                throw new IllegalArgumentException();
        }
        String remoteName = baseName + "_natives.jar.lzma";
        String localName = "natives/" + baseName + "_natives.jar";
        return new FileToDownload(remoteName, localName);
    }

    private void downloadLibs() {
        List<FileToDownload> allFileNames = Arrays.asList(libFileNames);
        allFileNames.add(pickNativeDownload());
        signalBeginDownload(allFileNames.size());

        for (FileToDownload file : allFileNames) {
            String fullURL = baseDownloadUrl + file;
            signalFileChange(file.localName);
        }
    }

    static class FileToDownload {

        public String remoteName, localName;

        public FileToDownload(String remoteName, String localName) {
            this.remoteName = remoteName;
            this.localName = localName;
        }
    }

    // =============================================================================================
    //                                                                            PROGRESS SIGNALING
    // =============================================================================================
    private void signalBeginDownload(int totalFiles) {
        status.filesTotal = totalFiles;
        status.overallProgress = 10;
        signalProgress();
    }

    private void signalFileChange(String fileName) {
        status.operation = ClientUpdateStatus.Op.Downloading;
        status.fileName = fileName;
        status.bytesDownloaded = 0;
        status.bytesTotal = 1;
        status.filesProcessed++;
        status.overallProgress = 10 + (90 * status.filesProcessed) / status.filesTotal;
        signalProgress();
    }

    private void signalProgress() {
        try {
            ClientUpdateStatus clone = (ClientUpdateStatus) status.clone();
        } catch (CloneNotSupportedException ex) {
            // ignored
        }
    }
    ClientUpdateStatus status;
}