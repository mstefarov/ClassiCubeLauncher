package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

final public class ClientLauncher {

    private static final String ClassPath = "client.jar;libs/*",
            ClientClassPath = "com.oyasunadev.mcraft.client.core.ClassiCubeStandalone",
            DefaultJavaArgs = "-Dorg.lwjgl.util.Debug=true "
            + "-Dsun.java2d.noddraw=true "
            + "-Dsun.awt.noerasebackground=true "
            + "-Dsun.java2d.d3d=false "
            + "-Dsun.java2d.opengl=false "
            + "-Dsun.java2d.pmoffscreen=false";

    public static String getDefaultJavaArgs(){
        return DefaultJavaArgs;
    }
    
    public static void launchClient() {
        LogUtil.getLogger().info("launchClient");
        final ServerInfo server = SessionManager.getServerInfo();
        final File java = getJavaPath();

        final String nativePath;
        try {
            nativePath = new File(PathUtil.getClientDir(), "natives").getCanonicalPath();
        } catch (IOException ex) {
            LogUtil.die(ex.toString());
            return;
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(
                java.getAbsolutePath(),
                "-cp",
                ClassPath,
                DefaultJavaArgs,
                "-Djava.library.path=" + nativePath,
                ClientClassPath,
                server.address.getHostAddress(),
                Integer.toString(server.port),
                SessionManager.getSession().getAccount().PlayerName,
                server.pass,
                SessionManager.getSession().getSkinUrl());
        processBuilder.directory(PathUtil.getClientDir());
        processBuilder.inheritIO();

        try {
            LogUtil.getLogger().log(Level.INFO, concatStringsWSep(processBuilder.command(), " "));
            final Process p = processBuilder.start();
            p.waitFor(); // temporary: don't wait for client to exit once we go into production
            System.exit(0);
        } catch (IOException | InterruptedException ex) {
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
