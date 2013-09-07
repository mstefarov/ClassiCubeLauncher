package net.classicube.launcher;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Level;
import javax.swing.SwingWorker;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

public class ClientUpdateTask extends SwingWorker<Boolean, ClientUpdateStatus> {
    // =============================================================================================
    //                                                                    CONSTANTS & INITIALIZATION
    // =============================================================================================

    private static final String BaseUrl = "http://www.classicube.net/static/client/",
            ClientHashUrl = BaseUrl + "client.jar.md5",
            LauncherHashUrl = BaseUrl + "ClassiCubeLauncher.jar.md5";
    private static ClientUpdateTask instance = new ClientUpdateTask();

    public static ClientUpdateTask getInstance() {
        return instance;
    }

    private ClientUpdateTask() {
    }

    // =============================================================================================
    //                                                                                          MAIN
    // =============================================================================================
    @Override
    protected Boolean doInBackground() throws Exception {
        digest = MessageDigest.getInstance("MD5");

        LogUtil.getLogger().log(Level.FINE, "ClientUpdateTask.doInBackground");

        // step 1: build up file list
        LogUtil.getLogger().log(Level.INFO, "Checking for updates.");
        final List<FileToDownload> files = findFilesToDownload();

        if (files.isEmpty()) {
            LogUtil.getLogger().log(Level.INFO, "No updates needed.");
        } else {
            LogUtil.getLogger().log(Level.INFO, "Downloading updates: {0}", listFileNames(files));
            int i = 0;
            for (FileToDownload file : files) {
                try {
                    // step 2: download
                    this.signalDownloadProgress(files, i);
                    final File downloadedFile = downloadFile(file);

                    // step 3: unpack
                    this.signalUnpackProgress(files, i);
                    final File processedFile = processDownload(downloadedFile, file);

                    // step 4: deploy
                    deployFile(processedFile, file.localName);

                } catch (IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE, "Error while downloading update.", ex);
                }
                i++;
            }
            LogUtil.getLogger().log(Level.INFO, "Updates applied.");
        }

        return true;
    }

    private List<FileToDownload> findFilesToDownload() {
        final List<FileToDownload> files = new ArrayList<>();

        final File clientDir = PathUtil.getClientDir();
        final File launcherDir = PathUtil.getLauncherDir();

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
            files.add(new FileToDownload(
                    BaseUrl + "jinput.jar.pack.lzma",
                    new File(clientDir, "libs/jinput.jar")));
            files.add(pickNativeDownload());
        }
        return files;
    }

    private static String listFileNames(List<FileToDownload> files) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (FileToDownload s : files) {
            sb.append(sep).append(s.localName.getName());
            sep = ", ";
        }
        return sb.toString();
    }
    // =============================================================================================
    //                                                                        CHECKING / DOWNLOADING
    // =============================================================================================
    private MessageDigest digest;
    private final byte[] ioBuffer = new byte[65536];

    private boolean checkForLauncherUpdate() {
        signalCheckProgress(0, "launcher");
        final File launcherFile = new File(PathUtil.getLauncherDir(), "ClassiCubeLauncher.jar");
        return checkUpdateByHash(launcherFile, LauncherHashUrl);
    }

    private boolean checkForClientUpdate() {
        signalCheckProgress(1, "client");
        final File clientFile = new File(PathUtil.getClientDir(), "client.jar");
        return checkUpdateByHash(clientFile, ClientHashUrl);
    }

    private boolean checkUpdateByHash(File localFile, String hashUrl) {
        final String name = localFile.getName();
        if (!localFile.exists()) {
            LogUtil.getLogger().log(Level.FINE, "{0}: No local copy, will download.", name);
            // if local file does not exist, always update/download
            return true;

        } else {
            // else check if remote hash is different from local hash
            final String remoteString = HttpUtil.downloadString(hashUrl);
            final String remoteHash = remoteString.substring(0, 32);
            if (remoteHash == null) {
                LogUtil.getLogger().log(Level.FINE,
                        "{0}: Error downloading remote hash, aborting.", name);
                return false; // remote server is down, dont try to update
            } else {
                try {
                    final String localHashString;
                    localHashString = computeLocalHash(localFile);
                    return !localHashString.equalsIgnoreCase(remoteHash);
                } catch (IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE,
                            name + ": Error computing local hash, aborting.", ex);
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

    private boolean checkForLibraries() {
        signalCheckProgress(2, "library");
        final File libDir = new File(PathUtil.getClientDir(), "libs");
        final FileToDownload nativeLib = pickNativeDownload();
        final File libLwjgl = new File(libDir, "lwjgl.jar");
        final File libLwjglUtil = new File(libDir, "lwjgl_util.jar");
        final File libJInput = new File(libDir, "jinput.jar");
        return !libDir.exists()
                || !nativeLib.localName.exists()
                || !libLwjgl.exists()
                || !libLwjglUtil.exists()
                || !libJInput.exists();
    }

    private static FileToDownload pickNativeDownload() {
        final String osName;
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
        final String remoteName = BaseUrl + osName + "_natives.jar.lzma";
        final File localPath = new File(PathUtil.getClientDir(),
                "natives/" + osName + "_natives.jar");
        return new FileToDownload(remoteName, localPath);
    }

    private static File downloadFile(FileToDownload file)
            throws MalformedURLException, FileNotFoundException, IOException {
        final File tempFile = File.createTempFile(file.localName.getName(), ".downloaded");
        final URL website = new URL(file.remoteUrl);
        final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // todo: progress updates
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return tempFile;
    }

    // =============================================================================================
    //                                                                      POST-DOWNLOAD PROCESSING
    // =============================================================================================
    private File processDownload(File rawFile, FileToDownload fileInfo)
            throws FileNotFoundException, IOException {
        LogUtil.getLogger().log(Level.FINE, "processDownload({0})", fileInfo.localName.getName());
        final String remoteUrlLower = fileInfo.remoteUrl.toLowerCase();
        final String namePart = fileInfo.localName.getName();

        if (remoteUrlLower.endsWith(".pack.lzma")) {
            // decompress (LZMA) and then unpack (Pack200)
            final File newFile1 = File.createTempFile(namePart, ".decompressed.tmp");
            decompressLzma(rawFile, newFile1);
            rawFile.delete();
            final File newFile2 = File.createTempFile(namePart, ".unpacked.tmp");
            unpack200(newFile1, newFile2);
            newFile1.delete();
            return newFile2;

        } else if (remoteUrlLower.endsWith(".lzma")) {
            // decompress (LZMA)
            final File newFile = File.createTempFile(namePart, ".decompressed.tmp");
            decompressLzma(rawFile, newFile);
            rawFile.delete();
            return newFile;

        } else if (remoteUrlLower.endsWith(".pack")) {
            // unpack (Pack200)
            final File newFile = File.createTempFile(namePart, ".unpacked.tmp");
            unpack200(rawFile, newFile);
            rawFile.delete();
            return newFile;

        } else {
            return rawFile;
        }
    }

    private void decompressLzma(File compressedInput, File decompressedOutput)
            throws FileNotFoundException, IOException {
        LogUtil.getLogger().info("LZMA: " + compressedInput.getName());
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

    private static void unpack200(File compressedInput, File decompressedOutput)
            throws FileNotFoundException, IOException {
        LogUtil.getLogger().info("unpack200: " + compressedInput.getName());
        try (FileOutputStream fostream = new FileOutputStream(decompressedOutput)) {
            try (JarOutputStream jostream = new JarOutputStream(fostream)) {
                final Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(compressedInput, jostream);
            }
        }
    }
    // =============================================================================================
    //                                                                            PROGRESS REPORTING
    // =============================================================================================
    private volatile ClientUpdateScreen updateScreen;

    public void registerUpdateScreen(ClientUpdateScreen updateScreen) {
        this.updateScreen = updateScreen;
    }

    @Override
    protected void process(List<ClientUpdateStatus> chunks) {
        ClientUpdateScreen screen = updateScreen;
        if (screen != null) {
            screen.setStatus(chunks.get(chunks.size() - 1));
        }
    }

    private void signalCheckProgress(int step, String fileName) {
        int overallProgress = step * 5; // between 0 and 15%
        String status = String.format("Checking for updates...", fileName);
        publish(new ClientUpdateStatus(fileName, status, overallProgress));
    }

    private void signalDownloadProgress(List<FileToDownload> files, int i) {
        int overallProgress = 15 + (i * 170) / (files.size() * 2);
        String fileName = files.get(i).localName.getName();
        String status = String.format("Downloading...", fileName);
        publish(new ClientUpdateStatus(fileName, status, overallProgress));
    }

    private void signalUnpackProgress(List<FileToDownload> files, int i) {
        int overallProgress = 15 + ((i + 1) * 170) / (files.size() * 2);
        String fileName = files.get(i).localName.getName();
        String status = String.format("Unpacking...", fileName);
        publish(new ClientUpdateStatus(fileName, status, overallProgress));
    }

    private void deployFile(File processedFile, File localName) {
        LogUtil.getLogger().log(Level.INFO, "deployFile({0})", localName.getName());
        try {
            File parentDir = localName.getCanonicalFile().getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            PathUtil.replaceFile(processedFile, localName);
            if (localName.getName().endsWith("natives.jar")) {
                extractNatives(localName);
            }
        } catch (IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error deploying " + localName.getName(), ex);
        }
    }

    @Override
    protected void done() {
        ClientUpdateScreen screen = updateScreen;
        if (screen != null) {
            screen.onUpdateDone();
        }
    }

    // =============================================================================================
    //                                                                                   INNER TYPES
    // =============================================================================================
    private static class FileToDownload {

        public final String remoteUrl;
        public final File localName;

        public FileToDownload(String remoteName, File localName) {
            this.remoteUrl = remoteName;
            this.localName = localName;
        }
    }

    protected void extractNatives(File jarPath)
            throws FileNotFoundException, IOException {
        LogUtil.getLogger().log(Level.FINE, "extractNatives({0})", jarPath.getName());

        File nativeFolder = new File(PathUtil.getClientDir(), "natives");

        if (!nativeFolder.exists()) {
            nativeFolder.mkdir();
        }

        try (JarFile jarFile = new JarFile(jarPath, true)) {
            Enumeration entities = jarFile.entries();

            while (entities.hasMoreElements()) {
                JarEntry entry = (JarEntry) entities.nextElement();

                if (!entry.isDirectory() && (entry.getName().indexOf('/') == -1)) {

                    File outFile = new File(nativeFolder, entry.getName());

                    if (outFile.exists() && !outFile.delete()) {
                        LogUtil.getLogger().log(Level.SEVERE,
                                "Could not replace native file: {0}", entry.getName());
                        continue;
                    }

                    try (InputStream in = jarFile.getInputStream(entry)) {
                        try (OutputStream out = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[65536];
                            int bufferSize;
                            while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
                                out.write(buffer, 0, bufferSize);
                            }
                        }
                    }
                }
            }
        }
    }
}