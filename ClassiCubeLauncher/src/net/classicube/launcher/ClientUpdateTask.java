package net.classicube.launcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

final class ClientUpdateTask extends SwingWorker<Boolean, ClientUpdateTask.ProgressUpdate> {
    // =============================================================================================
    //                                                                    CONSTANTS & INITIALIZATION
    // =============================================================================================

    private static final String BaseUrl = "http://www.classicube.net/static/client/",
            ClientHashUrl = BaseUrl + "client.jar.md5",
            LauncherHashUrl = BaseUrl + "ClassiCubeLauncher.jar.md5";
    private static final ClientUpdateTask instance = new ClientUpdateTask();

    public static ClientUpdateTask getInstance() {
        return instance;
    }

    private ClientUpdateTask() {
    }
    // =============================================================================================
    //                                                                                          MAIN
    // =============================================================================================
    private final List<FileToDownload> files = new ArrayList<>();

    @Override
    protected Boolean doInBackground()
            throws Exception {
        digest = MessageDigest.getInstance("MD5");
        Logger logger = LogUtil.getLogger();

        // step 1: build up file list
        logger.log(Level.INFO, "Checking for updates.");
        findFilesToDownload();

        if (files.isEmpty()) {
            logger.log(Level.INFO, "No updates needed.");
        } else {
            logger.log(Level.INFO, "Downloading updates: {0}", listFileNames(files));

            activeFile = 0;
            for (FileToDownload file : files) {
                try {
                    // step 2: download
                    signalDownloadProgress();
                    final File downloadedFile = downloadFile(file);

                    // step 3: unpack
                    signalUnpackProgress();
                    final File processedFile = processDownload(downloadedFile, file);

                    // step 4: deploy
                    deployFile(processedFile, file.localName);

                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Error while downloading update.", ex);
                }
                activeFile++;
            }
            logger.log(Level.INFO, "Updates applied.");
        }

        return true;
    }

    private void findFilesToDownload() {
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
    }

    private static String listFileNames(List<FileToDownload> files) {
        final StringBuilder sb = new StringBuilder();
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
    private final byte[] ioBuffer = new byte[64 * 1024];

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
        if (localFile == null) {
            throw new NullPointerException("localFile");
        }
        if (hashUrl == null) {
            throw new NullPointerException("hashUrl");
        }
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
                    final String localHashString = computeLocalHash(localFile);
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

    private File downloadFile(FileToDownload file)
            throws MalformedURLException, FileNotFoundException, IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        final File tempFile = File.createTempFile(file.localName.getName(), ".downloaded");
        final URL website = new URL(file.remoteUrl);
        final int fileSize = website.openConnection().getContentLength(); // TODO: work around lack of content-length

        try (InputStream siteIn = website.openStream()) {
            try (FileOutputStream fileOut = new FileOutputStream(tempFile)) {
                int len;
                int total = 0;
                while ((len = siteIn.read(ioBuffer)) > 0) {
                    fileOut.write(ioBuffer, 0, len);
                    total += len;
                    signalDownloadPercent(total, fileSize);
                }
            }
        }
        return tempFile;
    }

    // =============================================================================================
    //                                                                      POST-DOWNLOAD PROCESSING
    // =============================================================================================
    private File processDownload(File rawFile, FileToDownload fileInfo)
            throws FileNotFoundException, IOException {
        if (rawFile == null) {
            throw new NullPointerException("rawFile");
        }
        if (fileInfo == null) {
            throw new NullPointerException("fileInfo");
        }
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
        if (compressedInput == null) {
            throw new NullPointerException("compressedInput");
        }
        if (decompressedOutput == null) {
            throw new NullPointerException("decompressedOutput");
        }
        LogUtil.getLogger().log(Level.FINE, "LZMA: {0}", compressedInput.getName());
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
        if (compressedInput == null) {
            throw new NullPointerException("compressedInput");
        }
        if (decompressedOutput == null) {
            throw new NullPointerException("decompressedOutput");
        }
        LogUtil.getLogger().log(Level.FINE, "unpack200: {0}", compressedInput.getName());
        try (FileOutputStream fostream = new FileOutputStream(decompressedOutput)) {
            try (JarOutputStream jostream = new JarOutputStream(fostream)) {
                final Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(compressedInput, jostream);
            }
        }
    }

    private void deployFile(File processedFile, File localName) {
        if (processedFile == null) {
            throw new NullPointerException("processedFile");
        }
        if (localName == null) {
            throw new NullPointerException("localName");
        }
        LogUtil.getLogger().log(Level.INFO, "deployFile({0})", localName.getName());
        try {
            final File parentDir = localName.getCanonicalFile().getParentFile();
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

    protected void extractNatives(File jarPath)
            throws FileNotFoundException, IOException {
        if (jarPath == null) {
            throw new NullPointerException("jarPath");
        }
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
                        try (FileOutputStream out = new FileOutputStream(outFile)) {
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
    // =============================================================================================
    //                                                                            PROGRESS REPORTING
    // =============================================================================================
    private volatile ClientUpdateScreen updateScreen;
    int activeFile;

    public void registerUpdateScreen(ClientUpdateScreen updateScreen) {
        if (updateScreen == null) {
            throw new NullPointerException("updateScreen");
        }
        this.updateScreen = updateScreen;
    }

    @Override
    protected void process(List<ProgressUpdate> chunks) {
        if (chunks == null) {
            throw new NullPointerException("chunks");
        }
        ClientUpdateScreen screen = updateScreen;
        if (screen != null) {
            screen.setStatus(chunks.get(chunks.size() - 1));
        }
    }

    private void signalCheckProgress(int step, String fileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName");
        }
        int overallProgress = step * 5; // between 0 and 15%
        String status = String.format("Checking for updates...", fileName);
        publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDownloadProgress() {
        int overallProgress = 15 + (activeFile * 170) / (files.size() * 2);
        String fileName = files.get(activeFile).localName.getName();
        String status = "Downloading...";
        publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDownloadPercent(int bytesSoFar, int bytesTotal) {
        String fileName = files.get(activeFile).localName.getName();
        String status;
        int overallProgress;
        if (bytesTotal > 0 && bytesTotal < bytesSoFar) {
            int percent = Math.max(0, Math.min(100, (bytesSoFar * 100) / bytesTotal));
            int baseProgress = 15 + (activeFile * 170) / (files.size() * 2);
            int deltaProgress = 15 + ((activeFile + 1) * 170) / (files.size() * 2) - baseProgress;
            overallProgress = baseProgress + (deltaProgress * percent) / 100;
            status = String.format("Downloading (%s / %s)", bytesSoFar, bytesTotal);
        } else {
            status = String.format("Downloading... (%s)", bytesSoFar);
            overallProgress = 15 + (activeFile * 170) / (files.size() * 2);
        }
        publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalUnpackProgress() {
        int overallProgress = 15 + ((activeFile + 1) * 170) / (files.size() * 2);
        String fileName = files.get(activeFile).localName.getName();
        String status = String.format("Unpacking...", fileName);
        publish(new ProgressUpdate(fileName, status, overallProgress));
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

    public class ProgressUpdate {

        public String fileName;
        public String status;
        public int progress;

        public ProgressUpdate(String action, String status, int progress) {
            this.fileName = action;
            this.status = status;
            this.progress = progress;
        }
    }
}