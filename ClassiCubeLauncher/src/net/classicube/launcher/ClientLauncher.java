package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

final public class ClientLauncher {

    private static final String ClassPath = "client.jar;libs/*",
            ClientClassPath = "com.oyasunadev.mcraft.client.core.ClassiCubeStandalone";

    public static void launchClient() {
        LogUtil.getLogger().info("launchClient");
        final ServerJoinInfo joinInfo = SessionManager.getJoinInfo();
        final File java = getJavaPath();

        final String nativePath;
        try {
            nativePath = new File(PathUtil.getClientDir(), "natives").getCanonicalPath();
        } catch (IOException ex) {
            LogUtil.die(ex.toString());
            return;
        }

        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(
                    java.getAbsolutePath(),
                    "-cp",
                    ClassPath,
                    "-Djava.library.path=" + nativePath,
                    Prefs.getJavaArgs(),
                    "-Xmx" + Prefs.getMaxMemory() + "m",
                    ClientClassPath,
                    joinInfo.address.getHostAddress(),
                    Integer.toString(joinInfo.port),
                    joinInfo.playerName,
                    joinInfo.mppass,
                    SessionManager.getSession().getSkinUrl());
            processBuilder.directory(PathUtil.getClientDir());
            //processBuilder.inheritIO();

            LogUtil.getLogger().log(Level.INFO, concatStringsWSep(processBuilder.command(), " "));
            Process p = processBuilder.start();
            //p.waitFor();
        } catch (Exception ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error launching client", ex);
            LogUtil.die("Error launching client: " + ex);
        }
    }

    private static String concatStringsWSep(List<String> strings, String separator) {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (String s : strings) {
            sb.append(sep).append(s);
            sep = separator;
        }
        return sb.toString();
    }

    private static File getJavaPath() {
        return new File(System.getProperty("java.home"), "bin/java");
    }
}
