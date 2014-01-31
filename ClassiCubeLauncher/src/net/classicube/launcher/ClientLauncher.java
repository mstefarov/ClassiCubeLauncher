package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import net.classicube.launcher.gui.DebugWindow;
import net.classicube.launcher.gui.ErrorScreen;
import org.apache.commons.lang3.StringUtils;

// Handles launching the client process.
final public class ClientLauncher {

    private static final String ClassPath = "client.jar" + File.pathSeparatorChar + "libs/*",
            ClientClassPath = "com.oyasunadev.mcraft.client.core.ClassiCubeStandalone";

    public static void launchClient(final ServerJoinInfo joinInfo) {
        LogUtil.getLogger().info("launchClient");

        if (joinInfo != null) {
            SessionManager.getSession().storeResumeInfo(joinInfo);
        }// else if joinInfo==null, then we're launching singleplayer

        final File java = PathUtil.getJavaPath();

        final String nativePath;
        try {
            nativePath = new File(PathUtil.getClientDir(), "natives").getCanonicalPath();
        } catch (final Exception ex) {
            ErrorScreen.show("Could not launch the game",
                    "Error finding the LWJGL native library path:<br>" + ex.getMessage(), ex);
            return;
        }

        try {
            final ProcessBuilder processBuilder;
            if (joinInfo != null) {
                processBuilder = new ProcessBuilder(
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
                        (joinInfo.pass == null || joinInfo.pass.length() == 0 ? "none" : joinInfo.pass),
                        SessionManager.getSession().getSkinUrl(),
                        Boolean.toString(Prefs.getFullscreen()));
            } else {
                processBuilder = new ProcessBuilder(
                        java.getAbsolutePath(),
                        "-cp",
                        ClassPath,
                        "-Djava.library.path=" + nativePath,
                        Prefs.getJavaArgs(),
                        "-Xmx" + Prefs.getMaxMemory() + "m",
                        ClientClassPath,
                        "none",
                        "0",
                        "none",
                        "none",
                        SessionManager.getSession().getSkinUrl(),
                        Boolean.toString(Prefs.getFullscreen()));
            }

            processBuilder.directory(PathUtil.getClientDir());

            // log the command used to launch client
            String cmdLineToLog = StringUtils.join(processBuilder.command(), " ");
            if (joinInfo != null && joinInfo.pass != null && joinInfo.pass.length() > 16) {
                // sanitize mppass -- we don't want it logged.
                cmdLineToLog = cmdLineToLog.replace(joinInfo.pass, "########");
            }
            LogUtil.getLogger().log(Level.INFO, cmdLineToLog);

            if (Prefs.getDebugMode()) {
                processBuilder.redirectErrorStream(true);
                try {
                    final Process p = processBuilder.start();
                    DebugWindow.setWindowTitle("Game Running");

                    // capture output from the client, redirect to DebugWindow
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Scanner input = new Scanner(p.getInputStream());
                                while (true) {
                                    DebugWindow.writeLine(input.nextLine());
                                }
                            } catch (NoSuchElementException ex) {
                                DebugWindow.writeLine("(client closed)");
                                DebugWindow.setWindowTitle("Client Closed");
                            }
                        }
                    }.start();

                } catch (IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE, "Error launching client", ex);
                }
            } else {
                processBuilder.start();
                System.exit(0);
            }

        } catch (final Exception ex) {
            ErrorScreen.show("Could not launch the game",
                    "Error launching the client:<br>" + ex.getMessage(), ex);
        }
    }
}
